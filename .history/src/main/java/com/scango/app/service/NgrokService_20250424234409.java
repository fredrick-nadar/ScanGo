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
            logger.info("=== NGROK STARTUP SEQUENCE STARTED ===");
            logger.debug("Starting ngrok tunnel for port {}", serverPort);

            // Add shutdown hook with debug logging
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("=== JVM SHUTDOWN HOOK TRIGGERED ===");
                logger.debug("Initiating Ngrok shutdown from JVM shutdown hook");
                stopNgrok();
                logger.info("=== JVM SHUTDOWN HOOK COMPLETED ===");
            }));

            // Start ngrok process with debug logging
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ngrok", "http", String.valueOf(serverPort),
                    "--authtoken", authToken,
                    "--log", "stdout");

            logger.debug("Starting Ngrok process with command: {}", String.join(" ", processBuilder.command()));
            ngrokProcess = processBuilder.start();
            logger.debug("Ngrok process started with PID: {}", ngrokProcess.pid());

            // Wait for ngrok to start and get the URL
            waitForNgrokStart();

            logger.info("Ngrok tunnel established at: {}", publicUrl);
            logger.debug("Setting system property app.base-url to: {}", publicUrl);
            System.setProperty("app.base-url", publicUrl);

            logger.info("=== NGROK STARTUP SEQUENCE COMPLETED ===");
        } catch (Exception e) {
            logger.error("=== NGROK STARTUP SEQUENCE FAILED ===", e);
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
                logger.info("=== NGROK SHUTDOWN SEQUENCE STARTED ===");
                logger.debug("Current Ngrok process state - Alive: {}, Running: {}",
                        ngrokProcess.isAlive(),
                        "Process " + ngrokProcess.pid() + " is running");

                // First try to kill any existing ngrok processes
                try {
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        logger.debug("Executing Windows ngrok cleanup command");
                        Process killProcess = Runtime.getRuntime().exec("taskkill /F /IM ngrok.exe");
                        int exitCode = killProcess.waitFor();
                        logger.debug("Windows taskkill exit code: {}", exitCode);
                    } else {
                        logger.debug("Executing Unix ngrok cleanup command");
                        Process killProcess = Runtime.getRuntime().exec("pkill -f ngrok");
                        int exitCode = killProcess.waitFor();
                        logger.debug("Unix pkill exit code: {}", exitCode);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to kill existing ngrok processes", e);
                }

                // Then destroy our process
                logger.debug("Attempting graceful shutdown of Ngrok process {}", ngrokProcess.pid());
                ngrokProcess.destroy();

                // Wait for the process to terminate with increased timeout
                logger.debug("Waiting for Ngrok process to terminate...");
                boolean terminated = ngrokProcess.waitFor(30, TimeUnit.SECONDS);

                if (!terminated) {
                    logger.warn("Ngrok process did not terminate gracefully, forcing shutdown...");
                    ngrokProcess.destroyForcibly();
                    terminated = ngrokProcess.waitFor(10, TimeUnit.SECONDS);
                    logger.debug("Forcible shutdown result: process terminated = {}", terminated);
                }

                // Verify process state
                boolean isAlive = ngrokProcess.isAlive();
                logger.debug("Final Ngrok process state - Alive: {}", isAlive);

                if (isAlive) {
                    logger.error("Failed to terminate Ngrok process {}!", ngrokProcess.pid());
                } else {
                    logger.info("Ngrok process {} successfully terminated", ngrokProcess.pid());
                }

                // Clear the public URL
                logger.debug("Clearing Ngrok public URL and system properties");
                publicUrl = null;
                System.clearProperty("app.base-url");

                // Verify Ngrok is not running using platform-specific commands
                verifyNgrokStopped();

                logger.info("=== NGROK SHUTDOWN SEQUENCE COMPLETED ===");
            } catch (Exception e) {
                logger.error("Error during Ngrok shutdown sequence", e);
                // Try force kill as last resort
                ngrokProcess.destroyForcibly();
            } finally {
                ngrokProcess = null;
            }
        } else {
            logger.info("No Ngrok process to stop");
        }
    }

    private void verifyNgrokStopped() {
        try {
            logger.debug("Verifying Ngrok processes...");
            Process checkProcess;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                checkProcess = Runtime.getRuntime().exec("tasklist /FI \"IMAGENAME eq ngrok.exe\"");
            } else {
                checkProcess = Runtime.getRuntime().exec("pgrep -l ngrok");
            }

            // Read the process output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()))) {
                String line;
                boolean ngrokFound = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ngrok")) {
                        ngrokFound = true;
                        logger.warn("Found running Ngrok process: {}", line);
                    }
                }
                if (!ngrokFound) {
                    logger.info("Verified: No Ngrok processes running");
                }
            }
        } catch (Exception e) {
            logger.error("Error verifying Ngrok process status", e);
        }
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}