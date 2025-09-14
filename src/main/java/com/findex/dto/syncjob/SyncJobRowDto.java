package com.findex.dto.syncjob;

import com.findex.enums.SyncJobResult;
import com.findex.enums.SyncJobType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SyncJobRowDto(
    SyncJobType jobType,
    String indexName,
    LocalDate targetDate,
    String worker,
    LocalDateTime jobTime,
    SyncJobResult result
) {}