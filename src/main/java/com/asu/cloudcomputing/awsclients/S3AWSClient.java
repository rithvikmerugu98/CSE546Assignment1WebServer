package com.asu.cloudcomputing.awsclients;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class S3AWSClient {

    private static final String MESSAGES_FILE= "/SqsMessages/messages.json";
    S3Client s3Client;

    S3AWSClient(Region region) {
        s3Client = S3Client.builder().region(region).build();
    }

    public void saveSQSMessagesToS3(String bucketName, List<Message> messages) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(MESSAGES_FILE).build();

    }

    public void getMessagesFromFile(String bucketName) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(MESSAGES_FILE).build();
    }

}
