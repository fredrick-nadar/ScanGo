package com.scango.app.service;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NgrokService {
    private static final Logger logger = LoggerFactory.getLogger(NgrokService.class);
    private NgrokClient ngrokClient;
    private Tunnel tunnel;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${ngrok.auth-token:2t1bmDHElJinFHZfK7E3pyaSG5z_7dUU3krqPnhuRfCKBdaGm}")
    private String authToken;

    @PostConstruct
    public void startNgrok() {
        try {
            logger.info("Starting ngrok tunnel for port {}", serverPort);

            // Configure and start ngrok
            JavaNgrokConfig config = new JavaNgrokConfig.Builder()
                    .withAuthToken(authToken)
                    .build();

            ngrokClient = new NgrokClient.Builder()
                    .withJavaNgrokConfig(config)
                    .build();

            // Create HTTP tunnel
            CreateTunnel createTunnel = new CreateTunnel.Builder()
                    .withAddr(serverPort)
                    .build();

            tunnel = ngrokClient.connect(createTunnel);

            logger.info("Ngrok tunnel established at: {}", tunnel.getPublicUrl());

            // Update the application's base URL
            System.setProperty("app.base-url", tunnel.getPublicUrl());

        } catch (Exception e) {
            logger.error("Failed to start ngrok tunnel", e);
            throw new RuntimeException("Failed to start ngrok tunnel", e);
        }
    }

    @PreDestroy
    public void stopNgrok() {
        if (ngrokClient != null) {
            try {
                logger.info("Stopping ngrok tunnel");
                ngrokClient.disconnect(tunnel);
                ngrokClient.kill();
            } catch (Exception e) {
                logger.error("Error stopping ngrok tunnel", e);
            }
        }
    }

    public String getPublicUrl() {
        return tunnel != null ? tunnel.getPublicUrl() : null;
    }
}