package com.findex.util;

import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;

public class CacheKeys {

    public static String syncIndexData(IndexDataOpenApiSyncRequest request) {
        return "info=" + request.indexInfoIds() +
            "|start=" + request.baseDateFrom() +
            "|end=" + request.baseDateTo();
    }
}
