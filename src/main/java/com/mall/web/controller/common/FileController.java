package com.mall.web.controller.common;

import com.mall.common.result.Result;
import com.mall.common.service.FileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 提供商品图片上传和删除接口，并在存储前校验文件大小、类型及图片签名。 */
@RestController
@ConditionalOnBean(FileService.class)
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
    private static final Set<String> PRODUCT_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final FileService fileService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || !PRODUCT_IMAGE_TYPES.contains(file.getContentType()) || !hasValidImageSignature(file)) {
            throw new com.mall.common.exception.BusinessException("上传文件必须是 5MB 以内的图片文件");
        }
        String path = fileService.upload(file);
        String url = fileService.getPresignedUrl(path);
        Map<String, String> data = new HashMap<>();
        data.put("path", path);
        data.put("url", url);
        return Result.success(data);
    }

    @PostMapping("/product-image")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return upload(file);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> delete(@RequestParam String path) {
        fileService.delete(path);
        return Result.success(null);
    }

    /** 对象存储接收文件前同时检查声明类型和文件头，不能只信任浏览器 MIME 值。 */
    private boolean hasValidImageSignature(MultipartFile file) {
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (file.getContentType()) {
                case "image/jpeg" -> startsWith(header, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
                case "image/png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
                case "image/gif" -> startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                        || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII));
                case "image/webp" -> startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        && header.length >= 12 && Arrays.equals(header, 8, 12, "WEBP".getBytes(StandardCharsets.US_ASCII), 0, 4);
                default -> false;
            };
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && Arrays.equals(value, 0, prefix.length, prefix, 0, prefix.length);
    }
}
