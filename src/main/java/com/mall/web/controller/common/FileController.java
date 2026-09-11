package com.mall.web.controller.common;

import com.mall.common.result.Result;
import com.mall.common.service.FileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@ConditionalOnBean(FileService.class)
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
    private static final Set<String> PRODUCT_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final FileService fileService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
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
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || !PRODUCT_IMAGE_TYPES.contains(file.getContentType())) {
            throw new com.mall.common.exception.BusinessException("商品图片必须是 5MB 以内的图片文件");
        }
        return upload(file);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> delete(@RequestParam String path) {
        fileService.delete(path);
        return Result.success(null);
    }
}
