package com.findex.dto.dashboard;

import com.findex.entity.IndexInfo;
import java.math.BigDecimal;

public record IndexPerformanceRawDto(
    IndexInfo indexInfo,
    BigDecimal currentPrice,
    BigDecimal beforePrice
) {

}
