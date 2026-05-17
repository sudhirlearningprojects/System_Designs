package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.Content;
import org.sudhir512kj.netflix.repository.ContentCatalogRepository;
import java.util.List;
import java.util.Optional;

@Service
public class ContentService {
    
    @Autowired
    private ContentCatalogRepository contentRepository;
    
    public List<Content> searchContent(String query) {
        return contentRepository.findByTitleContainingIgnoreCase(query);
    }
    
    public List<Content> getContentByGenre(String genre) {
        return contentRepository.findByGenresContaining(genre);
    }
    
    public Optional<Content> getContentById(String contentId) {
        return contentRepository.findById(contentId);
    }
    
    public Content saveContent(Content content) {
        return contentRepository.save(content);
    }
}
