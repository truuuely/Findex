package com.findex.service;

import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.syncjob.IndexDataOpenApiResult;
import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.entity.IndexData;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.openapi.MarketIndexClient;
import com.findex.repository.indexdata.IndexDataRepository;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class IndexDataSyncService {

  private final MarketIndexClient client;
  private final IndexInfoRepository infoRepo;
  private final IndexDataRepository dataRepo;

  @Transactional
  public List<IndexDataOpenApiResult> syncFromOpenApi(IndexDataOpenApiSyncRequest req) {
    LocalDate from = req.baseDateFrom();
    LocalDate to   = req.baseDateTo();
    if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }

    var ids = new java.util.LinkedHashSet<>(req.indexInfoIds());
    var results = new ArrayList<IndexDataOpenApiResult>();
    for (Long id : ids) {
      results.add(syncOneIndex(id, from, to));
    }
    return results;
  }

  // 단일 인덱스 처리 (기존 로직 그대로 이동)
  private IndexDataOpenApiResult syncOneIndex(Long indexInfoId, LocalDate from, LocalDate to) {
    IndexInfo info = infoRepo.getOrThrow(indexInfoId);

    final int PAGE_SIZE = 500;
    List<MarketIndexClient.OpenApiIndexDataItem> items;

    // idxNm + idxCsf
    items = fetchAllPages(info.getIndexName(), info.getIndexClassification(), from, to, PAGE_SIZE);

    // idxNm만
    if (items.isEmpty()) {
      items = fetchAllPages(info.getIndexName(), null, from, to, PAGE_SIZE);
      log.warn("[index-data] fallback#1 only idxNm. indexInfoIds={}, collected={}", indexInfoId, items.size());
    }

    // 전체 받아 로컬 필터
    if (items.isEmpty()) {
      List<MarketIndexClient.OpenApiIndexDataItem> all = fetchAllPages(null, null, from, to, PAGE_SIZE);
      String want = norm(info.getIndexName());
      items = all.stream().filter(it -> want.equals(norm(it.indexName()))).toList();
      log.warn("[index-data] fallback#2 local filter. indexInfoIds={}, collected={}", indexInfoId, items.size());
    }

    // 업서트
    int saved = 0;
    for (var it : items) {
      if (it.baseDate() == null) continue;
      IndexData e = dataRepo.findByIndexInfoIdAndBaseDate(indexInfoId, it.baseDate())
          .orElseGet(() -> new IndexData(indexInfoId, it.baseDate(), IndexSourceType.OPEN_API));
      e.updatePrices(it.marketPrice(), it.closingPrice(), it.highPrice(), it.lowPrice());
      e.updateFluctuation(it.versus(), it.fluctuationRate());
      e.updateTrading(it.tradingQuantity(), it.tradingPrice(), it.marketTotalAmount());
      dataRepo.save(e);
      saved++;
    }
    log.info("[index-data] indexInfoIds={} upsert saved={}", indexInfoId, saved);

    // 조회/반환 (단일 id 결과 한 건)
    var list = fetchByIndexId(indexInfoId, from, to);
    if (!list.isEmpty()) return list.get(0);

    // 저장 0건이어도 응답 스키마 유지
    return new IndexDataOpenApiResult(
        info.getIndexName(),
        java.util.List.of(new IndexDataOpenApiResult.Group(indexInfoId, info.getIndexClassification(), java.util.List.of()))
    );
  }

  @Transactional(readOnly = true)
  public List<IndexDataOpenApiResult> fetchByIndexId(Long indexInfoId, LocalDate from, LocalDate to) {
    var rows = dataRepo.findJoinedSortedByIndexId(indexInfoId, from, to);

    Map<Long, List<IndexDataDto>> byInfoId = new LinkedHashMap<>();
    for (var r : rows) {
      byInfoId.computeIfAbsent(r.indexInfoId(), k -> new ArrayList<>())
          .add(new IndexDataDto(
              null, r.indexInfoId(), r.baseDate(), r.sourceType(),
              r.marketPrice(), r.closingPrice(), r.highPrice(), r.lowPrice(),
              r.versus(), r.fluctuationRate(), r.tradingQuantity(), r.tradingPrice(), r.marketTotalAmount()
          ));
    }

    IndexInfo info = infoRepo.getOrThrow(indexInfoId);
    List<IndexDataOpenApiResult.Group> groups = new ArrayList<>();
    byInfoId.forEach((id, list) ->
        groups.add(new IndexDataOpenApiResult.Group(id, info.getIndexClassification(), list))
    );

    return List.of(new IndexDataOpenApiResult(info.getIndexName(), groups));
  }

  // 페이징 수집
  private List<MarketIndexClient.OpenApiIndexDataItem> fetchAllPages(
      String idxNm, String idxCsf, LocalDate from, LocalDate to, int pageSize
  ) {
    List<MarketIndexClient.OpenApiIndexDataItem> out = new ArrayList<>();
    int page = 1;
    while (true) {
      var pr = client.callGetStockMarketIndexData(idxNm, idxCsf, from, to, page, pageSize);
      if (pr.items() != null && !pr.items().isEmpty()) out.addAll(pr.items());
      if (pr.items() == null || pr.items().isEmpty() || page * pr.numOfRows() >= pr.totalCount()) break;
      page++;
    }
    return out;
  }

  private static String norm(String s) {
    if (s == null) return "";
    return s.trim().replaceAll("\\s+", " ");
  }
}