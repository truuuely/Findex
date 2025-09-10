package com.findex.repository.indexinfo;

import static org.springframework.util.StringUtils.hasText;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.indexinfo.IndexInfoQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.entity.QIndexInfo;
import com.findex.enums.IndexSortField;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IndexInfoQueryRepositoryImpl implements IndexInfoQueryRepository{

    private final JPAQueryFactory queryFactory;
    private static final QIndexInfo i = QIndexInfo.indexInfo;

    @Override
    public CursorPageResponse findAll(IndexInfoQuery q) {
        // --- 0) 공통 파라미터 정규화
        int size = (q.size() != null && q.size() > 0) ? q.size() : IndexInfoQuery.DEFAULT_SIZE;
        IndexSortField sortField = IndexSortField.parse(q.sortField());
        boolean asc = "asc".equalsIgnoreCase(q.sortDirection());

        // --- 1) 필터(부분/완전 일치)
        //     - 분류명/지수명: 부분 일치
        //     - 즐겨찾기: 완전 일치
        BooleanBuilder where = new BooleanBuilder();
        if (hasText(q.indexClassification())) {
            where.and(i.indexClassification.contains(q.indexClassification()));
        }
        if (hasText(q.indexName())) {
            where.and(i.indexName.contains(q.indexName()));
        }
        if (q.favorite() != null) {
            where.and(i.favorite.eq(q.favorite()));
        }

        // --- 2) 커서, idAfter로 다음 페이지 범위 절단식 필터링
        BooleanExpression cursorRange = buildRangeFromCursor(sortField, asc, q.idAfter(), q.cursor());
        if (cursorRange != null) {
            where.and(cursorRange);
        }

        // --- 3) 정렬: 단일 정렬키 + id 타이브레이커
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(sortField, asc);

        // --- 4) 조회: plus-one 패턴
        List<IndexInfoDto> rows = queryFactory
            .select(Projections.constructor(IndexInfoDto.class,
                i.id,
                i.indexClassification,
                i.indexName,
                i.employedItemsCount,
                i.basePointInTime,
                i.baseIndex,
                i.sourceType,
                i.favorite
            ))
            .from(i)
            .where(where)
            .orderBy(orders.toArray(OrderSpecifier[]::new))
            .limit(size + 1)
            .fetch();

        // --- 5) plus-one 처리 및 다음 커서 생성
        boolean hasNext = rows.size() > size;
        Long nextIdAfter = null;
        String nextCursor = null;

        if (hasNext) {
            rows = rows.subList(0, size);
        }
        if (hasNext && !rows.isEmpty()) {
            IndexInfoDto last = rows.get(rows.size() - 1);
            nextIdAfter = last.id();
            nextCursor = switch (sortField) {
                case INDEX_CLASSIFICATION -> last.indexClassification();
                case INDEX_NAME -> last.indexName();
                case EMPLOYED_ITEMS_COUNT -> String.valueOf(last.employedItemsCount());
            };
        }

        return new CursorPageResponse(
            rows,
            nextCursor,
            nextIdAfter,
            size,
            rows.size(),
            hasNext
        );
    }

    /**
     * 커서/파라미터 → 키셋 범위 조건(WHERE).
     * - 개념: (정렬필드, id) 튜플 비교로 "다음 구간"만 잘라옵니다.
     *   ASC  : F > base  OR (F = base AND id > idAfter)
     *   DESC : F < base  OR (F = base AND id < idAfter)
     */
    private BooleanExpression buildRangeFromCursor(
        IndexSortField sortField,
        boolean asc,
        Long idAfter,
        String cursor
    ) {
        if (idAfter == null || !hasText(cursor)) {
            return null;
        }

        switch (sortField) {
            case INDEX_NAME -> {
                StringExpression f = i.indexName.coalesce("");
                return asc
                    ? f.gt(cursor).or(f.eq(cursor).and(i.id.gt(idAfter)))
                    : f.lt(cursor).or(f.eq(cursor).and(i.id.lt(idAfter)));
            }
            case EMPLOYED_ITEMS_COUNT -> {
                int cursorInt;
                try {
                    cursorInt = Integer.parseInt(cursor);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid cursor: " + cursor);
                }

                NumberExpression<Integer> f = i.employedItemsCount.coalesce(Integer.MIN_VALUE);
                return asc
                    ? f.gt(cursorInt).or(f.eq(cursorInt).and(i.id.gt(idAfter)))
                    : f.lt(cursorInt).or(f.eq(cursorInt).and(i.id.lt(idAfter)));
            }
            default -> {
                StringExpression f = i.indexClassification.coalesce("");
                return asc
                    ? f.gt(cursor).or(f.eq(cursor).and(i.id.gt(idAfter)))
                    : f.lt(cursor).or(f.eq(cursor).and(i.id.lt(idAfter)));
            }
        }
    }

    /**
     * 선택된 단일 정렬필드에 대한 ORDER BY 생성.
     * - 타이브레이커로 id를 같은 방향으로 추가해 결정적 순서 보장.
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(IndexSortField sortField, boolean asc) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        Order dir = asc ? Order.ASC : Order.DESC;

        switch (sortField) {
            case INDEX_NAME ->
                orders.add(new OrderSpecifier<>(dir, i.indexName));
            case EMPLOYED_ITEMS_COUNT ->
                orders.add(new OrderSpecifier<>(dir, i.employedItemsCount));
            default ->
                orders.add(new OrderSpecifier<>(dir, i.indexClassification));
        }
        orders.add(new OrderSpecifier<>(dir, i.id));
        return orders;
    }
}
