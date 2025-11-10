package org.sudhir512kj.googledocs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.googledocs.dto.*;
import org.sudhir512kj.googledocs.model.*;
import org.sudhir512kj.googledocs.repository.*;
import org.sudhir512kj.googledocs.ot.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final VersionRepository versionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OperationalTransform operationalTransform;
    
    @Transactional
    public DocumentDTO createDocument(String title, String userId) {
        Document document = Document.builder()
            .title(title)
            .content("")
            .ownerId(userId)
            .status(Document.DocumentStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .version(1)
            .build();
        
        Permission permission = Permission.builder()
            .userId(userId)
            .type(Permission.PermissionType.OWNER)
            .grantedAt(LocalDateTime.now())
            .grantedBy(userId)
            .build();
        permission.setDocument(document);
        document.getPermissions().add(permission);
        
        document = documentRepository.save(document);
        
        Version version = Version.builder()
            .document(document)
            .versionNumber(1)
            .content("")
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .description("Initial version")
            .build();
        versionRepository.save(version);
        
        redisTemplate.opsForValue().set("doc:" + document.getId(), document.getContent());
        
        return toDTO(document);
    }
    
    @Transactional
    public DocumentDTO updateDocument(String documentId, Operation operation) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        String cachedContent = (String) redisTemplate.opsForValue().get("doc:" + documentId);
        String currentContent = cachedContent != null ? cachedContent : document.getContent();
        
        List<Operation> pendingOps = getPendingOperations(documentId);
        for (Operation pendingOp : pendingOps) {
            if (!pendingOp.getUserId().equals(operation.getUserId())) {
                operation = operationalTransform.transform(operation, pendingOp);
            }
        }
        
        String newContent = operationalTransform.applyOperation(currentContent, operation);
        
        document.setContent(newContent);
        document.setUpdatedAt(LocalDateTime.now());
        document.setVersion(document.getVersion() + 1);
        document = documentRepository.save(document);
        
        redisTemplate.opsForValue().set("doc:" + documentId, newContent);
        redisTemplate.opsForList().rightPush("ops:" + documentId, operation);
        
        return toDTO(document);
    }
    
    @Transactional
    public void saveVersion(String documentId, String userId, String description) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        Version version = Version.builder()
            .document(document)
            .versionNumber(document.getVersion())
            .content(document.getContent())
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .description(description)
            .build();
        
        versionRepository.save(version);
    }
    
    public List<VersionDTO> getVersionHistory(String documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
            .stream()
            .map(this::toVersionDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public DocumentDTO restoreVersion(String documentId, String versionId, String userId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        Version version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        
        document.setContent(version.getContent());
        document.setUpdatedAt(LocalDateTime.now());
        document.setVersion(document.getVersion() + 1);
        document = documentRepository.save(document);
        
        redisTemplate.opsForValue().set("doc:" + documentId, version.getContent());
        
        return toDTO(document);
    }
    
    @Transactional
    public void shareDocument(String documentId, String userId, Permission.PermissionType permissionType, String grantedBy) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        Permission permission = Permission.builder()
            .userId(userId)
            .type(permissionType)
            .grantedAt(LocalDateTime.now())
            .grantedBy(grantedBy)
            .build();
        permission.setDocument(document);
        
        document.getPermissions().add(permission);
        documentRepository.save(document);
    }
    
    public DocumentDTO getDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        return toDTO(document);
    }
    
    public List<DocumentDTO> getUserDocuments(String userId) {
        List<Document> ownedDocs = documentRepository.findByOwnerId(userId);
        List<Document> sharedDocs = documentRepository.findBySharedWithUser(userId);
        
        Set<Document> allDocs = new HashSet<>();
        allDocs.addAll(ownedDocs);
        allDocs.addAll(sharedDocs);
        
        return allDocs.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void addWatermark(String documentId, String watermark) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        document.setWatermark(watermark);
        documentRepository.save(document);
    }
    
    private List<Operation> getPendingOperations(String documentId) {
        List<Object> ops = redisTemplate.opsForList().range("ops:" + documentId, 0, -1);
        return ops != null ? ops.stream()
            .map(op -> (Operation) op)
            .collect(Collectors.toList()) : new ArrayList<>();
    }
    
    private DocumentDTO toDTO(Document document) {
        return DocumentDTO.builder()
            .id(document.getId())
            .title(document.getTitle())
            .content(document.getContent())
            .ownerId(document.getOwnerId())
            .status(document.getStatus().name())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .watermark(document.getWatermark())
            .tags(document.getTags())
            .version(document.getVersion())
            .permissions(document.getPermissions().stream()
                .map(p -> PermissionDTO.builder()
                    .userId(p.getUserId())
                    .type(p.getType().name())
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
    
    private VersionDTO toVersionDTO(Version version) {
        return VersionDTO.builder()
            .id(version.getId())
            .versionNumber(version.getVersionNumber())
            .content(version.getContent())
            .createdBy(version.getCreatedBy())
            .createdAt(version.getCreatedAt())
            .description(version.getDescription())
            .build();
    }
}
