package com.scango.app.controller;

import com.scango.app.model.FileInfo;
import com.scango.app.service.FileStorageService;
import com.scango.app.service.QRCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private QRCodeService qrCodeService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        FileInfo fileInfo = fileStorageService.storeFile(file);
        
        // Generate QR code for the download URL
        String qrCodeImage = qrCodeService.generateQRCodeImage(fileInfo.getDownloadUrl(), 250, 250);
        
        model.addAttribute("fileName", fileInfo.getFileName());
        model.addAttribute("fileId", fileInfo.getFileId());
        model.addAttribute("fileSize", fileInfo.getSize());
        model.addAttribute("downloadUrl", fileInfo.getDownloadUrl());
        model.addAttribute("qrCode", qrCodeImage);
        
        return "result";
    }

    @GetMapping("/api/files/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileId);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Fallback to the default content type
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
} 