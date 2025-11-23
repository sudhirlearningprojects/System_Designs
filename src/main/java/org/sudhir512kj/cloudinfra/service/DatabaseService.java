package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cloudinfra.dto.CreateDatabaseRequest;
import org.sudhir512kj.cloudinfra.model.ManagedDatabase;
import org.sudhir512kj.cloudinfra.model.ResourceState;
import org.sudhir512kj.cloudinfra.repository.ManagedDatabaseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService {
    private final ManagedDatabaseRepository databaseRepository;
    
    @Transactional
    public ManagedDatabase createDatabase(String accountId, CreateDatabaseRequest request) {
        ManagedDatabase db = new ManagedDatabase();
        db.setId("db-" + UUID.randomUUID().toString().substring(0, 8));
        db.setName(request.getName());
        db.setRegion(request.getRegion());
        db.setAccountId(accountId);
        db.setEngine(request.getEngine());
        db.setEngineVersion(request.getEngineVersion());
        db.setInstanceClass(request.getInstanceClass());
        db.setStorageGb(request.getStorageGb());
        db.setMasterUsername(request.getMasterUsername());
        db.setMultiAz(request.getMultiAz());
        db.setBackupEnabled(true);
        db.setBackupRetentionDays(7);
        db.setEndpoint(db.getId() + ".db.cloud.com");
        db.setPort(getDefaultPort(request.getEngine()));
        db.setMultiAz(request.getMultiAz());
        
        return databaseRepository.save(db);
    }
    
    private Integer getDefaultPort(ManagedDatabase.DBEngine engine) {
        if (engine == null) return 5432;
        return switch (engine) {
            case POSTGRES -> 5432;
            case MYSQL -> 3306;
            case MONGODB -> 27017;
            case REDIS -> 6379;
            case CASSANDRA -> 9042;
        };
    }
    
    public List<ManagedDatabase> listDatabases(String accountId) {
        return databaseRepository.findByAccountId(accountId);
    }
}
