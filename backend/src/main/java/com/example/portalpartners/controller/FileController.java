package com.example.portalpartners.controller;

import com.example.portalpartners.service.MinioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files")
public class FileController {

    private final MinioService minioService;

    public FileController(MinioService service) {
        this.minioService = service;
    }
}
