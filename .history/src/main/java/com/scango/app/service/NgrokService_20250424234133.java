package com.scango.app.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        int maxAttempts = 30;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                // Try to get the ngrok tunnel URL from its API
                URL url = new URL("http://localhost:4040/api/tunnels");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    // Extract the public URL using regex
                    Pattern pattern = Pattern.compile("\"public_url\":\"(https://[^\"]+)\"");
                    Matcher matcher = pattern.matcher(content.toString());

                    if (matcher.find()) {
                        publicUrl = matcher.group(1);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.debug("Waiting for ngrok to start (attempt {}/{})", attempt + 1, maxAttempts);
            }

            TimeUnit.SECONDS.sleep(1);
            attempt++;
        }

        throw new RuntimeException("Failed to get ngrok public URL after " + maxAttempts + " attempts");
    }

    @PreDestroy
    public void stopNgrok() {
        if (ngrokProcess != null) {
            try {
                logger.info("Stopping ngrok tunnel");

                // First try to kill any existing ngrok processes
                try {
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        Runtime.getRuntime().exec("taskkill /F /IM ngrok.exe");
                    } else {
                        Runtime.getRuntime().exec("pkill -f ngrok");
                    }
                } catch (Exception e) {
                    logger.warn("Failed to kill existing ngrok processes", e);
                }

                // Then destroy our process
                ngrokProcess.destroy();

                // Wait for the process to terminate with increased timeout
                if (!ngrokProcess.waitFor(30, TimeUnit.SECONDS)) {
                    logger.warn("Ngrok process did not terminate gracefully, forcing shutdown...");
                    ngrokProcess.destroyForcibly();
                    if (!ngrokProcess.waitFor(10, TimeUnit.SECONDS)) {
                        logger.error("Failed to forcibly terminate ngrok process");
                    }
                }

                // Clear the public URL
                publicUrl = null;
                System.clearProperty("app.base-url");
            } catch (Exception e) {
                logger.error("Error stopping ngrok tunnel", e);
            }
        }
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}