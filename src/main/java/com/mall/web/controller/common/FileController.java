package com.mall.web.controller.common;

import com.mall.common.result.Result;
import com.mall.common.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
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

    @DeleteMapping
    public Result<Void> delete(@RequestParam String path) {
        fileService.delete(path);
        return Result.success(null);
    }
}
