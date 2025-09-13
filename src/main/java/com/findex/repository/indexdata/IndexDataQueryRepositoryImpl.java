package com.findex.repository.indexdata;

import static com.findex.entity.QIndexData.indexData;

import com.findex.dto.indexdata.IndexDataQuery;
import com.findex.dto.response.CursorPageResponse;
import com.findex.entity.IndexData;
import com.findex.enums.IndexDataSortField;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IndexDataQueryRepositoryImpl implements IndexDataQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse findAll(IndexDataQuery indexDataQuery) {
        IndexDataSortField sortField = indexDataQuery.sortFieldEnum();

        BooleanExpression indexInfoIdEquals = indexDataQuery.indexInfoId() != null ? indexData.indexInfoId.eq(indexDataQuery.indexInfoId()) : null;

        List<IndexData> results = queryFactory.selectFrom(indexData)
                .where(indexInfoIdEquals,
                        indexData.baseDate.goe(indexDataQuery.startDate()).and(indexData.baseDate.loe(indexDataQuery.endDate())),
                        buildCursorPredicate(sortField, indexDataQuery.asc(), indexDataQuery.cursor(), indexDataQuery.idAfter()))
                .orderBy(createOrderSpecifiers(sortField, indexDataQuery.asc()))
                .limit(indexDataQuery.size() + 1)
                .fetch();

        Long totalCount = queryFactory.select(indexData.count())
                .from(indexData)
                .where(indexInfoIdEquals,
                        indexData.baseDate.goe(indexDataQuery.startDate()).and(indexData.baseDate.loe(indexDataQuery.endDate())))
                .fetchOne();

        long totalElements = totalCount != null ? totalCount : 0;

        if (results.size() <= indexDataQuery.size()) {
            return new CursorPageResponse(results, null, null, results.size(), totalElements, false);
        }

        results = results.subList(0, indexDataQuery.size());
        IndexData lastRow = results.get(results.size() - 1);

        return new CursorPageResponse(
                results,
                getNextCursor(lastRow, sortField),
                lastRow.getId(),
                results.size(),
                totalElements,
                true
        );
    }

    @Override
    public Stream<IndexData> findAllForExport(IndexDataQuery indexDataQuery) {
        IndexDataSortField sortField = indexDataQuery.sortFieldEnum();

        BooleanExpression indexInfoIdEquals = indexDataQuery.indexInfoId() != null ? indexData.indexInfoId.eq(indexDataQuery.indexInfoId()) : null;

        return queryFactory.selectFrom(indexData)
                .where(indexInfoIdEquals,
                        indexData.baseDate.goe(indexDataQuery.startDate()).and(indexData.baseDate.loe(indexDataQuery.endDate())))
                .orderBy(createOrderSpecifiers(sortField, indexDataQuery.asc()))
                .stream();
    }

    private OrderSpecifier<?>[] createOrderSpecifiers(IndexDataSortField sortField, boolean asc) {
        Order direction = asc ? Order.ASC : Order.DESC;

        OrderSpecifier<?> primary = switch (sortField) {
            case BASE_DATE -> new OrderSpecifier<>(direction, indexData.baseDate);
            case MARKET_PRICE -> new OrderSpecifier<>(direction, indexData.marketPrice);
            case CLOSING_PRICE -> new OrderSpecifier<>(direction, indexData.closingPrice);
            case HIGH_PRICE -> new OrderSpecifier<>(direction, indexData.highPrice);
            case LOW_PRICE -> new OrderSpecifier<>(direction, indexData.lowPrice);
            case VERSUS -> new OrderSpecifier<>(direction, indexData.versus);
            case FLUCTUATION_RATE -> new OrderSpecifier<>(direction, indexData.fluctuationRate);
            case TRADING_QUANTITY -> new OrderSpecifier<>(direction, indexData.tradingQuantity);
            case TRADING_PRICE -> new OrderSpecifier<>(direction, indexData.tradingPrice);
            case MARKET_TOTAL_AMOUNT -> new OrderSpecifier<>(direction, indexData.marketTotalAmount);
        };

        OrderSpecifier<Long> secondary = new OrderSpecifier<>(Order.ASC, indexData.id);
        return new OrderSpecifier[]{primary, secondary};
    }

    private BooleanBuilder buildCursorPredicate(IndexDataSortField sortField, boolean asc, String cursor, Long idAfter) {
        BooleanBuilder builder = new BooleanBuilder();
        if (cursor == null || idAfter == null) {
            return builder;
        }

        switch (sortField) {
            case BASE_DATE -> {
                if (asc) {
                    builder.and(indexData.baseDate.gt(LocalDate.parse(cursor)))
                            .or(indexData.baseDate.eq(LocalDate.parse(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.baseDate.lt(LocalDate.parse(cursor)))
                            .or(indexData.baseDate.eq(LocalDate.parse(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case MARKET_PRICE -> {
                if (asc) {
                    builder.and(indexData.marketPrice.gt(new BigDecimal(cursor)))
                            .or(indexData.marketPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.marketPrice.lt(new BigDecimal(cursor)))
                            .or(indexData.marketPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case CLOSING_PRICE -> {
                if (asc) {
                    builder.and(indexData.closingPrice.gt(new BigDecimal(cursor)))
                            .or(indexData.closingPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.closingPrice.lt(new BigDecimal(cursor)))
                            .or(indexData.closingPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case HIGH_PRICE -> {
                if (asc) {
                    builder.and(indexData.highPrice.gt(new BigDecimal(cursor)))
                            .or(indexData.highPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.highPrice.lt(new BigDecimal(cursor)))
                            .or(indexData.highPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case LOW_PRICE -> {
                if (asc) {
                    builder.and(indexData.lowPrice.gt(new BigDecimal(cursor)))
                            .or(indexData.lowPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.lowPrice.lt(new BigDecimal(cursor)))
                            .or(indexData.lowPrice.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case VERSUS -> {
                if (asc) {
                    builder.and(indexData.versus.gt(new BigDecimal(cursor)))
                            .or(indexData.versus.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.versus.lt(new BigDecimal(cursor)))
                            .or(indexData.versus.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case FLUCTUATION_RATE -> {
                if (asc) {
                    builder.and(indexData.fluctuationRate.gt(new BigDecimal(cursor)))
                            .or(indexData.fluctuationRate.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.fluctuationRate.lt(new BigDecimal(cursor)))
                            .or(indexData.fluctuationRate.eq(new BigDecimal(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case TRADING_QUANTITY -> {
                if (asc) {
                    builder.and(indexData.tradingQuantity.gt(Long.parseLong(cursor)))
                            .or(indexData.tradingQuantity.eq(Long.parseLong(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.tradingQuantity.lt(Long.parseLong(cursor)))
                            .or(indexData.tradingQuantity.eq(Long.parseLong(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case TRADING_PRICE -> {
                if (asc) {
                    builder.and(indexData.tradingPrice.gt(Long.parseLong(cursor)))
                            .or(indexData.tradingPrice.eq(Long.parseLong(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.tradingPrice.lt(Long.parseLong(cursor)))
                            .or(indexData.tradingPrice.eq(Long.parseLong(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
            case MARKET_TOTAL_AMOUNT -> {
                if (asc) {
                    builder.and(indexData.marketTotalAmount.gt(Long.parseLong(cursor)))
                            .or(indexData.marketTotalAmount.eq(Long.parseLong(cursor)).and(indexData.id.gt(idAfter)));
                } else {
                    builder.and(indexData.marketTotalAmount.lt(Long.parseLong(cursor)))
                            .or(indexData.marketTotalAmount.eq(Long.parseLong(cursor)).and(indexData.id.gt(idAfter)));
                }
            }
        }

        return builder;
    }

    private String getNextCursor(IndexData lastRow, IndexDataSortField sortField) {
        return switch (sortField) {
            case BASE_DATE -> lastRow.getBaseDate().toString();
            case MARKET_PRICE -> lastRow.getMarketPrice().toString();
            case CLOSING_PRICE -> lastRow.getClosingPrice().toString();
            case HIGH_PRICE -> lastRow.getHighPrice().toString();
            case LOW_PRICE -> lastRow.getLowPrice().toString();
            case VERSUS -> lastRow.getVersus().toString();
            case FLUCTUATION_RATE -> lastRow.getFluctuationRate().toString();
            case TRADING_QUANTITY -> String.valueOf(lastRow.getTradingQuantity());
            case TRADING_PRICE -> String.valueOf(lastRow.getTradingPrice());
            case MARKET_TOTAL_AMOUNT -> String.valueOf(lastRow.getMarketTotalAmount());
        };
    }
}
