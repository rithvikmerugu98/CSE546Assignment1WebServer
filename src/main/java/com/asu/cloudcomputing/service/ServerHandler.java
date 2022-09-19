package com.asu.cloudcomputing.service;

import com.asu.cloudcomputing.awsclients.AWSClientProvider;
import com.asu.cloudcomputing.awsclients.Ec2AWSClient;
import com.asu.cloudcomputing.awsclients.S3AWSClient;
import com.asu.cloudcomputing.awsclients.SQSAWSClient;
import com.asu.cloudcomputing.utility.PropertiesReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandler {

    private static PropertiesReader props;
    private static AWSClientProvider awsClientsProvider;


    public ServerHandler() {
        try {
            props = new PropertiesReader("application.properties");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        awsClientsProvider = new AWSClientProvider();
    }

    public String publishImageToSQSQueue(String messageBody) {
        String requestId = String.valueOf(Instant.now().toEpochMilli());
        String requestQueue = props.getProperty("amazon.sqs.request-queue");
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        sqsClient.publishMessages(requestQueue, messageBody, requestId );
        return requestId;
    }

    public String getClassifiedImageResult(String requestId) {
        String bucketName = props.getProperty("amazon.s3.bucket-name");
        S3AWSClient s3Client = awsClientsProvider.getS3Client();
        while(true) {
            Map<String, String> responses = s3Client.getMessagesFromFile(bucketName);
            if(responses.containsKey(requestId)) {
                return responses.get(requestId);
            } else {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    return "Unable to return the response, Check the S3 bucket.";
                }
            }
        }

    }

    public void loadBalancing() {
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        Ec2AWSClient ec2Client = awsClientsProvider.getEc2Client();
        String requestQueue = props.getProperty("amazon.sqs.request-queue");
        String appTierLaunchTemplate = props.getProperty("amazon.ec2.apptier.launch-template");
        int messageCount = sqsClient.getMessagesInQueue(requestQueue);
        List<Instance> instances = ec2Client.getActiveAppInstances();
        int appFleetSize = instances.size();
        if(appFleetSize * 5 < messageCount) {
            int reqNumberOfInstances = (int) Math.ceil((messageCount - (appFleetSize * 5))/5.0);
            int instanceNumber = appFleetSize + 1;
            while(instanceNumber <= 20 && instanceNumber < appFleetSize + reqNumberOfInstances) {
                ec2Client.launchAppTierInstance(appTierLaunchTemplate, instanceNumber);
            }
        } else if(appFleetSize == 0) {
            ec2Client.launchAppTierInstance(appTierLaunchTemplate, 1);
        }
    }

    public void processResponseSQS() {
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        S3AWSClient s3Client = awsClientsProvider.getS3Client();
        String responseQueueURL = props.getProperty("amazon.sqs.response-queue");
        String bucketName = props.getProperty("amazon.s3.bucket-name");
        Map<String, String> newResponses = sqsClient.getMessagesFromResponseQueue(responseQueueURL);
        Map<String, String> savedResponses = s3Client.getMessagesFromFile(bucketName);
        savedResponses.putAll(newResponses);
        s3Client.saveSQSMessagesToS3(bucketName, savedResponses);

    }

    public static void main(String[] args) {
        S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build();

        Map<String, String> mp = new HashMap<>();
        Map<String, String> mp2 = new HashMap<>();

        mp.put("Key1", "Value1");
        mp.put("Key2", "Value2");
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key("SqsMessages/messages.json")
                .bucket("rmerugu-assignment1")
                .build();

        ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(objectRequest);
        System.out.println(object.response().contentLength()) ;
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<>() {
        };
        try {
            mp2 = objectMapper.readValue(object.asByteArray(), typeRef);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket("rmerugu-assignment1")
                .key("SqsMessages/messages.json")
                .build();
        mp2.put("Key4", "Value2");
        try {
            PutObjectResponse response = s3Client
                    .putObject(putOb, RequestBody.fromString(objectMapper.writeValueAsString(mp2)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


    }

}
