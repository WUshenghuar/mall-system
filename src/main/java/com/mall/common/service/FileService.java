package com.mall.common.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 管理 MinIO 对象路径、上传和临时访问地址，业务数据保存路径而非预签名 URL。 */
@Slf4j // 使用@Slf4j注解，用于日志记录
@Service // 标记此类为Spring服务组件
@ConditionalOnBean(MinioClient.class) // 当容器中存在MinioClient Bean时才创建此Bean
@RequiredArgsConstructor // 使用lombok的@RequiredArgsConstructor注解，生成包含final字段的构造函数
public class FileService {
    // 注入MinioClient客户端
    private final MinioClient minioClient;
    // 注入Minio配置类
    private final com.mall.common.config.MinioConfig minioConfig;

    /**
     * 上传文件到 MinIO，返回访问路径
     * @param file 要上传的文件
     * @return 文件的访问路径
     */
    public String upload(MultipartFile file) {
        // 确保存储桶存在
        ensureBucket();
        // 清理文件名中的路径字符
        String originalName = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
        // 获取文件名（去除路径）
        String fileName = originalName.substring(originalName.lastIndexOf('/') + 1);
        // 如果文件名为空或不合法，使用默认名"upload"
        if (!StringUtils.hasText(fileName) || "null".equals(fileName) || fileName.contains("..")) fileName = "upload";
        // 生成唯一文件名：UUID前缀 + 原始文件名
        String objectName = UUID.randomUUID().toString().substring(0, 8)
                + "-" + fileName;
        try (InputStream is = file.getInputStream()) {
            // 使用MinIO客户端上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket()) // 指定存储桶
                    .object(objectName) // 指定对象名
                    .stream(is, file.getSize(), -1) // 设置输入流和文件大小
                    .contentType(file.getContentType()) // 设置文件类型
                    .build());
            // 记录上传成功的日志
            log.info("File uploaded: {}", objectName);
            // 返回文件的访问路径
            return minioConfig.getBucket() + "/" + objectName;
        } catch (Exception e) {
            // 记录上传失败的日志
            log.error("Upload failed", e);
            // 抛出业务异常
            throw new com.mall.common.exception.BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名 URL（有效期 7 天）
     * @param objectPath 文件对象路径
     * @return 预签名URL
     */
    public String getPresignedUrl(String objectPath) {
        // 获取对象名称
        String objectName = objectName(objectPath);
        try {
            // 生成预签名URL
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioConfig.getBucket()) // 指定存储桶
                    .object(objectName) // 指定对象名
                    .method(Method.GET) // 指定HTTP方法
                    .expiry(7, TimeUnit.DAYS) // 设置有效期7天
                    .build());
        } catch (Exception e) {
            // 记录生成URL失败的日志
            log.error("Presigned URL failed", e);
            // 返回原始路径
            return objectPath;
        }
    }

    /**
     * 删除文件
     * @param objectPath 文件对象路径
     */
    public void delete(String objectPath) {
        // 获取对象名称
        String objectName = objectName(objectPath);
        try {
            // 使用MinIO客户端删除文件
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket()) // 指定存储桶
                    .object(objectName) // 指定对象名
                    .build());
        } catch (Exception e) {
            // 记录删除失败的日志
            log.error("Delete failed", e);
            // 抛出业务异常
            throw new com.mall.common.exception.BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 从对象路径中提取对象名称
     * @param objectPath 文件对象路径
     * @return 对象名称
     */
    private String objectName(String objectPath) {
        // 存储桶路径前缀
        String prefix = minioConfig.getBucket() + "/";
        // 检查路径合法性
        if (!StringUtils.hasText(objectPath) || !objectPath.startsWith(prefix)) {
            throw new com.mall.common.exception.BusinessException("文件路径不合法");
        }
        // 提取对象名称
        String objectName = objectPath.substring(prefix.length());
        // 检查对象名称合法性
        if (!StringUtils.hasText(objectName) || objectName.contains("..")) {
            throw new com.mall.common.exception.BusinessException("文件路径不合法");
        }
        return objectName;
    }

    /**
     * 确保存储桶存在，如果不存在则创建
     */
    private void ensureBucket() {
        try {
            // 检查存储桶是否存在
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            // 如果不存在则创建
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
            }
        } catch (Exception e) {
            // 记录存储桶操作失败的日志
            log.error("Ensure bucket failed", e);
        }
    }
}
