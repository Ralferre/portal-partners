package com.example.portalpartners.controller;

import com.example.portalpartners.service.MinioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FileController {

    private final MinioService minioService;

    public FileController(MinioService service) {
        this.minioService = service;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        return minioService.uploadFile(file);
    }
}
