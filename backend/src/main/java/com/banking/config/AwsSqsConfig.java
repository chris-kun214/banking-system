package com.banking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsSqsConfig {

    @Bean
    public SqsClient sqsClient(
            @Value("${aws.region:ap-southeast-2}") String region,
            @Value("${aws.sqs.endpoint:}") String endpoint,
            @Value("${aws.sqs.access-key:}") String accessKey,
            @Value("${aws.sqs.secret-key:}") String secretKey) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region));

        // LocalStack typically needs static test credentials + endpoint override.
        if (!endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint));
            String finalAccessKey = accessKey.isBlank() ? "test" : accessKey;
            String finalSecretKey = secretKey.isBlank() ? "test" : secretKey;
            builder = builder.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(finalAccessKey, finalSecretKey))
            );
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
