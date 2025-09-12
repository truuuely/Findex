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
import com.findex.openapi.MarketIndexClient.IndexDataPageResult;
import com.findex.repository.indexdata.IndexDataRepository;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
    IndexInfo info = infoRepo.getOrThrow(req.indexInfoId());

    final int PAGE_SIZE = 500; // API 상한 고려 (100/500 등)
    List<MarketIndexClient.OpenApiIndexDataItem> items;

    // 1차: 이름+분류로 전페이지 수집
    items = fetchAllPages(info.getIndexName(), info.getIndexClassification(), req.from(), req.to(), PAGE_SIZE);

    // 2차 fallback: 이름만
    if (items.isEmpty()) {
      items = fetchAllPages(info.getIndexName(), null, req.from(), req.to(), PAGE_SIZE);
      log.warn("[index-data] fallback#1 only idxNm. collected={}", items.size());
    }

    // 3차 fallback: 전체 받아 로컬 필터
    if (items.isEmpty()) {
      List<MarketIndexClient.OpenApiIndexDataItem> all = fetchAllPages(null, null, req.from(), req.to(), PAGE_SIZE);
      String want = norm(info.getIndexName());
      items = all.stream().filter(it -> want.equals(norm(it.indexName()))).toList();
      log.warn("[index-data] fallback#2 local filter by idxNm. collected={}", items.size());
    }

    // 저장
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

    // 조회/반환 (id+기간, 날짜 내림차순)
    return fetchByIndexId(req.indexInfoId(), req.from(), req.to());
  }

  // 페이징 수집 헬퍼
  private void collectAll(List<MarketIndexClient.OpenApiIndexDataItem> sink, Supplier<IndexDataPageResult> caller) {
    int page = 1;
    while (true) {
      var pr = caller.get();
      if (pr.items() != null) sink.addAll(pr.items());
      if (pr.totalCount() <= page * pr.numOfRows() || pr.items() == null || pr.items().isEmpty()) break;
      page++;
      // 다음 페이지 호출을 위해 caller가 page를 받도록 바꿔도 되지만, 간단히 람다 안에서 page 인자만 갱신하는 방식으로 분해
      // (아래 pageCall에서 page 값을 가지고 새 caller를 만들어 주세요)
      caller = nextCaller(caller, page);
    }
  }

  // page를 바꾼 caller 생성 (간단 구현)
  private Supplier<MarketIndexClient.IndexDataPageResult> nextCaller(Supplier<MarketIndexClient.IndexDataPageResult> old, int newPage) {
    return old; // 아래 pageCall을 직접 쓰는 편이 더 단순해서, nextCaller는 사용하지 않을 수도 있습니다.
  }

  // 호출자 래퍼 (가독성 위해 분리)
  private MarketIndexClient.IndexDataPageResult pageCall(String idxNm, String idxCsf,
      IndexDataOpenApiSyncRequest req, int page, int size) {
    return client.callGetStockMarketIndexData(idxNm, idxCsf, req.from(), req.to(), page, size);
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

  private static String norm(String s) {
    if (s == null) return "";
    return s.trim().replaceAll("\\s+", " ");
  }

  private List<MarketIndexClient.OpenApiIndexDataItem> fetchAllPages(
      String idxNm, String idxCsf, LocalDate from, LocalDate to, int pageSize
  ) {
    List<MarketIndexClient.OpenApiIndexDataItem> out = new ArrayList<>();
    int page = 1;

    while (true) {
      var pr = client.callGetStockMarketIndexData(idxNm, idxCsf, from, to, page, pageSize);

      if (pr.items() != null && !pr.items().isEmpty()) {
        out.addAll(pr.items());
      }

      // 종료 조건: 더 이상 다음 페이지가 없으면 break
      if (pr.items() == null || pr.items().isEmpty() || page * pr.numOfRows() >= pr.totalCount()) {
        break;
      }
      page++;
    }
    return out;
  }
}