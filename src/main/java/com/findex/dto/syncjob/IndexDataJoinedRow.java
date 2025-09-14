package com.findex.dto.syncjob;

import com.findex.enums.IndexSourceType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IndexDataJoinedRow(
    Long indexInfoId,
    String indexClassification,
    String indexName,
    Long dataId,
    LocalDate baseDate,
    IndexSourceType sourceType,
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