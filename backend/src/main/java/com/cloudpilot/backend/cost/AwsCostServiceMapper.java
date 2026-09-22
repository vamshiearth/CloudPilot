package com.cloudpilot.backend.cost;

import org.springframework.stereotype.Component;

@Component
public class AwsCostServiceMapper {

    public CloudServiceCategory map(String awsServiceName) {
        if (awsServiceName == null) {
            return CloudServiceCategory.OTHER;
        }

        String value = awsServiceName.toLowerCase();

        if (value.contains("elastic compute cloud") || value.contains("ec2")) {
            return CloudServiceCategory.COMPUTE;
        }
        if (value.contains("elastic kubernetes") || value.contains("eks")) {
            return CloudServiceCategory.KUBERNETES;
        }
        if (value.contains("elastic block store")
            || value.contains("ebs")
            || value.contains("simple storage")
            || value.contains("s3")) {
            return CloudServiceCategory.STORAGE;
        }
        if (value.contains("relational database") || value.contains("rds")) {
            return CloudServiceCategory.DATABASE;
        }
        if (value.contains("elasticache")) {
            return CloudServiceCategory.CACHE;
        }
        if (value.contains("kafka") || value.contains("msk")) {
            return CloudServiceCategory.MESSAGING;
        }
        if (value.contains("cloudwatch")) {
            return CloudServiceCategory.OBSERVABILITY;
        }
        if (value.contains("container registry") || value.contains("ecr")) {
            return CloudServiceCategory.CONTAINER_REGISTRY;
        }
        if (value.contains("data transfer")
            || value.contains("network")
            || value.contains("nat gateway")) {
            return CloudServiceCategory.NETWORK;
        }

        return CloudServiceCategory.OTHER;
    }
}