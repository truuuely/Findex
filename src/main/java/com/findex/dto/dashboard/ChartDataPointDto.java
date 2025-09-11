package com.findex.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChartDataPointDto(
    LocalDate date,
    BigDecimal value
) {}
