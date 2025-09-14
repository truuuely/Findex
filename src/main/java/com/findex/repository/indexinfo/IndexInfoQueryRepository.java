package com.findex.repository.indexinfo;

import com.findex.dto.indexinfo.IndexInfoQuery;
import com.findex.dto.response.CursorPageResponse;

public interface IndexInfoQueryRepository {

    CursorPageResponse findAll(IndexInfoQuery query);
}
