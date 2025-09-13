package com.findex.repository.autoSyncRepository;

import static com.findex.enums.AutoSyncConfigSortField.parse;
import static org.springframework.util.StringUtils.hasText;

import com.findex.dto.autoSyncConfig.AutoSyncConfigDto;
import com.findex.dto.autoSyncConfig.AutoSyncConfigQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.entity.QAutoSyncConfig;
import com.findex.enums.AutoSyncConfigSortField;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AutoSyncConfigQueryRepositoryImpl implements AutoSyncConfigQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse findAll(AutoSyncConfigQuery configQuery) {
        List<AutoSyncConfigDto> rows = queryFactory
                .select(Projections.constructor(AutoSyncConfigDto.class,
                        QAutoSyncConfig.autoSyncConfig.id,
                        QAutoSyncConfig.autoSyncConfig.indexInfo.id,
                        QAutoSyncConfig.autoSyncConfig.indexInfo.indexClassification,
                        QAutoSyncConfig.autoSyncConfig.indexInfo.indexName,
                        QAutoSyncConfig.autoSyncConfig.enabled
                ))
                .from(QAutoSyncConfig.autoSyncConfig)
                .where(
                        configQuery.indexInfoId() != null ? QAutoSyncConfig.autoSyncConfig.indexInfo.id.eq(configQuery.indexInfoId()) : null,
                        configQuery.enabled() != null ? QAutoSyncConfig.autoSyncConfig.enabled.eq(configQuery.enabled()) : null,
                        buildRangeFromCursor(parse(configQuery.sortField()), configQuery.asc(configQuery.sortDirection()), configQuery.idAfter(), configQuery.cursor())
                )
                .orderBy(buildOrderSpecifiers(parse(configQuery.sortField()), configQuery.asc(configQuery.sortDirection())).toArray(OrderSpecifier[]::new))
                .limit(configQuery.size() + 1)
                .fetch();

        Long totalElements = queryFactory
                .select(QAutoSyncConfig.autoSyncConfig.count())
                .from(QAutoSyncConfig.autoSyncConfig)
                .where(configQuery.indexInfoId() != null ? QAutoSyncConfig.autoSyncConfig.indexInfo.id.eq(configQuery.indexInfoId()) : null,
                        configQuery.enabled() != null ? QAutoSyncConfig.autoSyncConfig.enabled.eq(configQuery.enabled()) : null)
                .fetchOne();
        long total = totalElements != null ? totalElements : 0;

        boolean hasNext = rows.size() > configQuery.size();
        if (!hasNext) {
            return new CursorPageResponse(
                    rows,
                    null,
                    null,
                    configQuery.size(),
                    total,
                    false
            );
        }

        rows = rows.subList(0, configQuery.size());

        AutoSyncConfigDto last = rows.get(rows.size() - 1);
        Long nextIdAfter = last.id();
        String nextCursor = switch (parse(configQuery.sortField())) {
            case INDEX_INFO_INDEX_NAME -> last.indexName();
            case ENABLED -> String.valueOf(last.enabled());
        };


        return new CursorPageResponse(
                rows,
                nextCursor,
                nextIdAfter,
                configQuery.size(),
                total,
                true
        );
    }


    private BooleanExpression buildRangeFromCursor(
            AutoSyncConfigSortField sortField,
            boolean asc,
            Long idAfter,
            String cursor
    ) {
        if (idAfter == null || !hasText(cursor)) {
            return null;
        }

        return switch (sortField) {
            case INDEX_INFO_INDEX_NAME -> {
                StringExpression f = QAutoSyncConfig.autoSyncConfig.indexInfo.indexName.coalesce("");
                yield asc
                        ? f.gt(cursor).or(f.eq(cursor).and(QAutoSyncConfig.autoSyncConfig.id.gt(idAfter)))
                        : f.lt(cursor).or(f.eq(cursor).and(QAutoSyncConfig.autoSyncConfig.id.lt(idAfter)));
            }
            case ENABLED -> {
                Boolean cursorBool = Boolean.parseBoolean(cursor);
                yield asc
                        ? QAutoSyncConfig.autoSyncConfig.enabled.gt(cursorBool).or(QAutoSyncConfig.autoSyncConfig.enabled.eq(cursorBool).and(QAutoSyncConfig.autoSyncConfig.id.gt(idAfter)))
                        : QAutoSyncConfig.autoSyncConfig.enabled.lt(cursorBool).or(QAutoSyncConfig.autoSyncConfig.enabled.eq(cursorBool).and(QAutoSyncConfig.autoSyncConfig.id.lt(idAfter)));
            }
        };
    }


    private List<OrderSpecifier<?>> buildOrderSpecifiers(AutoSyncConfigSortField sortField, boolean asc) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        Order dir = asc ? Order.ASC : Order.DESC;

        switch (sortField) {
            case INDEX_INFO_INDEX_NAME ->
                    orders.add(new OrderSpecifier<>(dir, QAutoSyncConfig.autoSyncConfig.indexInfo.indexName));
            case ENABLED -> orders.add(new OrderSpecifier<>(dir, QAutoSyncConfig.autoSyncConfig.enabled));
        }
        orders.add(new OrderSpecifier<>(dir, QAutoSyncConfig.autoSyncConfig.id));

        return orders;
    }
}
