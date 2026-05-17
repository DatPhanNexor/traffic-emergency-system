package com.example.suco.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        // InputStream serviceAccount =
        //         new ClassPathResource("canhbao-b600b-firebase-adminsdk-fbsvc-85c5d04d98.json").getInputStream();

        Resource serviceAccountResource = new ClassPathResource("canhbao-b600b-firebase-adminsdk-fbsvc-85c5d04d98.json");
        if (!serviceAccountResource.exists()) {
            System.out.println("Firebase service account JSON not found. Skipping Firebase initialization.");
            return;
        }

        InputStream serviceAccount = serviceAccountResource.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
