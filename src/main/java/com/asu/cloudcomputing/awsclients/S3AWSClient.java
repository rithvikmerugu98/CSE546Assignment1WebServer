package com.asu.cloudcomputing.awsclients;

import com.amazonaws.services.s3.AmazonS3URI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3AWSClient {

    private static final String MESSAGES_FILE= "SqsMessages/messages.json";
    S3Client s3Client;

    S3AWSClient(Region region) {
        s3Client = S3Client.builder().region(region).build();
    }

    public void saveSQSMessagesToS3(String bucketName, Map<String, String> messages) {
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(MESSAGES_FILE)
                .build();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            s3Client.putObject(putOb, RequestBody.fromString(objectMapper.writeValueAsString(messages)));
        } catch (JsonProcessingException e) {
            System.out.println("Unable to save the messages to s3.");
        }
    }

    public void saveImageToS3(String bucketName, String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName).key(fileName).build();
        s3Client.putObject(request, RequestBody.fromFile(new File(filePath)));
    }



    public Map<String, String> getMessagesFromFile(String bucketName) {
        Map<String, String> messages = new HashMap<>();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(MESSAGES_FILE)
                .build();

        ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(request);
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<>() {
        };
        try {
            messages = objectMapper.readValue(object.asByteArray(), typeRef);
        } catch (IOException e) {
            System.out.println("Unable to fetch the data from queue.");
        }
        return messages;
    }

    public void uploadImageToS3(String bucketName, String fileName, byte[] byteData) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(byteData));
    }


}
