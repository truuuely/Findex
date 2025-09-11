package com.findex.dto.autoSyncConfig;

public record AutoSyncConfigQuery(
        Long indexInfoId,
        Boolean enabled,
        Long idAfter,
        String cursor,
        String sortField,
        String sortDirection,
        Integer size
) {
    public static final String SORT_FIELD = "indexInfo.indexName";
    public static final String SORT_DIRECTION = "asc";
    public static final int SIZE_FIELD = 10;

    public AutoSyncConfigQuery{
        if (sortField == null) {sortField = SORT_FIELD;}
        if (sortDirection == null) {sortDirection = SORT_DIRECTION;}
        if (size == null) {size = SIZE_FIELD;}

    }
}
