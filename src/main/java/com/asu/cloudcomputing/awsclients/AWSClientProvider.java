package com.asu.cloudcomputing.awsclients;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

public class AWSClientProvider {

    private SQSAWSClient sqsClient;
    private Ec2AWSClient ec2Client;

    private Region region;

    public AWSClientProvider() {
        region = Region.US_EAST_1;
    }


    public Ec2AWSClient getEc2Client() {
        if(ec2Client == null) {
            ec2Client = new Ec2AWSClient(region);
        }
        return ec2Client;
    }

    public SQSAWSClient getSQSClient() {
        if(sqsClient == null) {
            sqsClient = new SQSAWSClient(region);
        }
        return sqsClient;
    }


}
