package com.findex.controller;

import com.findex.dto.dashboard.RankedIndexPerformanceDto;
import com.findex.enums.PeriodType;
import com.findex.service.IndexRankService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/index-data")
public class IndexRankController {

  private final IndexRankService indexRankService;

  @GetMapping("/performance/rank")
  public ResponseEntity<List<RankedIndexPerformanceDto>> getPerformanceRank(
      @RequestParam(defaultValue = "DAILY") PeriodType periodType,
      @RequestParam(defaultValue = "10") int limit
  ) {
    List<RankedIndexPerformanceDto> result = indexRankService.getPerformanceRank(periodType, limit);
    return ResponseEntity.ok(result);
  }
}