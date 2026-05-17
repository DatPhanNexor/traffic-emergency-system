package com.example.suco.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {

    @Value("${MAPBOX_PUBLIC_TOKEN}")
    private String mapboxToken;

    public String getMapboxToken() {
        return mapboxToken;
    }
}