package com.findex.repository.autosyncconfig;

import com.findex.dto.autosyncconfig.AutoSyncConfigQuery;
import com.findex.dto.response.CursorPageResponse;

public interface AutoSyncConfigQueryRepository {
    CursorPageResponse findAll(AutoSyncConfigQuery query);
}
