package org.sudhir512kj.tiktok.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.sudhir512kj.tiktok.websocket.LiveStreamWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final LiveStreamWebSocketHandler liveStreamWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveStreamWebSocketHandler, "/ws/live")
                .setAllowedOrigins("*");
    }
}
