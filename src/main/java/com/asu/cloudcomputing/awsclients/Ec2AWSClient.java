package com.asu.cloudcomputing.awsclients;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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

    public Predicate<Instance> notFirstAppInstance = instance -> {
        if(instance.hasTags()) {
            for(Tag tag : instance.tags()) {
                if(tag.key().equals("Name") && tag.value().equals("app-instance1")) {
                    return false;
                }
            }
        }
        return true;
    };

    public RunInstancesResponse launchAppTierInstance(String launchTemplate, int instanceNumber) {

        RunInstancesRequest ec2RunRequest = RunInstancesRequest.builder()
                .launchTemplate(LaunchTemplateSpecification.builder()
                        .launchTemplateId(launchTemplate).build())
                .tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder().key("Name")
                                .value("app-instance" + instanceNumber).build(),
                            Tag.builder().key("ServerType")
                                .value("AppTier").build()).build()
                        )
                .minCount(1)
                .maxCount(1)
                .build();
        RunInstancesResponse response = ec2Client.runInstances(ec2RunRequest);
        return response;
    }

    public List<Instance> getActiveAppInstances() {
        List<Instance> instances = new ArrayList<>();
        DescribeInstancesRequest req = DescribeInstancesRequest.builder()
                .filters(Filter.builder().name("instance-state-name")
                            .values("pending", "running").build(),
                        Filter.builder().name("tag:ServerType")
                            .values("AppTier").build()
                ).build();
        DescribeInstancesResponse res = ec2Client.describeInstances(req);
        if(res.hasReservations()) {
            for(Reservation reservation : res.reservations()) {
                if (reservation.hasInstances()) {
                    instances.addAll(reservation.instances());
                }
            }
        }
        return instances;
    }

    public void terminateInstance(String instanceID){
        ec2Client.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instanceID).build());
    }



}
