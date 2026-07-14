package com.example.backend.service.impl;

import com.example.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public String uploadWalkthrough(@org.springframework.lang.NonNull MultipartFile file, @org.springframework.lang.NonNull Long propertyId) {
        String key = "properties/" + propertyId + "/walkthrough-" + System.currentTimeMillis() + "-" + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(Objects.requireNonNullElse(file.getContentType(), "video/mp4"))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload video walkthrough to S3", e);
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    @Override
    public String uploadFile(@org.springframework.lang.NonNull MultipartFile file, @org.springframework.lang.NonNull String keyPrefix) {
        String cleanFileName = Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");
        String key = keyPrefix + "/" + System.currentTimeMillis() + "-" + cleanFileName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(Objects.requireNonNullElse(file.getContentType(), "application/octet-stream"))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}
