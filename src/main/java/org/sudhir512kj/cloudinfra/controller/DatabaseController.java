package org.sudhir512kj.cloudinfra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.cloudinfra.dto.CreateDatabaseRequest;
import org.sudhir512kj.cloudinfra.model.ManagedDatabase;
import org.sudhir512kj.cloudinfra.service.DatabaseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database")
@RequiredArgsConstructor
public class DatabaseController {
    private final DatabaseService databaseService;
    
    @PostMapping("/instances")
    public ResponseEntity<ManagedDatabase> createDatabase(
            @RequestHeader("X-Account-Id") String accountId,
            @RequestBody CreateDatabaseRequest request) {
        return ResponseEntity.ok(databaseService.createDatabase(accountId, request));
    }
    
    @GetMapping("/instances")
    public ResponseEntity<List<ManagedDatabase>> listDatabases(
            @RequestHeader("X-Account-Id") String accountId) {
        return ResponseEntity.ok(databaseService.listDatabases(accountId));
    }
}
