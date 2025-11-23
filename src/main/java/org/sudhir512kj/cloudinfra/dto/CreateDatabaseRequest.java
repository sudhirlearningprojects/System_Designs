package org.sudhir512kj.cloudinfra.dto;

import lombok.Data;
import org.sudhir512kj.cloudinfra.model.ManagedDatabase;

@Data
public class CreateDatabaseRequest {
    private String name;
    private String region;
    private ManagedDatabase.DBEngine engine;
    private String engineVersion;
    private String instanceClass;
    private Integer storageGb;
    private String masterUsername;
    private Boolean multiAz;
}
