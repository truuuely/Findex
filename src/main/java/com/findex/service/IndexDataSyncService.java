package com.findex.service;

import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.syncjob.IndexDataJoinedRow;
import com.findex.dto.syncjob.IndexDataOpenApiResult;
import com.findex.dto.syncjob.IndexDataOpenApiResult.Group;
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
    List<IndexInfo> targets = (req.indexName()==null || req.indexName().isBlank())
        ? infoRepo.findAll()
        : infoRepo.findByIndexName(req.indexName().trim());
    if (targets.isEmpty()) {
      log.warn("[index-data] no targets for indexName='{}' — will try fallback later", req.indexName());
    }

    final int PAGE_SIZE = 1000;
    int savedCnt = 0;

    for (IndexInfo info : targets) {
      var page = client.callGetStockMarketIndexData(info.getIndexName(), req.from(), req.to(), 1, PAGE_SIZE);
      List<MarketIndexClient.OpenApiIndexDataItem> items =
          page.items() == null ? List.of() : page.items();

      // ✅ fallback: idxNm 없이 전체 받아, OpenAPI 아이템의 idxNm로 필터
      if (items.isEmpty()) {
        var all = client.callGetStockMarketIndexData(null, req.from(), req.to(), 1, PAGE_SIZE);
        String want = norm(info.getIndexName());
        items = (all.items()==null?List.<MarketIndexClient.OpenApiIndexDataItem>of():all.items())
            .stream()
            .filter(it -> want.equals(norm(it.indexName()))) // ✅ 여기!
            .toList();
      }

      for (var it : items) {
        if (it.baseDate() == null) continue;
        IndexData e = dataRepo.findByIndexInfoIdAndBaseDate(info.getId(), it.baseDate())
            .orElseGet(() -> new IndexData(info.getId(), it.baseDate(), IndexSourceType.OPEN_API));
        e.updatePrices(it.marketPrice(), it.closingPrice(), it.highPrice(), it.lowPrice());
        e.updateFluctuation(it.versus(), it.fluctuationRate());
        e.updateTrading(it.tradingQuantity(), it.tradingPrice(), it.marketTotalAmount());
        dataRepo.save(e);
        savedCnt++;
      }
    }

    log.info("[index-data] upsert saved count = {}", savedCnt);

    return fetchJoinedSorted(req.from(), req.to(), req.indexName()); // ✅ 이제 인식됨
  }

  @Transactional(readOnly = true)
  public List<IndexDataOpenApiResult> fetchJoinedSorted(LocalDate from, LocalDate to, String indexName) {
    List<IndexDataJoinedRow> rows = dataRepo.findJoinedSorted(from, to, indexName);

    Map<String, Map<Long, List<IndexDataDto>>> bucket = new LinkedHashMap<>();
    for (IndexDataJoinedRow r : rows) {
      bucket.computeIfAbsent(r.indexName(), k -> new LinkedHashMap<>())
          .computeIfAbsent(r.indexInfoId(), k -> new ArrayList<>())
          .add(new IndexDataDto(
              null,
              r.indexInfoId(),
              r.baseDate(),
              r.sourceType(),
              r.marketPrice(),
              r.closingPrice(),
              r.highPrice(),
              r.lowPrice(),
              r.versus(),
              r.fluctuationRate(),
              r.tradingQuantity(),
              r.tradingPrice(),
              r.marketTotalAmount()
          ));
    }

    List<IndexDataOpenApiResult> out = new ArrayList<>();
    bucket.forEach((idxName, byInfoId) -> {
      List<IndexDataOpenApiResult.Group> groups = new ArrayList<>();
      byInfoId.forEach((infoId, list) -> {
        String cls = infoRepo.getOrThrow(infoId).getIndexClassification();
        groups.add(new IndexDataOpenApiResult.Group(infoId, cls, list));
      });
      out.add(new IndexDataOpenApiResult(idxName, groups));
    });
    return out;
  }

  private static String norm(String s) {
    if (s == null) return "";
    return s.trim().replaceAll("\\s+", " ");
  }
}