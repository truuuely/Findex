package com.findex.repository.indexdata;

import com.findex.dto.indexdata.IndexDataQuery;
import com.findex.dto.response.CursorPageResponse;

public interface IndexDataQueryRepository {
    CursorPageResponse findAll(IndexDataQuery indexDataQuery);
}
