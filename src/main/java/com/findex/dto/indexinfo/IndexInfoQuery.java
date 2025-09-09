package com.findex.dto.indexinfo;

import org.springframework.data.domain.Sort;

public record IndexInfoQuery(
    String indexClassification,
    String indexName,
    Boolean favorite,
    Long idAfter,
    Long cursor,
    String sortField,
    Sort.Direction sortDirection,
    Integer size
) {
    public static final int DEFAULT_SIZE = 10;
    public static final String DEFAULT_SORT_FIELD = "indexClassification";
    public static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.ASC;

    public IndexInfoQuery {
        if (size == null) size = DEFAULT_SIZE;
        if (sortField == null) sortField = DEFAULT_SORT_FIELD;
        if (sortDirection == null) sortDirection = DEFAULT_SORT_DIRECTION;
    }
}
