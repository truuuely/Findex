package com.findex.repository.syncjob;

import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.SyncJobQuery;

public interface SyncJobQueryRepository {

    CursorPageResponse findAll(SyncJobQuery query);
}
