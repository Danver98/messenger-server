package com.danver.messengerserver.configs;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class StorageConfig {

    private Environment env;

    public StorageConfig(Environment env) {
        this.env = env;
    }

    @Bean
    AmazonS3 getAmazonS3Client() {
        // Alternative way of getting credentials
        /*AWSCredentials credentials = new BasicAWSCredentials(
                this.env.getProperty("yandex.cloud.object-storage.account.access.key.id"),
                this.env.getProperty("yandex.cloud.object-storage.account.secret.key")
        );*/
        AWSCredentials credentials;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AmazonS3ClientBuilder.EndpointConfiguration(
                                this.env.getProperty("s3.storage.endpoint"),
                                this.env.getProperty("s3.storage.signingRegion")
                        )
                )
                .withPathStyleAccessEnabled(true)
                .build();
/*        AmazonS3ClientBuilder.standard();
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new ProfileCredentialsProvider())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                this.env.getProperty("s3.storage.endpoint"),
                                this.env.getProperty("s3.storage.signingRegion")
                        )
                )
                .build();*/
    }
}
