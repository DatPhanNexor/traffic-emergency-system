package com.example.suco.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt một "broker" đơn giản để gửi tin nhắn về client
        config.enableSimpleBroker("/topic");
        // Prefix cho các message gửi từ client lên server (nếu cần)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint để client kết nối vào
        registry.addEndpoint("/ws-suco")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}