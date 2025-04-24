package com.scango.app.service;

import com.scango.app.config.FileStorageProperties;
import com.scango.app.exception.FileStorageException;
import com.scango.app.model.FileInfo;
import com.scango.app.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final FileRepository fileRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties, FileRepository fileRepository) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        this.fileRepository = fileRepository;
    }

    public void init() {
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                    e);
        }
    }

    public FileInfo storeFile(MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence " + fileName);
            }

            // Create the file info entity
            FileInfo fileInfo = new FileInfo(fileName, file.getContentType(), file.getSize());

            // Create the file on the server
            Path targetLocation = this.fileStorageLocation.resolve(fileInfo.getFileId() + "_" + fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create the download URL with configurable base URL
            String fileDownloadUri = baseUrl + "/api/files/download/" + fileInfo.getFileId();
            fileInfo.setDownloadUrl(fileDownloadUri);

            // Save the file info
            return fileRepository.save(fileInfo);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileId) {
        try {
            Optional<FileInfo> fileInfoOpt = fileRepository.findByFileId(fileId);
            if (!fileInfoOpt.isPresent()) {
                throw new FileStorageException("File not found with id " + fileId);
            }

            FileInfo fileInfo = fileInfoOpt.get();
            Path filePath = this.fileStorageLocation.resolve(fileInfo.getFileId() + "_" + fileInfo.getFileName())
                    .normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("File not found " + fileInfo.getFileName());
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found", ex);
        }
    }
}