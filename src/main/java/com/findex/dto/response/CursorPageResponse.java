package com.findex.dto.response;

import java.util.List;

public record CursorPageResponse(
    List<?> content,
    String nextCursor,
    Long nextIdAfter,
    int size,
    long totalElements,
    boolean hasNext
) {

}
