package com.asu.cloudcomputing.awsclients;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class Ec2AWSClient {
    Ec2Client ec2Client;

    public Ec2AWSClient(Region region) {
        ec2Client = Ec2Client.builder().region(region).build();
    }

    public Ec2Client getEc2Client() {
        if(ec2Client == null) {
            ec2Client = Ec2Client.builder().build();
        }
        return ec2Client;
    }

    public RunInstancesResponse launchAppTierInstance(String launchTemplate, int instanceNumber) {

        RunInstancesRequest ec2RunRequest = RunInstancesRequest.builder()
                
                .launchTemplate(LaunchTemplateSpecification.builder()
                        .launchTemplateId(launchTemplate).build())
                .tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder()
                                .key("Name")
                                .value("app-instance" + instanceNumber).build()).build())
                .build();
        RunInstancesResponse response = ec2Client.runInstances(ec2RunRequest);
        return response;
    }

}
