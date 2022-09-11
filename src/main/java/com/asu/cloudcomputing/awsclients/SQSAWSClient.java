package com.asu.cloudcomputing.awsclients;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQSAWSClient {
    SqsClient sqsClient;

    public SQSAWSClient(Region region) {
        sqsClient = SqsClient.builder().region(region).build();
    }

    public void publishMessages(String queueURL, String messageBody, String requestId) {
        Map<String, MessageAttributeValue> attr = new HashMap<>();
        attr.put("requestId", MessageAttributeValue.builder().dataType("String").stringValue(requestId).build());
        sqsClient.sendMessage(SendMessageRequest.builder()
                .messageBody(messageBody)
                .messageAttributes(attr)
                .queueUrl(queueURL).build());
    }

    public void deleteMessages(String queueUrl, List<String> messageReceipts){
        for(String receipt : messageReceipts) {
            DeleteMessageRequest request = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receipt)
                    .build();
            sqsClient.deleteMessage(request);
        }
    }

    public String getMessageWithRequestId(String queueUrl, String requestID) {
        int count =  0;
        while(count++ < 10) {
            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl).messageAttributeNames("*").maxNumberOfMessages(10).build());

            System.out.println(response);
            if (response.hasMessages()) {
                List<Message> messages = response.messages();
                for (Message message : messages) {
                    String messageRequestID = message.messageAttributes().get("requestId").stringValue();
                    if(requestID.equals(messageRequestID)) {
                        return message.body();
                    }
                }
            }
            try{
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                return "Was not able to process the request.";
            }
        }
        return "";
    }

}
