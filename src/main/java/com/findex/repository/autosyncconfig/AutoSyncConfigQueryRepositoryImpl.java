package com.findex.repository.autosyncconfig;

import static com.findex.entity.QAutoSyncConfig.autoSyncConfig;
import static org.springframework.util.StringUtils.hasText;

import com.findex.dto.autosyncconfig.AutoSyncConfigDto;
import com.findex.dto.autosyncconfig.AutoSyncConfigQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.enums.AutoSyncConfigSortField;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
                autoSyncConfig.id,
                autoSyncConfig.indexInfo.id,
                autoSyncConfig.indexInfo.indexClassification,
                autoSyncConfig.indexInfo.indexName,
                autoSyncConfig.enabled
            ))
            .from(autoSyncConfig)
            .where(
                configQuery.indexInfoId() != null ? autoSyncConfig.indexInfo.id.eq(configQuery.indexInfoId()) : null,
                configQuery.enabled() != null ? autoSyncConfig.enabled.eq(configQuery.enabled()) : null,
                buildRangeFromCursor(configQuery.sortFieldEnum(), configQuery.asc(), configQuery.idAfter(), configQuery.cursor())
            )
            .orderBy(buildOrderSpecifiers(configQuery.sortFieldEnum(), configQuery.asc()))
            .limit(configQuery.size() + 1)
            .fetch();

        Long totalElements = queryFactory
            .select(autoSyncConfig.count())
            .from(autoSyncConfig)
            .where(configQuery.indexInfoId() != null ? autoSyncConfig.indexInfo.id.eq(configQuery.indexInfoId()) : null,
                    configQuery.enabled() != null ? autoSyncConfig.enabled.eq(configQuery.enabled()) : null)
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
        String nextCursor;
        if (configQuery.sortFieldEnum() == AutoSyncConfigSortField.ENABLED) {
            nextCursor = String.valueOf(last.enabled());
        } else {
            nextCursor = last.indexName();
        }

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
                StringExpression f = autoSyncConfig.indexInfo.indexName.coalesce("");
                yield asc
                    ? f.gt(cursor).or(f.eq(cursor).and(autoSyncConfig.id.gt(idAfter)))
                    : f.lt(cursor).or(f.eq(cursor).and(autoSyncConfig.id.lt(idAfter)));
            }
            case ENABLED -> {
                Boolean cursorBool = Boolean.parseBoolean(cursor);
                yield asc
                    ? autoSyncConfig.enabled.gt(cursorBool).or(autoSyncConfig.enabled.eq(cursorBool).and(autoSyncConfig.id.gt(idAfter)))
                    : autoSyncConfig.enabled.lt(cursorBool).or(autoSyncConfig.enabled.eq(cursorBool).and(autoSyncConfig.id.lt(idAfter)));
            }
        };
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(AutoSyncConfigSortField sortField, boolean asc) {
        Order sortDirection = asc ? Order.ASC : Order.DESC;

        if (sortField == AutoSyncConfigSortField.ENABLED) {
            return new OrderSpecifier<?>[] {
                new OrderSpecifier<>(sortDirection, autoSyncConfig.enabled),
                new OrderSpecifier<>(Order.ASC, autoSyncConfig.id)
            };
        }
        return new OrderSpecifier[] {
            new OrderSpecifier<>(sortDirection, autoSyncConfig.indexInfo.indexName),
            new OrderSpecifier<>(Order.ASC, autoSyncConfig.id)
        };
    }
}
