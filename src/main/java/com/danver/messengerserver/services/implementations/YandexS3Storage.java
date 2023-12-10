package com.danver.messengerserver.services.implementations;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.utils.FileStorageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Primary
@Service
@Qualifier("yandexS3Storage")
public class YandexS3Storage implements StorageService {

    private final AmazonS3 s3;
    private final Environment env;
    private final String bucketName;

    @Autowired
    public YandexS3Storage(AmazonS3 s3, Environment env) {
        this.s3 = s3;
        this.env = env;
        bucketName = this.env.getProperty("yandex.cloud.object-storage.bucket");
    }

    @Override
    public String store(MultipartFile file) throws StorageException {
        return null;
    }

    @Override
    public String store(MultipartFile file, FileStorageOptions options) throws StorageException {
        if (!s3.doesBucketExistV2(this.bucketName)) {
            CreateBucketRequest request = new CreateBucketRequest(this.bucketName);
            s3.createBucket(request);
        }
        try {
            //Path filepath = Paths.get(System.getProperty("java.io.tmpdir" + "/" + file.getOriginalFilename()));
            String key = options.getPath() + "/" + file.getOriginalFilename();

            File tempFile = File.createTempFile("tmp-file-prefix", "tmp-file-postfix");
            tempFile.deleteOnExit();
            file.transferTo(tempFile);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("owner", String.valueOf(options.getOwner()));
            PutObjectRequest request = new PutObjectRequest(this.bucketName, key, tempFile)
                    .withMetadata(metadata);
            PutObjectResult result = s3.putObject(request);
            tempFile.delete();
            String url = s3.getUrl(this.bucketName, key).toString();
            return url;
        } catch (IOException e) {
            throw new StorageException("Couldn't upload file " + file.getOriginalFilename());
        } catch (SecurityException ex) {
            throw new StorageException("Couldn't delete temp file while uploading " + file.getOriginalFilename());
        }
    }

    @Override
    public void load(String filename) {

    }

    @Override
    public String getRootPath() {
        return null;
    }
}
