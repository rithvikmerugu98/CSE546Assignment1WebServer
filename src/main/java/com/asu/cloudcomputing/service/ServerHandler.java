package com.asu.cloudcomputing.service;

import com.asu.cloudcomputing.awsclients.AWSClientProvider;
import com.asu.cloudcomputing.awsclients.SQSAWSClient;
import com.asu.cloudcomputing.utility.PropertiesReader;

import java.io.IOException;
import java.time.Instant;

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

    public String publishImageToSQSQueue(String messageBody, String fileName) {
        String requestId = String.valueOf(Instant.now().toEpochMilli());
        String requestQueue = props.getProperty("amazon.sqs.request-queue");
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();
        sqsClient.publishMessages(requestQueue, messageBody, requestId,fileName );
        return requestId;
    }

    public String getClassifiedImageResult(String requestId) {
        String responseQueueURL = props.getProperty("amazon.sqs.response-queue");
        SQSAWSClient sqsClient = awsClientsProvider.getSQSClient();

        return sqsClient.getMessageWithRequestId(responseQueueURL, requestId);

    }

}
