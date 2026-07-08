package com.mall.common.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final MinioClient minioClient;
    private final com.mall.common.config.MinioConfig minioConfig;

    /**
     * 上传文件到 MinIO，返回访问路径
     */
    public String upload(MultipartFile file) {
        ensureBucket();
        String objectName = UUID.randomUUID().toString().substring(0, 8)
                + "-" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("File uploaded: {}", objectName);
            return minioConfig.getBucket() + "/" + objectName;
        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new com.mall.common.exception.BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名 URL（有效期 7 天）
     */
    public String getPresignedUrl(String objectPath) {
        String[] parts = objectPath.split("/", 2);
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(parts[0])
                    .object(parts[1])
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Presigned URL failed", e);
            return objectPath;
        }
    }

    public void delete(String objectPath) {
        String[] parts = objectPath.split("/", 2);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(parts[0])
                    .object(parts[1])
                    .build());
        } catch (Exception e) {
            log.error("Delete failed", e);
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
            }
        } catch (Exception e) {
            log.error("Ensure bucket failed", e);
        }
    }
}
