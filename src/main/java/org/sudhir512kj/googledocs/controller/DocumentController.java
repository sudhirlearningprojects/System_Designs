package org.sudhir512kj.googledocs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.googledocs.dto.*;
import org.sudhir512kj.googledocs.model.Permission;
import org.sudhir512kj.googledocs.service.DocumentService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    
    @PostMapping
    public ResponseEntity<DocumentDTO> createDocument(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(documentService.createDocument(
            request.get("title"), 
            request.get("userId")
        ));
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable String documentId) {
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentDTO>> getUserDocuments(@PathVariable String userId) {
        return ResponseEntity.ok(documentService.getUserDocuments(userId));
    }
    
    @PostMapping("/{documentId}/share")
    public ResponseEntity<Void> shareDocument(@PathVariable String documentId, 
                                               @RequestBody Map<String, String> request) {
        documentService.shareDocument(
            documentId, 
            request.get("userId"), 
            Permission.PermissionType.valueOf(request.get("permissionType")),
            request.get("grantedBy")
        );
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{documentId}/watermark")
    public ResponseEntity<Void> addWatermark(@PathVariable String documentId, 
                                              @RequestBody Map<String, String> request) {
        documentService.addWatermark(documentId, request.get("watermark"));
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<VersionDTO>> getVersionHistory(@PathVariable String documentId) {
        return ResponseEntity.ok(documentService.getVersionHistory(documentId));
    }
    
    @PostMapping("/{documentId}/versions")
    public ResponseEntity<Void> saveVersion(@PathVariable String documentId, 
                                             @RequestBody Map<String, String> request) {
        documentService.saveVersion(documentId, request.get("userId"), request.get("description"));
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{documentId}/versions/{versionId}/restore")
    public ResponseEntity<DocumentDTO> restoreVersion(@PathVariable String documentId, 
                                                       @PathVariable String versionId,
                                                       @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(documentService.restoreVersion(documentId, versionId, request.get("userId")));
    }
}
