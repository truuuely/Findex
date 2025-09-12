package com.findex.repository.indexdata;

import com.findex.dto.indexdata.IndexDataQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.entity.IndexData;

import java.util.stream.Stream;

public interface IndexDataQueryRepository {
    CursorPageResponse findAll(IndexDataQuery indexDataQuery);
    Stream<IndexData> findAllForExport(IndexDataQuery indexDataQuery);
}
