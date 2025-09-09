package com.findex.repository.indexinfo;

import static org.springframework.util.StringUtils.hasText;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.indexinfo.IndexInfoQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.entity.QIndexInfo;
import com.findex.enums.IndexSortField;
import com.findex.util.CursorCodec;
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
        //     - 분류명/지수명: 부분 일치(대소문자 무시)
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

        // --- 2) 커서에 담긴 앵커(직전 페이지 마지막 행)의 정렬값을 꺼내 다음 페이지 범위를 자르는 where 조건을 만듭니다.
        BooleanExpression cursorRange = buildRangeFromCursor(sortField, asc, q.idAfter(), q.cursor());
        if (cursorRange != null) {
            where.and(cursorRange);
        }

        // --- 3) 정렬: 단일 정렬키 + id 타이브레이커
        //     - 선택된 단일 정렬필드에 대한 OrderSpecifier 생성
        //     - tie-breaker로 id 정렬을 추가해 순서 보장
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(sortField, asc);

        // --- 4) 조회: limit(size + 1)로 plus-one 패턴 (hasNext 판별용)
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
        //     - hasNext: size+1로 더 가져왔는지로 판별
        //     - rows는 size개만 남기고, "남긴 마지막 행"을 앵커로 커서를 생성
        boolean hasNext = rows.size() > size;
        Long nextIdAfter = null;
        String nextCursor = null;
        if (hasNext) {
            rows = rows.subList(0, size);
        }
        if (hasNext && !rows.isEmpty()) {
            var last = rows.get(rows.size() - 1);
            nextIdAfter = last.id();
            nextCursor = switch (sortField) {
                case INDEX_CLASSIFICATION -> CursorCodec.encodeString(last.indexClassification());
                case INDEX_NAME -> CursorCodec.encodeString(last.indexName());
                case EMPLOYED_ITEMS_COUNT -> CursorCodec.encodeNumber(last.employedItemsCount());
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
     * - base(정렬값)는 cursor에서 디코드하거나, cursor가 없으면 idAfter로 1회 조회합니다.
     * - 문자열 컬럼은 coalesce("") / 숫자 컬럼은 coalesce(Integer.MIN_VALUE)로 null을 정규화해
     *   ORDER BY의 NullsLast와 논리적으로 일관되게 비교합니다.
     */
    private BooleanExpression buildRangeFromCursor(IndexSortField sortField, boolean asc, Long idAfter, String cursor) {
        if (idAfter == null || !hasText(cursor)) {
            return null;
        }

        switch (sortField) {
            case INDEX_CLASSIFICATION -> {
                String base = CursorCodec.decodeString(cursor);

                StringExpression f = i.indexClassification.coalesce("");
                return asc
                    ? f.gt(base).or(f.eq(base).and(i.id.gt(idAfter)))
                    : f.lt(base).or(f.eq(base).and(i.id.lt(idAfter)));
            }
            case INDEX_NAME -> {
                String base = CursorCodec.decodeString(cursor);

                StringExpression f = i.indexName.coalesce("");
                return asc
                    ? f.gt(base).or(f.eq(base).and(i.id.gt(idAfter)))
                    : f.lt(base).or(f.eq(base).and(i.id.lt(idAfter)));
            }
            case EMPLOYED_ITEMS_COUNT -> {
                int base = CursorCodec.decodeNumber(cursor);

                NumberExpression<Integer> f = i.employedItemsCount.coalesce(Integer.MIN_VALUE);
                return asc
                    ? f.gt(base).or(f.eq(base).and(i.id.gt(idAfter)))
                    : f.lt(base).or(f.eq(base).and(i.id.lt(idAfter)));
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 선택된 단일 정렬필드에 대한 ORDER BY 생성.
     * - 주 정렬키에는 NullsLast를 고정하여 UI/경계 일관성을 높입니다.
     * - 타이브레이커로 항상 id를 같은 방향으로 추가하여 "결정적 순서"를 보장합니다.
     *   (키셋 비교식의 (F, id)와 순서를 반드시 맞춰야 합니다.)
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(IndexSortField sortField, boolean asc) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        Order dir = asc ? Order.ASC : Order.DESC;

        switch (sortField) {
            case INDEX_NAME ->
                orders.add(new OrderSpecifier<>(dir, i.indexName));
            case EMPLOYED_ITEMS_COUNT ->
                orders.add(new OrderSpecifier<>(dir, i.employedItemsCount, OrderSpecifier.NullHandling.NullsLast));
            default ->
                orders.add(new OrderSpecifier<>(dir, i.indexClassification));
        }

        // tie-breaker: id (같은 방향으로)
        orders.add(new OrderSpecifier<>(dir, i.id));
        return orders;
    }
}
