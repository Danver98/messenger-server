package com.danver.messengerserver.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfig {

    private final Environment env;

    public StorageConfig(Environment env) {
        this.env = env;
    }

    @Bean
    S3Client getAmazonS3Client() {
        // Alternative way of getting credentials (commented - using BasicAwsCredentials)
        /*
        AwsCredentials credentials = AwsBasicCredentials.create(
            this.env.getProperty("yandex.cloud.object-storage.account.access.key.id"),
            this.env.getProperty("yandex.cloud.object-storage.account.secret.key")
        );
        */

        // Get credentials from profile (~/.aws/credentials)
        AwsCredentialsProvider credentialsProvider;
        try {
            credentialsProvider = ProfileCredentialsProvider.create();
            // Test the credentials provider by calling resolveCredentials()
            credentialsProvider.resolveCredentials();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        // Build S3 client with path-style access for Yandex Cloud / S3-compatible storage
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(this.env.getProperty("s3.storage.signingRegion")))
                .endpointOverride(java.net.URI.create(this.env.getProperty("s3.storage.endpoint")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
/*        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(this.env.getProperty("s3.storage.signingRegion")))
                .endpointOverride(URI.create(this.env.getProperty("s3.storage.endpoint")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();*/
    }
}