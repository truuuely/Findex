package com.findex.service;


import com.findex.dto.dashboard.ChartDataPointDto;
import com.findex.dto.dashboard.IndexChartDto;
import com.findex.dto.dashboard.IndexPerformanceDto;
import com.findex.dto.dashboard.IndexPerformanceRawDto;
import com.findex.dto.dashboard.RankedIndexPerformanceDto;
import com.findex.entity.IndexData;
import com.findex.entity.IndexInfo;
import com.findex.enums.ChartPeriodType;
import com.findex.enums.PeriodType;
import com.findex.exception.NotFoundException;
import com.findex.repository.indexdata.IndexDataRepository;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final IndexInfoRepository indexInfoRepository;
    private final IndexDataRepository indexDataRepository;

    private static final int PERFORMANCE_CALCULATION_SCALE = 4;
    private static final int CHART_CALCULATION_SCALE = 2;
    private static final int MA_5_DAYS = 5;
    private static final int MA_20_DAYS = 20;

    public List<IndexPerformanceDto> getFavoriteIndexPerformances(PeriodType periodType) {

        LocalDate currentDate = LocalDate.now();
        LocalDate beforeDate = switch (periodType) {
            case DAILY -> currentDate.minusDays(1);
            case WEEKLY -> currentDate.minusWeeks(1);
            case MONTHLY -> currentDate.minusMonths(1);
        };

        List<IndexPerformanceRawDto> rawDataList = indexInfoRepository.findPerformanceRawData(
            currentDate, beforeDate);

        return rawDataList.stream()
            .map(raw -> {
                IndexInfo indexInfo = raw.indexInfo();
                BigDecimal currentPrice = Optional.ofNullable(raw.currentPrice()).orElse(BigDecimal.ZERO);
                BigDecimal beforePrice = Optional.ofNullable(raw.beforePrice()).orElse(BigDecimal.ZERO);

                BigDecimal versus = currentPrice.subtract(beforePrice);
                BigDecimal fluctuationRate = BigDecimal.ZERO;
                if (beforePrice.compareTo(BigDecimal.ZERO) != 0) {
                    fluctuationRate = versus.divide(beforePrice, PERFORMANCE_CALCULATION_SCALE, RoundingMode.HALF_UP)
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


    public IndexChartDto getChartData(Long indexInfoId, ChartPeriodType periodType) {

        IndexInfo indexInfo = indexInfoRepository.findById(indexInfoId)
            .orElseThrow(() -> new NotFoundException("Index not found"));

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

            if (i >= MA_5_DAYS - 1) {
                BigDecimal ma5 = calculateMovingAverage(historicalData, i, MA_5_DAYS);
                ma5DataPoints.add(new ChartDataPointDto(currentData.getBaseDate(), ma5));
            }
            if (i >= MA_20_DAYS - 1) {
                BigDecimal ma20 = calculateMovingAverage(historicalData, i, MA_20_DAYS);
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
        return sum.divide(new BigDecimal(days), CHART_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

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