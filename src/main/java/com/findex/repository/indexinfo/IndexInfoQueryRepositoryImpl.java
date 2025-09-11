package com.findex.repository.indexinfo;

import static com.findex.entity.QIndexInfo.indexInfo;
import static org.springframework.util.StringUtils.hasText;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.indexinfo.IndexInfoQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.enums.IndexSortField;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class IndexInfoQueryRepositoryImpl implements IndexInfoQueryRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse findAll(IndexInfoQuery q) {
        List<IndexInfoDto> rows = queryFactory
            .select(Projections.constructor(IndexInfoDto.class,
                indexInfo.id,
                indexInfo.indexClassification,
                indexInfo.indexName,
                indexInfo.employedItemsCount,
                indexInfo.basePointInTime,
                indexInfo.baseIndex,
                indexInfo.sourceType,
                indexInfo.favorite
            ))
            .from(indexInfo)
            .where(
                hasText(q.indexClassification()) ? indexInfo.indexClassification.contains(q.indexClassification()) : null,
                hasText(q.indexName()) ? indexInfo.indexName.contains(q.indexName()) : null,
                q.favorite() != null ? indexInfo.favorite.eq(q.favorite()) : null,
                buildRangeFromCursor(q.sortFieldEnum(), q.asc(), q.idAfter(), q.cursor())
            )
            .orderBy(buildOrderSpecifiers(q.sortFieldEnum(), q.asc()))
            .limit(q.size() + 1)
            .fetch();

        Long total = queryFactory
            .select(indexInfo.count())
            .from(indexInfo)
            .where(
                hasText(q.indexClassification()) ? indexInfo.indexClassification.contains(q.indexClassification()) : null,
                hasText(q.indexName()) ? indexInfo.indexName.contains(q.indexName()) : null,
                q.favorite() != null ? indexInfo.favorite.eq(q.favorite()) : null
            )
            .fetchOne();

        long totalElements = (total != null) ? total : 0;

        if (rows.size() <= q.size()) {
            return new CursorPageResponse(rows, null, null, q.size(), totalElements, false);
        }

        rows = rows.subList(0, q.size());
        IndexInfoDto last = rows.get(rows.size() - 1);

        return new CursorPageResponse(
            rows,
            switch (q.sortFieldEnum()) {
                case INDEX_NAME -> last.indexName();
                case EMPLOYED_ITEMS_COUNT -> String.valueOf(last.employedItemsCount());
                default -> last.indexClassification();
            },
            last.id(),
            q.size(),
            totalElements,
            true
        );
    }

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
                StringExpression f = indexInfo.indexName.coalesce("");
                return asc
                    ? f.gt(cursor).or(f.eq(cursor).and(indexInfo.id.gt(idAfter)))
                    : f.lt(cursor).or(f.eq(cursor).and(indexInfo.id.lt(idAfter)));
            }
            case EMPLOYED_ITEMS_COUNT -> {
                int cursorInt;
                try {
                    cursorInt = Integer.parseInt(cursor);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid cursor: " + cursor);
                }
                NumberExpression<Integer> f = indexInfo.employedItemsCount.coalesce(Integer.MIN_VALUE);
                return asc
                    ? f.gt(cursorInt).or(f.eq(cursorInt).and(indexInfo.id.gt(idAfter)))
                    : f.lt(cursorInt).or(f.eq(cursorInt).and(indexInfo.id.lt(idAfter)));
            }
            default -> {
                StringExpression f = indexInfo.indexClassification.coalesce("");
                return asc
                    ? f.gt(cursor).or(f.eq(cursor).and(indexInfo.id.gt(idAfter)))
                    : f.lt(cursor).or(f.eq(cursor).and(indexInfo.id.lt(idAfter)));
            }
        }
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(IndexSortField sortField, boolean asc) {
        Order dir = asc ? Order.ASC : Order.DESC;

        OrderSpecifier<?> primary = switch (sortField) {
            case INDEX_NAME -> new OrderSpecifier<>(dir, indexInfo.indexName);
            case EMPLOYED_ITEMS_COUNT -> new OrderSpecifier<>(dir, indexInfo.employedItemsCount);
            default -> new OrderSpecifier<>(dir, indexInfo.indexClassification);
        };

        return new OrderSpecifier<?>[] {
            primary,
            new OrderSpecifier<>(Order.ASC, indexInfo.id)
        };
    }
}
