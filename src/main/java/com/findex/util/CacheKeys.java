package com.findex.util;

import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;

public final class CacheKeys {
    private CacheKeys() {}

    public static String syncIndexData(IndexDataOpenApiSyncRequest request) {
        return "info=" + request.indexInfoIds() +
            "|start=" + request.baseDateFrom() +
            "|end=" + request.baseDateTo();
    }
}
