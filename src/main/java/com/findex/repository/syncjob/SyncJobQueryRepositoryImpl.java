package com.findex.repository.syncjob;

import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.SyncJobQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SyncJobQueryRepositoryImpl implements SyncJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse findAll(SyncJobQuery query) {
        return null;
    }
}
