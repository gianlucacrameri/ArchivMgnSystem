package com.archiv.ArchivMgnSystem.dtos;

import com.archiv.ArchivMgnSystem.models.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ActivityLogPageDTO {
    private List<ActivityLog> entries;
    private long totalElements;
}
