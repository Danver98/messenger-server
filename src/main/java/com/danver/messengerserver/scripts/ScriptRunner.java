package com.danver.messengerserver.scripts;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.danver.messengerserver.services.permission.PermissionType;
import com.danver.messengerserver.services.permission.ResourceType;
import com.danver.messengerserver.utils.Constants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScriptRunner {
    @Autowired
    AmazonS3 s3;
    @Autowired
    Environment env;
    @Autowired
    RedisTemplate<String, ?> redis;
    @Autowired
    JdbcTemplate jdbcTemplate;

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

    @PostConstruct
    public void fillRedisUserPermissions() {
/*        String PERMISSION_KEY = Constants.REDIS_USERS_PERMISSIONS;
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        int resourceType = ResourceType.CHAT.getValue();
        int [] userList = new int[] {
                16,
                17,
                18,
                20,
                21,
                22,
                21,
                20,
                24,
                25,
                26,
                27,
                21,
                26,
                27,
                26,
                27,
                25,
                21,
                24,
                26,
                24,
                21,
                21,
                27,
                21,
                24,
                25
        };

        int [] resourceList = new int[] {
                6,
                6,
                6,
                6,
                6,
                6,
                19,
                19,
                6,
                6,
                6,
                6,
                65,
                65,
                65,
                66,
                66,
                66,
                66,
                67,
                67,
                68,
                68,
                69,
                69,
                70,
                70,
                70
        };
        for (int i=0; i < userList.length; i++) {
            String key = userList[i] + ":" + resourceList[i] + ":" + resourceType;
            try {
                String permissionsStr = "[ " + PermissionType.Chat.DEFAULT.getValue() + "]";
                hashOps.put(PERMISSION_KEY, key, permissionsStr);
            } catch (RedisConnectionFailureException ignored) {
                int a = 1;
            }
        }
        int a = 1;*/
    }
}
