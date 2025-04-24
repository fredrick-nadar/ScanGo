package com.scango.app.controller;

import com.scango.app.model.FileInfo;
import com.scango.app.service.FileStorageService;
import com.scango.app.service.QRCodeService;
import com.scango.app.service.NgrokService;
import com.scango.app.exception.FileStorageException;
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

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import java.time.format.DateTimeFormatter;

@Controller
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private NgrokService ngrokService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        FileInfo fileInfo = fileStorageService.storeFile(file);

        // Get the ngrok public URL
        String baseUrl = ngrokService.getPublicUrl();
        if (baseUrl == null) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        // Generate the download URL using the ngrok public URL
        String downloadUrl = baseUrl + "/api/files/download/" + fileInfo.getFileId();
        fileInfo.setDownloadUrl(downloadUrl);

        // Generate QR code for the download URL
        String qrCodeImage = qrCodeService.generateQRCodeImage(downloadUrl, 250, 250);

        model.addAttribute("fileName", fileInfo.getFileName());
        model.addAttribute("fileId", fileInfo.getFileId());
        model.addAttribute("fileSize", fileInfo.getSize());
        model.addAttribute("downloadUrl", downloadUrl);
        model.addAttribute("qrCode", qrCodeImage);

        return "result";
    }

    @GetMapping("/api/files/download/{fileId}")
    public String showDownloadPage(@PathVariable String fileId, Model model) {
        try {
            // Get file info from repository
            Optional<FileInfo> fileInfoOpt = fileStorageService.getFileInfo(fileId);
            if (!fileInfoOpt.isPresent()) {
                throw new FileStorageException("File not found with id " + fileId);
            }

            FileInfo fileInfo = fileInfoOpt.get();

            // Get the ngrok public URL
            String baseUrl = ngrokService.getPublicUrl();
            if (baseUrl == null) {
                baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            }

            // Add file information to the model
            model.addAttribute("fileName", fileInfo.getFileName());
            model.addAttribute("fileSize", formatFileSize(fileInfo.getSize()));
            model.addAttribute("uploadDate", fileInfo.getUploadDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            model.addAttribute("downloadUrl", baseUrl + "/api/files/download/start/" + fileId);

            return "download";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error retrieving file: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/api/files/download/start/{fileId}")
    public ResponseEntity<Resource> startDownload(@PathVariable String fileId, HttpServletRequest request) {
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

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}