package com.findex.repository.syncjob;

import static com.findex.entity.QSyncJob.syncJob;
import static com.findex.enums.SyncJobSortField.TARGET_DATE;
import static org.springframework.util.StringUtils.hasText;

import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.SyncJobDto;
import com.findex.dto.syncjob.SyncJobQuery;
import com.findex.enums.SyncJobSortField;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SyncJobQueryRepositoryImpl implements SyncJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse findAll(SyncJobQuery query) {
        List<SyncJobDto> rows =
            queryFactory
                .select(
                    Projections.constructor(
                        SyncJobDto.class,
                        syncJob.id,
                        syncJob.jobType,
                        syncJob.indexInfoId,
                        syncJob.targetDate,
                        syncJob.worker,
                        syncJob.jobTime,
                        syncJob.result))
                .from(syncJob)
                .where(
                    query.baseDateFrom() != null ? syncJob.targetDate.goe(query.baseDateFrom()) : null,
                    query.baseDateTo() != null ? syncJob.targetDate.loe(query.baseDateTo()) : null,
                    query.jobType() != null ? syncJob.jobType.eq(query.jobType()) : null,
                    query.indexInfoId() != null ? syncJob.indexInfoId.eq(query.indexInfoId()) : null,
                    query.jobTimeFrom() != null ? syncJob.jobTime.goe(query.jobTimeFrom()) : null,
                    query.jobTimeTo() != null ? syncJob.jobTime.loe(query.jobTimeTo()) : null,
                    query.status() != null ? syncJob.result.eq(query.status()) : null,
                    buildRangeFromCursor(
                        query.sortFieldEnum(), query.asc(), query.idAfter(), query.cursor()))
                .orderBy(buildOrderSpecifiers(query.sortFieldEnum(), query.asc()))
                .limit(query.size() + 1)
                .fetch();

        Long total =
            queryFactory
                .select(syncJob.count())
                .from(syncJob)
                .where(
                    query.baseDateFrom() != null ? syncJob.targetDate.goe(query.baseDateFrom()) : null,
                    query.baseDateTo() != null ? syncJob.targetDate.loe(query.baseDateTo()) : null,
                    query.jobType() != null ? syncJob.jobType.eq(query.jobType()) : null,
                    query.indexInfoId() != null ? syncJob.indexInfoId.eq(query.indexInfoId()) : null,
                    query.jobTimeFrom() != null ? syncJob.jobTime.goe(query.jobTimeFrom()) : null,
                    query.jobTimeTo() != null ? syncJob.jobTime.loe(query.jobTimeTo()) : null,
                    query.status() != null ? syncJob.result.eq(query.status()) : null
                )
                .fetchOne();

    long totalElements = (total != null) ? total : 0;

    if (rows.size() <= query.size()) {
      return new CursorPageResponse(rows, null, null, query.size(), totalElements, false);
    }

    rows = rows.subList(0, query.size());
    SyncJobDto last = rows.get(rows.size() - 1);

    return new CursorPageResponse(
        rows,
        query.sortFieldEnum() == TARGET_DATE ? String.valueOf(last.targetDate()) : String.valueOf(last.jobTime()),
        last.id(),
        query.size(),
        totalElements,
        true);
    }

    private BooleanExpression buildRangeFromCursor(
      SyncJobSortField sortField,
      boolean asc,
      Long idAfter,
      String cursor
    ) {
    if (idAfter == null || !hasText(cursor)) {
      return null;
    }

    if (sortField == TARGET_DATE) {
        LocalDate cursorDate;
        try {
            cursorDate = LocalDate.parse(cursor);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid cursor: " + cursor);
        }
        DateExpression<LocalDate> f = syncJob.targetDate;
        return asc
            ? f.gt(cursorDate).or(f.eq(cursorDate).and(syncJob.id.gt(idAfter)))
            : f.lt(cursorDate).or(f.eq(cursorDate).and(syncJob.id.lt(idAfter)));
    }
    LocalDateTime cursorTime;
    try {
        cursorTime = LocalDateTime.parse(cursor);
    } catch (Exception e) {
        throw new IllegalArgumentException("invalid cursor: " + cursor);
    }
    DateTimeExpression<LocalDateTime> f = syncJob.jobTime;
    return asc
        ? f.gt(cursorTime).or(f.eq(cursorTime).and(syncJob.id.gt(idAfter)))
        : f.lt(cursorTime).or(f.eq(cursorTime).and(syncJob.id.lt(idAfter)));
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(SyncJobSortField sortField, boolean asc) {
        Order sortDirection = asc ? Order.ASC : Order.DESC;

        if (sortField == TARGET_DATE) {
            return new OrderSpecifier<?>[] {
                new OrderSpecifier<>(sortDirection, syncJob.targetDate),
                new OrderSpecifier<>(Order.ASC, syncJob.id)
            };
        }
        return new OrderSpecifier<?>[] {
            new OrderSpecifier<>(sortDirection, syncJob.jobTime),
            new OrderSpecifier<>(Order.ASC, syncJob.id)
        };
    }
}