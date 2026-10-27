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

/**
 * 文件存储控制器。
 *
 * <p>提供图片上传和删除入口。上传时在交由文件服务保存之前检查文件大小、声明类型及文件头签名；
 * 仅当应用上下文中存在 {@link FileService} Bean 时才注册本控制器。</p>
 */
@RestController
@ConditionalOnBean(FileService.class)
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
    /** 上传接口接受的图片 MIME 类型白名单。 */
    private static final Set<String> PRODUCT_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    /** 执行对象存储上传、预签名地址生成及文件删除。 */
    private final FileService fileService;

    /**
     * 上传图片并返回存储路径及访问 URL。
     *
     * <p>文件必须非空、大小不超过 5 MiB，且声明 MIME 类型与支持的图片文件头相符；
     * 校验通过后由文件服务上传并生成预签名访问地址。</p>
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || !PRODUCT_IMAGE_TYPES.contains(file.getContentType()) || !hasValidImageSignature(file)) {
            throw new com.mall.common.exception.BusinessException("上传文件必须是 5MB 以内的图片文件");
        }
        String path = fileService.upload(file);
        String url = fileService.getPresignedUrl(path);
        // 同时返回对象存储路径和可访问 URL，供前端分别保存及展示使用。
        Map<String, String> data = new HashMap<>();
        data.put("path", path);
        data.put("url", url);
        return Result.success(data);
    }

    /** 商品 SPU 图片上传入口，额外要求具备商品编辑权限。 */
    @PostMapping("/product-image")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return upload(file);
    }

    /** 删除指定对象存储路径对应的文件，需要商品编辑权限。 */
    @DeleteMapping
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result<Void> delete(@RequestParam String path) {
        fileService.delete(path);
        return Result.success(null);
    }

    /**
     * 对照声明 MIME 类型校验图片文件签名。
     *
     * <p>文件头检查可降低仅伪造浏览器 MIME 值上传非图片内容的风险；
     * 输入流读取失败时按签名无效处理。</p>
     */
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

    /** 判断字节数组是否以指定签名字节序列开头。 */
    private boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && Arrays.equals(value, 0, prefix.length, prefix, 0, prefix.length);
    }
}
