package com.scango.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.scango.app.config.FileStorageProperties;
import org.springframework.boot.CommandLineRunner;

import jakarta.annotation.Resource;

@SpringBootApplication
@EnableConfigurationProperties({
        FileStorageProperties.class
})
@EnableScheduling
public class ScanGoApplication implements CommandLineRunner {

    @Resource
    private com.scango.app.service.FileStorageService fileStorageService;

    public static void main(String[] args) {
        SpringApplication.run(ScanGoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        fileStorageService.init();
    }
}