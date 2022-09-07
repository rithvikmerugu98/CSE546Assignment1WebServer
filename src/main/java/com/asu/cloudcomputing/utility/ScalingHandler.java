package com.asu.cloudcomputing.utility;

import com.asu.cloudcomputing.model.Ec2Instance;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ScalingHandler {

    PriorityQueue<Ec2Instance> queue = new PriorityQueue<>(Comparator.comparing(Ec2Instance::getCount));

    public void addInstance(Instance instance) {
        queue.add(new Ec2Instance(instance, 0));
    }

    public String getNextInvokeURL() {
        Ec2Instance ec2Instance = queue.poll();
        String publicIP = ec2Instance.getInstance().publicIpAddress();

        return publicIP + ":8080/classifyImage";
    }


}
