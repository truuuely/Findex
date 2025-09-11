package com.findex.service;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.openapi.MarketIndexClient;
import com.findex.repository.indexinfo.IndexInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexInfoSyncService {

    private final MarketIndexClient client;         // 실제 OpenAPI 호출
    private final IndexInfoRepository repo;

    @Transactional
    public List<IndexInfoDto> SyncResponse() {
        final int limit = 300;  // ← 200개만 연동
        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");

        MarketIndexClient.PageResult pr = client.callGetStockMarketIndex(1, limit);
        var items = (pr != null) ? pr.items() : null;

        List<IndexInfoDto> result = new ArrayList<>();
        if (items == null || items.isEmpty()) return result;

        for (var it : items) { // it: MarketIndexClient.OpenApiItem
            String cls = norm(it.idxCsf());
            String name = norm(it.idxNm());
            if (cls == null || name == null) continue;

            Integer cnt = it.epyItmsCnt();
            LocalDate bp = parseYmd(it.basPntm(), ymd);
            Integer bidx = it.basIdx();

            var existingOpt = repo.findByIndexClassificationAndIndexName(cls, name);
            IndexInfo saved;
            if (existingOpt.isPresent()) {
                var e = existingOpt.get();
                e.setEmployedItemsCount(cnt);
                e.setBasePointInTime(bp);
                e.setBaseIndex(bidx);
                // sourceType/favorite은 보존 (필요시 e.setSourceType(OPEN_API) 적용)
                saved = repo.save(e);
            } else {
                saved = repo.save(new IndexInfo(cls, name, cnt, bp, bidx, IndexSourceType.OPEN_API, false));
            }
            result.add(toDto(saved));
        }

        return result; // ← List<IndexInfoDto>
    }

    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parseYmd(String s, DateTimeFormatter fmt) {
        try {
            return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim(), fmt);
        } catch (Exception e) {
            return null;
        }
    }

    private static IndexInfoDto toDto(IndexInfo e) {
        return new IndexInfoDto(
                e.getId(),
                e.getIndexClassification(),
                e.getIndexName(),
                e.getEmployedItemsCount(),
                e.getBasePointInTime(),
                e.getBaseIndex(),
                e.getSourceType(),
                e.isFavorite()
        );
    }
}