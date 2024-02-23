package com.danver.messengerserver.scripts;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ScriptRunner {
    @Autowired
    AmazonS3 s3;
    @Autowired
    Environment env;

    @PostConstruct
    public void createBucket() {
        String bucketName = "filebase-bucket-name-2";
        try {
            AmazonS3ClientBuilder.standard();
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("s3.filebase.com", "us-east-1"))
                    .build();
            if (!s3Client.doesBucketExistV2(bucketName)) {
                s3Client.createBucket(new CreateBucketRequest(bucketName));
                String bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
                System.out.println("Bucket location: " + bucketLocation);
            }
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        } catch (SdkClientException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void enableVersioning() {
        try {
            String bucketName = env.getProperty("s3.storage.bucket");
            // 1. Enable versioning on the bucket.
            BucketVersioningConfiguration configuration =
                    new BucketVersioningConfiguration().withStatus("Enabled");

            SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest =
                    new SetBucketVersioningConfigurationRequest(bucketName,configuration);

            s3.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest);

            // 2. Get bucket versioning configuration information.
            BucketVersioningConfiguration conf = s3.getBucketVersioningConfiguration(bucketName);
            System.out.println("bucket versioning configuration status:    " + conf.getStatus());

        } catch (AmazonS3Exception amazonS3Exception) {
            System.out.format("An Amazon S3 error occurred. Exception: %s", amazonS3Exception);
        } catch (Exception ex) {
            System.out.format("Exception: %s", ex);
        }
    }
}
