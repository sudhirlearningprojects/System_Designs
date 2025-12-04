package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.spotify.model.UserLibrary;
import org.sudhir512kj.spotify.repository.UserLibraryRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserLibraryService {
    private final UserLibraryRepository userLibraryRepository;
    
    @Transactional
    public void addToLibrary(String userId, String entityId, UserLibrary.EntityType entityType) {
        if (userLibraryRepository.existsByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId)) {
            throw new RuntimeException("Already in library");
        }
        
        UserLibrary library = UserLibrary.builder()
            .userId(userId)
            .entityType(entityType)
            .entityId(entityId)
            .addedAt(LocalDateTime.now())
            .build();
        
        userLibraryRepository.save(library);
    }
    
    @Transactional
    public void removeFromLibrary(String userId, String entityId, UserLibrary.EntityType entityType) {
        userLibraryRepository.deleteByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId);
    }
    
    public List<UserLibrary> getUserLibrary(String userId, UserLibrary.EntityType entityType) {
        return userLibraryRepository.findByUserIdAndEntityType(userId, entityType);
    }
}
