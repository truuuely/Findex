package com.findex.dto.syncjob;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenApiIndexDataItem(
    String indexClassification,
    String indexName,
    LocalDate baseDate,
    BigDecimal marketPrice,
    BigDecimal closingPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal versus,
    BigDecimal fluctuationRate,
    Long tradingQuantity,
    Long tradingPrice,
    Long marketTotalAmount
) {}
