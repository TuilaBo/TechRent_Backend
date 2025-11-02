package com.rentaltech.techrental.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseNotificationService {

    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fcm.api-url:https://fcm.googleapis.com/v1}")
    private String apiUrl;

    @Value("${fcm.project-id:}")
    private String projectId;

    @Value("${fcm.service-account-file:}")
    private String serviceAccountFile;

    private volatile GoogleCredentials cachedCredentials;

    public void sendNotification(String token, String title, String body, NotificationType type) {
        if (!StringUtils.hasText(projectId)) {
            log.debug("FCM project id is not configured. Skip sending notification.");
            return;
        }
        if (!StringUtils.hasText(serviceAccountFile)) {
            log.debug("FCM service account file is not configured. Skip sending notification.");
            return;
        }
        if (!StringUtils.hasText(token)) {
            log.debug("FCM token is empty. Skip sending notification.");
            return;
        }

        try {
            String accessToken = fetchAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                log.debug("Unable to obtain FCM access token. Skip sending notification.");
                return;
            }

            Map<String, Object> message = new HashMap<>();
            message.put("token", token);

            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            message.put("notification", notification);

            Map<String, Object> data = new HashMap<>();
            data.put("type", type.name());
            data.put("message", body);
            message.put("data", data);

            Map<String, Object> payload = Map.of("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            String endpoint = String.format("%s/projects/%s/messages:send", apiUrl, projectId);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            restTemplate.postForEntity(endpoint, request, String.class);
        } catch (Exception ex) {
            log.warn("Failed to send FCM notification: {}", ex.getMessage());
        }
    }

    private String fetchAccessToken() throws IOException {
        GoogleCredentials credentials = getCredentials();
        if (credentials == null) {
            return null;
        }
        synchronized (credentials) {
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token == null) {
                credentials.refresh();
                token = credentials.getAccessToken();
            }
            return token != null ? token.getTokenValue() : null;
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        if (cachedCredentials == null) {
            synchronized (this) {
                if (cachedCredentials == null) {
                    try (InputStream inputStream = resolveServiceAccountStream()) {
                        if (inputStream == null) {
                            return null;
                        }
                        cachedCredentials = GoogleCredentials.fromStream(inputStream)
                                .createScoped(Collections.singletonList(FCM_SCOPE));
                    }
                }
            }
        }
        return cachedCredentials;
    }

    private InputStream resolveServiceAccountStream() throws IOException {
        if (!StringUtils.hasText(serviceAccountFile)) {
            return null;
        }
        String trimmed = serviceAccountFile.trim();
        if (trimmed.startsWith("{")) {
            return new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8));
        }
        return Files.newInputStream(Path.of(serviceAccountFile));
    }
}
