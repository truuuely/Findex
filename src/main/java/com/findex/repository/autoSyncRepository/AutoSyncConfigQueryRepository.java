package com.findex.repository.autoSyncRepository;

import com.findex.dto.autoSyncConfig.AutoSyncConfigQuery;
import com.findex.dto.response.CursorPageResponse;

public interface AutoSyncConfigQueryRepository {
    CursorPageResponse findAll(AutoSyncConfigQuery query);
}
