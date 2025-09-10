package com.findex.service;


import com.findex.dto.syncjob.MarketIndexRoot;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.enums.SourceType;
import com.findex.openapi.MarketIndexClient;
import com.findex.repository.IndexInfoRepository;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class IndexInfoSyncService {

  private final MarketIndexClient client;         // 실제 OpenAPI 호출
  private final IndexInfoRepository repo;

  public record SyncResult(int fetched, int inserted, int updated) {}

  @Transactional
  public SyncResult syncAll() {
    final int pageSize = 100;
    int page = 1;
    int fetched = 0, inserted = 0, updated = 0;

    DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 페이지 고착(같은 페이지 반복) 방지용
    String lastFirstKey = null;
    final int HARD_PAGE_LIMIT = 200;

    while (true) {
      // OpenAPI 호출
      MarketIndexClient.PageResult pr = client.callGetStockMarketIndex(page, pageSize);
      var items = pr.items();
      if (items == null || items.isEmpty()) break;

      // 같은 페이지가 반복되는지..
      String firstKey = null;
      var first = items.get(0);
      if (first != null) firstKey = norm(first.idxCsf()) + "|" + norm(first.idxNm());
      if (firstKey != null && firstKey.equals(lastFirstKey)) break;
      lastFirstKey = firstKey;

      for (var it : items) {
        String cls = norm(it.idxCsf());
        String name = norm(it.idxNm());
        if (cls == null || name == null)
          continue; // 키 누락 스킵

        Integer cnt = it.epyItmsCnt();
        LocalDate bp = parseYmd(it.basPntm(), ymd);
        fetched++;

        var existingOpt = repo.findByIndexClassificationAndIndexName(cls, name);
        Integer bidx = it.basIdx();

        if (existingOpt.isPresent()) {
          var e = existingOpt.get();
          boolean changed =
              !Objects.equals(e.getEmployedItemsCount(), cnt) ||
                  !Objects.equals(e.getBasePointInTime(), bp) ||
                  !Objects.equals(e.getBaseIndex(), bidx);

          if (changed) {
            e.setEmployedItemsCount(cnt);
            e.setBasePointInTime(bp);
            e.setBaseIndex(bidx);
            repo.save(e);
            updated++;
          }
        } else {
          var n = IndexInfo.builder()
              .indexClassification(cls)
              .indexName(name)
              .employedItemsCount(cnt)
              .basePointInTime(bp)
              .baseIndex(bidx)
              .sourceType(IndexSourceType.OPEN_API)
              .favorite(false)
              .build();
          repo.save(n);
          inserted++;
        }
      }

      // 종료 조건: 메타 우선, 없으면 하드리밋으로 안전 탈출
      if (pr.pageNo() * pr.numOfRows() >= pr.totalCount()) break;
      if (page >= HARD_PAGE_LIMIT) break;
      page++;
    }

    return new SyncResult(fetched, inserted, updated);
  }

  private static String norm(String s) {
    if (s == null) return null;
    String t = s.trim().replaceAll("\\s+", " ");
    return t.isEmpty() ? null : t;
  }
  private static LocalDate parseYmd(String s, DateTimeFormatter fmt) {
    try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim(), fmt); }
    catch (Exception e) { return null; }
  }
}