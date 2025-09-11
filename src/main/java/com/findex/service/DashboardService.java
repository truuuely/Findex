package com.findex.service;


import com.findex.dto.dashboard.IndexPerformanceDto;
import com.findex.dto.dashboard.IndexPerformanceRawDto;
import com.findex.entity.IndexInfo;
import com.findex.enums.PeriodType;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final IndexInfoRepository indexInfoRepository;

  public List<IndexPerformanceDto> getFavoriteIndexPerformances(PeriodType periodType) {

    LocalDate currentDate = LocalDate.now();
    LocalDate beforeDate = switch (periodType) {
      case DAILY -> currentDate.minusDays(1);
      case WEEKLY -> currentDate.minusWeeks(1);
      case MONTHLY -> currentDate.minusMonths(1);
    };

    List<IndexPerformanceRawDto> rawDataList = indexInfoRepository.findPerformanceRawData(currentDate, beforeDate);

    return rawDataList.stream()
        .map(raw -> {
          IndexInfo indexInfo = raw.indexInfo();
          BigDecimal currentPrice = Optional.ofNullable(raw.currentPrice()).orElse(BigDecimal.ZERO);
          BigDecimal beforePrice = Optional.ofNullable(raw.beforePrice()).orElse(BigDecimal.ZERO);

          BigDecimal versus = currentPrice.subtract(beforePrice);
          BigDecimal fluctuationRate = BigDecimal.ZERO;
          if (beforePrice.compareTo(BigDecimal.ZERO) != 0) {
            fluctuationRate = versus.divide(beforePrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
          }

          return new IndexPerformanceDto(
              indexInfo.getId(),
              indexInfo.getIndexClassification(),
              indexInfo.getIndexName(),
              currentPrice,
              beforePrice,
              versus,
              fluctuationRate
          );
        })
        .collect(Collectors.toList());
  }
}