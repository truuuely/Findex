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
    // id로 대상 찾기
    IndexInfo info = infoRepo.getOrThrow(req.indexInfoId());

    final int PAGE_SIZE = 500; // API 제한 고려
    List<MarketIndexClient.OpenApiIndexDataItem> items;

    //idxNm + idxCsf 로 페이징 수집
    items = fetchAllPages(info.getIndexName(), info.getIndexClassification(), req.from(), req.to(), PAGE_SIZE);

    //idxNm만
    if (items.isEmpty()) {
      items = fetchAllPages(info.getIndexName(), null, req.from(), req.to(), PAGE_SIZE);
      log.warn("[index-data] fallback#1 only idxNm. collected={}", items.size());
    }

    //전체 호출 후 로컬에서 이름 일치 필터
    if (items.isEmpty()) {
      List<MarketIndexClient.OpenApiIndexDataItem> all =
          fetchAllPages(null, null, req.from(), req.to(), PAGE_SIZE);
      String want = norm(info.getIndexName());
      items = all.stream().filter(it -> want.equals(norm(it.indexName()))).toList();
      log.warn("[index-data] fallback#2 local filter by idxNm. collected={}", items.size());
    }

    //업서트 저장 (baseDate 필수, null 필드는 update*가 무시)
    int saved = 0;
    for (var it : items) {
      if (it.baseDate() == null) continue;
      IndexData e = dataRepo.findByIndexInfoIdAndBaseDate(info.getId(), it.baseDate())
          .orElseGet(() -> new IndexData(info.getId(), it.baseDate(), IndexSourceType.OPEN_API));
      e.updatePrices(it.marketPrice(), it.closingPrice(), it.highPrice(), it.lowPrice());
      e.updateFluctuation(it.versus(), it.fluctuationRate());
      e.updateTrading(it.tradingQuantity(), it.tradingPrice(), it.marketTotalAmount());
      dataRepo.save(e);
      saved++;
    }
    log.info("[index-data] indexInfoId={} upsert saved={}", info.getId(), saved);

    //id + 기간으로만 조회해 반환 (날짜 내림차순)
    return fetchByIndexId(req.indexInfoId(), req.from(), req.to());
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

  // OpenAPI 페이징 수집: totalCount/numOfRows 기준으로 끝까지 호출
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