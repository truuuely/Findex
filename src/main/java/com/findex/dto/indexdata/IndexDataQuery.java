package com.findex.dto.indexdata;

import com.findex.enums.IndexDataSortField;
import jakarta.validation.constraints.Positive;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public record IndexDataQuery(
        Long indexInfoId,
        LocalDate startDate,
        LocalDate endDate,
        Long idAfter,
        String cursor,
        String sortField,
        String sortDirection,
        @Positive(message = "Page size must not be less than one")
        Integer size
) {
    public static final LocalDate START_DATE_DEFAULT_VALUE = LocalDate.of(1900, 1, 1);
    public static final String SORT_DIRECTION_DEFAULT_VALUE = "desc";
    public static final int SIZE_DEFAULT_VALUE = 10;

    public IndexDataQuery {
        if (startDate == null) {
            startDate = START_DATE_DEFAULT_VALUE;
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (!StringUtils.hasText(sortDirection)) {
            sortDirection = SORT_DIRECTION_DEFAULT_VALUE;
        }
        if (size == null) {
            size = SIZE_DEFAULT_VALUE;
        }
    }

    public IndexDataSortField getSortFieldEnum() {
        return IndexDataSortField.from(sortField);
    }
}
