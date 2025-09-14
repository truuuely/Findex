package com.findex.controller;

import com.findex.dto.dashboard.IndexChartDto;
import com.findex.enums.ChartPeriodType;
import com.findex.service.ChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/index-data")
public class ChartController {

  private final ChartService chartService;

  @GetMapping("/{id}/chart")
  public ResponseEntity<IndexChartDto> getChartData(
      @PathVariable("id") Long id,
      @RequestParam(defaultValue = "MONTHLY") ChartPeriodType periodType
  ) {
    IndexChartDto result = chartService.getChartData(id, periodType);
    return ResponseEntity.ok(result);
  }
}
