package org.sudhir512kj.spotify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.spotify.dto.StreamRequest;
import org.sudhir512kj.spotify.service.StreamingService;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamingController {
    private final StreamingService streamingService;
    
    @PostMapping
    public ResponseEntity<byte[]> streamTrack(@RequestBody StreamRequest request) {
        byte[] audioData = streamingService.streamTrack(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(audioData.length);
        headers.set("Accept-Ranges", "bytes");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(audioData);
    }
}
