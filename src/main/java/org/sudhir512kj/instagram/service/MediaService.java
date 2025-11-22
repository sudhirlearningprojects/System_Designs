package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    
    @Value("${media.storage.path:/tmp/instagram/media}")
    private String storagePath;
    
    @Value("${media.base.url:http://localhost:8087/media}")
    private String baseUrl;

    public String uploadImage(MultipartFile file, Long userId) throws IOException {
        validateImage(file);
        
        String fileId = UUID.randomUUID().toString();
        String extension = getExtension(file.getOriginalFilename());
        String fileName = fileId + extension;
        
        Path userDir = Paths.get(storagePath, "images", userId.toString());
        Files.createDirectories(userDir);
        
        Path filePath = userDir.resolve(fileName);
        file.transferTo(filePath.toFile());
        
        // Generate thumbnail
        generateThumbnail(filePath.toFile(), userDir.resolve("thumb_" + fileName).toFile());
        
        return baseUrl + "/images/" + userId + "/" + fileName;
    }

    public String uploadVideo(MultipartFile file, Long userId) throws IOException {
        validateVideo(file);
        
        String fileId = UUID.randomUUID().toString();
        String extension = getExtension(file.getOriginalFilename());
        String fileName = fileId + extension;
        
        Path userDir = Paths.get(storagePath, "videos", userId.toString());
        Files.createDirectories(userDir);
        
        Path filePath = userDir.resolve(fileName);
        file.transferTo(filePath.toFile());
        
        return baseUrl + "/videos/" + userId + "/" + fileName;
    }

    public void deleteMedia(String mediaUrl) {
        try {
            String relativePath = mediaUrl.replace(baseUrl, "");
            Path filePath = Paths.get(storagePath, relativePath);
            Files.deleteIfExists(filePath);
            
            // Delete thumbnail if exists
            Path thumbPath = filePath.getParent().resolve("thumb_" + filePath.getFileName());
            Files.deleteIfExists(thumbPath);
        } catch (IOException e) {
            log.error("Failed to delete media: {}", mediaUrl, e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Invalid image format");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new RuntimeException("Image size exceeds 10MB");
        }
    }

    private void validateVideo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new RuntimeException("Invalid video format");
        }
        
        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            throw new RuntimeException("Video size exceeds 100MB");
        }
    }

    private void generateThumbnail(File source, File destination) {
        try {
            BufferedImage original = ImageIO.read(source);
            int width = 150;
            int height = (int) (original.getHeight() * ((double) width / original.getWidth()));
            
            BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, width, height, null);
            g.dispose();
            
            ImageIO.write(thumbnail, "jpg", destination);
        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".jpg";
    }
}
