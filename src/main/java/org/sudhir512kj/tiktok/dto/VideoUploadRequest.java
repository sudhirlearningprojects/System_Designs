package org.sudhir512kj.tiktok.dto;

import lombok.Data;

@Data
public class VideoUploadRequest {
    private String caption;
    private Boolean isPublic = true;
    private Boolean allowComments = true;
    private Boolean allowDuet = true;
    private Boolean allowStitch = true;
}
