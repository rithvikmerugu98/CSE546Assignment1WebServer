package com.asu.cloudcomputing.model;

import software.amazon.awssdk.services.ec2.model.Instance;

public class Ec2Instance {


    Instance instance;
    volatile int count;

    public Ec2Instance(Instance instance, int count) {
        this.instance = instance;
        this.count = count;
    }


    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
