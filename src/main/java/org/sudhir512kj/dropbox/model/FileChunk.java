package org.sudhir512kj.dropbox.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "dropbox_file_chunks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"file_id", "chunk_index"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileChunk {
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(name = "chunk_hash", nullable = false, length = 64)
    private String chunkHash;
    
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;
    
    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;
}