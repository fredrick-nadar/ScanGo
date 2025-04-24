package com.scango.app.service;

import com.ngrok.api.Tunnels;
import com.ngrok.services.TunnelsClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class NgrokService {
    private static final Logger logger = LoggerFactory.getLogger(NgrokService.class);
    private Process ngrokProcess;
    private String publicUrl;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${ngrok.auth-token}")
    private String authToken;

    @PostConstruct
    public void startNgrok() {
        try {
            logger.info("Starting ngrok tunnel for port {}", serverPort);

            // Start ngrok process
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ngrok", "http", String.valueOf(serverPort),
                    "--authtoken", authToken,
                    "--log", "stdout");

            ngrokProcess = processBuilder.start();

            // Wait for ngrok to start and get the URL
            waitForNgrokStart();

            logger.info("Ngrok tunnel established at: {}", publicUrl);

            // Update the application's base URL
            System.setProperty("app.base-url", publicUrl);

        } catch (Exception e) {
            logger.error("Failed to start ngrok tunnel", e);
            throw new RuntimeException("Failed to start ngrok tunnel", e);
        }
    }

    private void waitForNgrokStart() throws InterruptedException {
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                // Create a tunnels client to get the public URL
                TunnelsClient tunnelsClient = new TunnelsClient();
                Tunnels tunnels = tunnelsClient.list().get(0);
                publicUrl = tunnels.getPublicUrl();

                if (publicUrl != null && !publicUrl.isEmpty()) {
                    return;
                }
            } catch (Exception e) {
                logger.debug("Waiting for ngrok to start (attempt {}/{})", attempt + 1, maxAttempts);
            }

            TimeUnit.SECONDS.sleep(2);
            attempt++;
        }

        throw new RuntimeException("Failed to get ngrok public URL after " + maxAttempts + " attempts");
    }

    @PreDestroy
    public void stopNgrok() {
        if (ngrokProcess != null) {
            try {
                logger.info("Stopping ngrok tunnel");
                ngrokProcess.destroy();

                // Wait for the process to terminate
                if (!ngrokProcess.waitFor(10, TimeUnit.SECONDS)) {
                    ngrokProcess.destroyForcibly();
                }
            } catch (Exception e) {
                logger.error("Error stopping ngrok tunnel", e);
            }
        }
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}