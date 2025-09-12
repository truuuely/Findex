package com.findex.service;

import com.findex.dto.dashboard.IndexPerformanceDto;
import com.findex.dto.dashboard.IndexPerformanceRawDto;
import com.findex.dto.dashboard.RankedIndexPerformanceDto;
import com.findex.enums.PeriodType;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexRankService {

  private final IndexInfoRepository indexInfoRepository;

  public List<RankedIndexPerformanceDto> getPerformanceRank(PeriodType periodType, int limit) {
    LocalDate currentDate = LocalDate.now();
    LocalDate beforeDate = switch (periodType) {
      case DAILY -> currentDate.minusDays(1);
      case WEEKLY -> currentDate.minusWeeks(1);
      case MONTHLY -> currentDate.minusMonths(1);
    };

    List<IndexPerformanceRawDto> rawDataList = indexInfoRepository.findAllPerformanceRawData(
        currentDate, beforeDate);

    List<IndexPerformanceDto> sortedPerformances = rawDataList.stream()
        .map(raw -> {
          BigDecimal currentPrice = Optional.ofNullable(raw.currentPrice()).orElse(BigDecimal.ZERO);
          BigDecimal beforePrice = Optional.ofNullable(raw.beforePrice()).orElse(BigDecimal.ZERO);
          BigDecimal versus = currentPrice.subtract(beforePrice);
          BigDecimal fluctuationRate = BigDecimal.ZERO;
          if (beforePrice.compareTo(BigDecimal.ZERO) != 0) {
            fluctuationRate = versus.divide(beforePrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
          }
          return new IndexPerformanceDto(
              raw.indexInfo().getId(),
              raw.indexInfo().getIndexClassification(),
              raw.indexInfo().getIndexName(),
              currentPrice,
              beforePrice,
              versus,
              fluctuationRate
          );
        })
        .filter(p -> p.fluctuationRate() != null)
        .sorted(Comparator.comparing(IndexPerformanceDto::fluctuationRate).reversed())
        .limit(limit)
        .toList();


    return IntStream.range(0, sortedPerformances.size())
        .mapToObj(i -> new RankedIndexPerformanceDto(i + 1, sortedPerformances.get(i)))
        .toList();
  }
}