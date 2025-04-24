package com.scango.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableScheduling
@Profile("ngrok")
public class NgrokUrlConfig {

    private static final Logger logger = LoggerFactory.getLogger(NgrokUrlConfig.class);
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    /**
     * Checks every 10 minutes if the ngrok URL has changed and updates the configuration accordingly
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void updateNgrokUrl() {
        try {
            String currentNgrokUrl = getNgrokUrl();
            if (currentNgrokUrl != null && !currentNgrokUrl.equals(baseUrl)) {
                logger.info("Detected ngrok URL change from {} to {}", baseUrl, currentNgrokUrl);
                baseUrl = currentNgrokUrl;
                
                // Update the properties file
                updateConfigFile(currentNgrokUrl);
                
                logger.info("Updated ngrok URL to: {}", currentNgrokUrl);
            }
        } catch (Exception e) {
            logger.error("Failed to update ngrok URL", e);
        }
    }
    
    private String getNgrokUrl() {
        try {
            URL url = new URL("http://localhost:4040/api/tunnels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() != 200) {
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            
            // Find the HTTPS URL
            Pattern pattern = Pattern.compile("\"public_url\":\"(https://[^\"]+)\"");
            Matcher matcher = pattern.matcher(content.toString());
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error getting ngrok URL", e);
            return null;
        }
    }
    
    private void updateConfigFile(String newNgrokUrl) {
        try {
            String configPath = "ngrok-config.properties";
            String content = "# Temporary configuration - do not commit\n" +
                             "app.base-url=" + newNgrokUrl + "\n" +
                             "file.upload-dir=./uploads\n" +
                             "spring.profiles.active=ngrok\n";
            
            Files.writeString(Paths.get(configPath), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            logger.error("Failed to update ngrok config file", e);
        }
    }
} 