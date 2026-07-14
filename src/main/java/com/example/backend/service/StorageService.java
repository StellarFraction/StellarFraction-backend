package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadWalkthrough(@org.springframework.lang.NonNull MultipartFile file, @org.springframework.lang.NonNull Long propertyId);
    
    /**
     * Uploads a generic file to the storage provider (e.g. S3).
     */
    String uploadFile(@org.springframework.lang.NonNull MultipartFile file, @org.springframework.lang.NonNull String keyPrefix);
}
