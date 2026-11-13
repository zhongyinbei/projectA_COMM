package com.lab.controller;

import com.lab.common.Result;
import com.lab.interceptor.RequireRole;
import com.lab.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @RequireRole({"ADMIN", "REPAIRER", "REPORTER"})
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(Map.of("url", fileService.upload(file)));
    }
}
