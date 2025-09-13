package com.findex.dto.indexinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.findex.enums.IndexSortField;

public record IndexInfoQuery(
    String indexClassification,
    String indexName,
    Boolean favorite,
    Long idAfter,
    String cursor,
    String sortField,
    String sortDirection,
    Integer size,

    @JsonIgnore
    IndexSortField sortFieldEnum,

    @JsonIgnore
    boolean asc
) {
    public static final String DEFAULT_SORT_FIELD = "indexClassification";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    public static final int DEFAULT_SIZE = 10;

    public IndexInfoQuery {
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
        sortFieldEnum = IndexSortField.from(sortField);
        asc = "asc".equalsIgnoreCase(sortDirection);
    }
}
