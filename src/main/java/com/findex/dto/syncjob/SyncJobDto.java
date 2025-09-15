package com.findex.dto.syncjob;

import com.findex.enums.JobType;
import com.findex.enums.SyncJobStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SyncJobDto(
    Long id,
    JobType jobType,
    Long indexInfoId,
    LocalDate targetDate,
    String worker,
    LocalDateTime jobTime,
    SyncJobStatus result
) {}
