package com.asu.cloudcomputing.service;

import com.asu.cloudcomputing.awsclients.AWSClientProvider;
import com.asu.cloudcomputing.awsclients.Ec2AWSClient;
import com.asu.cloudcomputing.awsclients.S3AWSClient;
import com.asu.cloudcomputing.awsclients.SQSAWSClient;
import com.asu.cloudcomputing.utility.PropertiesReader;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerHandler {

    private static PropertiesReader props;
    private static AWSClientProvider awsClientsProvider;

    private static volatile LocalDateTime lastResponseTime;
    private static volatile LocalDateTime lastRequestTime;


    public ServerHandler() {
        try {
            props = new PropertiesReader("application.properties");
            lastResponseTime = LocalDateTime.now();
            lastRequestTime = LocalDateTime.now();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        awsClientsProvider = new AWSClientProvider();
    }

    public String publishImageToSQSQueue(String messageBody) {
        String requestId = UUID.randomUUID().toString();
        String requestQueue = props.getProperty("amazon.sqs.request-queue");
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        sqsClient.publishMessages(requestQueue, messageBody, requestId );
        System.out.println("Successfully uploaded the image to queue with requestID - " + requestId);
        lastRequestTime = LocalDateTime.now();
        return requestId;
    }

    public String getClassifiedImageResult(String requestId) {
        String bucketName = props.getProperty("amazon.s3.bucket-name");
        S3AWSClient s3Client = awsClientsProvider.getS3Client();
        System.out.println("Starting to poll for the response from S3.");
        while(true) {
            System.out.println("Polling messages for request - " + requestId);
            Map<String, String> responses = s3Client.getMessagesFromFile(bucketName);
            if(responses.containsKey(requestId)) {
                System.out.println("Returning the following response from the bucket - " + responses.get(requestId) + " for request - " + requestId);
                return responses.get(requestId);
            } else {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    return "Unable to return the response, Check the S3 bucket. Issue occurred when polling for response.";
                }
            }
        }

    }

    public void loadBalancing() {
        System.out.println("Invoking LoadBalancer at " + LocalTime.now());
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        Ec2AWSClient ec2Client = awsClientsProvider.getEc2Client();
        String requestQueue = props.getProperty("amazon.sqs.request-queue");
        String appTierLaunchTemplate = props.getProperty("amazon.ec2.apptier.launch-template");
        int messageCount = sqsClient.getMessagesInQueue(requestQueue);

        List<Instance> instances = ec2Client.getActiveAppInstances();
        int appFleetSize = instances.size();
        System.out.println("App Server fleet size - " + appFleetSize);
        System.out.println("Approx number of pending requests - " + messageCount);
        if(appFleetSize * 5 < messageCount) {
            int reqNumberOfInstances = (int) Math.ceil((messageCount - (appFleetSize * 5))/5.0);
            reqNumberOfInstances = Math.min(reqNumberOfInstances, 3);
            System.out.println("Creating " + reqNumberOfInstances + " to balance the load." );
            int instanceNumber = appFleetSize + 1;
            while(instanceNumber <= 20 && reqNumberOfInstances-->0) {
                ec2Client.launchAppTierInstance(appTierLaunchTemplate, instanceNumber++);
            }
        } else if(messageCount == 0) {
            LocalDateTime now = LocalDateTime.now();
            System.out.println("Last Request received at - " + lastRequestTime);
            System.out.println("Last Response received at - " + lastResponseTime);
            if(lastRequestTime.plusMinutes(1).compareTo(now) < 0 &&
                    lastResponseTime.plusSeconds(35).compareTo(now) < 0 && appFleetSize > 1) {
                System.out.println("Downscaling the instances.");
                instances.stream().filter(ec2Client.notFirstAppInstance)
                        .map(Instance::instanceId)
                        .forEach(ec2Client::terminateInstance);
            }
        }
        if(appFleetSize == 0) {
            System.out.println("No app instances found so creating an instance.");
            ec2Client.launchAppTierInstance(appTierLaunchTemplate, 1);
        }
    }

    public void processResponseSQS() {
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        S3AWSClient s3Client = awsClientsProvider.getS3Client();
        String responseQueueURL = props.getProperty("amazon.sqs.response-queue");
        String bucketName = props.getProperty("amazon.s3.bucket-name");
        Map<String, String> newResponses = sqsClient.getMessagesFromResponseQueue(responseQueueURL);
        if(newResponses.size() > 0) {
            System.out.println("Received the following responses from response queue - " + newResponses);
            Map<String, String> savedResponses = s3Client.getMessagesFromFile(bucketName);
            savedResponses.putAll(newResponses);
            s3Client.saveSQSMessagesToS3(bucketName, savedResponses);
            System.out.println("Saved the message responses to S3.");
            lastResponseTime = LocalDateTime.now();
        }
    }


}
