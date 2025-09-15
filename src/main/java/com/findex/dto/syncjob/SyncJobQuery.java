package com.findex.dto.syncjob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.findex.enums.JobType;
import com.findex.enums.SyncJobSortField;
import com.findex.enums.SyncJobStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SyncJobQuery(
    JobType jobType,
    Long indexInfoId,
    LocalDate baseDateFrom,
    LocalDate baseDateTo,
    LocalDateTime jobTimeFrom,
    LocalDateTime jobTimeTo,
    SyncJobStatus status,
    Long idAfter,
    String cursor,
    String sortField,
    String sortDirection,
    Integer size,

    @JsonIgnore
    SyncJobSortField sortFieldEnum,

    @JsonIgnore
    Boolean asc
) {
    public static final String DEFAULT_SORT_FIELD = "jobTime";
    public static final String DEFAULT_SORT_DIRECTION = "desc";
    public static final int DEFAULT_SIZE = 10;

    public SyncJobQuery {
        if (sortField == null) {
            sortField = DEFAULT_SORT_FIELD;
        }
        if (sortDirection == null) {
            sortDirection = DEFAULT_SORT_DIRECTION;
        }
        if (size == null) {
            size = DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        sortFieldEnum = SyncJobSortField.from(sortField);
        asc = "asc".equalsIgnoreCase(sortDirection);
    }
}
