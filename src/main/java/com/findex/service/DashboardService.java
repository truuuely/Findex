package com.findex.service;


import com.findex.dto.dashboard.IndexPerformanceDto;
import com.findex.entity.IndexInfo;
import com.findex.enums.PeriodType;
import com.findex.repository.IndexDataRepository;
import com.findex.repository.IndexInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final IndexInfoRepository indexInfoRepository;
  private final IndexDataRepository indexDataRepository;

  public List<IndexPerformanceDto> getFavoriteIndexPerformances(PeriodType periodType) {
    List<IndexInfo> favoriteIndices = indexInfoRepository.findAllByFavoriteIsTrue();

    LocalDate currentDate = LocalDate.now();
    LocalDate beforeDate = switch (periodType) {
      case DAILY -> currentDate.minusDays(1);
      case WEEKLY -> currentDate.minusWeeks(1);
      case MONTHLY -> currentDate.minusMonths(1);
    };

    return favoriteIndices.stream()
        .map(indexInfo -> calculatePerformance(indexInfo, currentDate, beforeDate))
        .collect(Collectors.toList());
  }

  private IndexPerformanceDto calculatePerformance(IndexInfo indexInfo, LocalDate currentDate, LocalDate beforeDate) {
    Pageable pageable = PageRequest.of(0, 1);

    List<BigDecimal> currentPriceList = indexDataRepository.findClosingPrice(indexInfo.getId(), currentDate, pageable);
    List<BigDecimal> beforePriceList = indexDataRepository.findClosingPrice(indexInfo.getId(), beforeDate, pageable);

    BigDecimal currentPrice = currentPriceList.isEmpty() ? BigDecimal.ZERO : currentPriceList.get(0);
    BigDecimal beforePrice = beforePriceList.isEmpty() ? BigDecimal.ZERO : beforePriceList.get(0);

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
  }
}