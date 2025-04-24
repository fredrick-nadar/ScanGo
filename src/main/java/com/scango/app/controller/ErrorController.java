package com.scango.app.controller;

import com.scango.app.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ErrorController {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleFileStorageException(FileStorageException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
    
    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleMaxSizeException(Exception ex, Model model) {
        model.addAttribute("errorMessage", "File size exceeds the maximum allowed size of " + maxFileSize + "! Please try a smaller file or contact administrator.");
        return "error";
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model, HttpServletRequest request) {
        String errorMsg = "An unexpected error occurred";
        if (ex.getMessage() != null) {
            errorMsg += ": " + ex.getMessage();
        }
        
        // Add request info for debugging
        model.addAttribute("errorMessage", errorMsg);
        model.addAttribute("path", request.getRequestURI());
        
        return "error";
    }
} 