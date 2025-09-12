package com.findex.service;

import com.findex.dto.dashboard.ChartDataPointDto;
import com.findex.dto.dashboard.IndexChartDto;
import com.findex.entity.IndexData;
import com.findex.entity.IndexInfo;
import com.findex.enums.ChartPeriodType;
import com.findex.repository.indexdata.IndexDataRepository;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartService {

  private final IndexInfoRepository indexInfoRepository;
  private final IndexDataRepository indexDataRepository;

  public IndexChartDto getChartData(Long indexInfoId, ChartPeriodType periodType) {

    IndexInfo indexInfo = indexInfoRepository.findById(indexInfoId)
        .orElseThrow(() -> new IllegalArgumentException("Index not found"));

    LocalDate startDate = LocalDate.now();
    switch (periodType) {
      case MONTHLY -> startDate = startDate.minusMonths(1);
      case QUARTERLY -> startDate = startDate.minusMonths(3);
      case YEARLY -> startDate = startDate.minusYears(1);
    }

    List<IndexData> historicalData = indexDataRepository.findChartData(indexInfoId, startDate);

    List<ChartDataPointDto> dataPoints = new ArrayList<>();
    List<ChartDataPointDto> ma5DataPoints = new ArrayList<>();
    List<ChartDataPointDto> ma20DataPoints = new ArrayList<>();

    for (int i = 0; i < historicalData.size(); i++) {
      IndexData currentData = historicalData.get(i);

      dataPoints.add(
          new ChartDataPointDto(currentData.getBaseDate(), currentData.getClosingPrice()));

      if (i >= 4) {
        BigDecimal ma5 = calculateMovingAverage(historicalData, i, 5);
        ma5DataPoints.add(new ChartDataPointDto(currentData.getBaseDate(), ma5));
      }
      if (i >= 19) {
        BigDecimal ma20 = calculateMovingAverage(historicalData, i, 20);
        ma20DataPoints.add(new ChartDataPointDto(currentData.getBaseDate(), ma20));
      }
    }

    return new IndexChartDto(
        indexInfo.getId(),
        indexInfo.getIndexClassification(),
        indexInfo.getIndexName(),
        periodType,
        dataPoints,
        ma5DataPoints,
        ma20DataPoints
    );
  }

  private BigDecimal calculateMovingAverage(List<IndexData> data, int currentIndex, int days) {
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < days; i++) {
      sum = sum.add(data.get(currentIndex - i).getClosingPrice());
    }
    return sum.divide(new BigDecimal(days), 2, RoundingMode.HALF_UP);
  }
}


