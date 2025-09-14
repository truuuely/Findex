package com.findex.service;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.openapi.MarketIndexClient;
import com.findex.repository.indexinfo.IndexInfoRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndexInfoSyncService {

    private final MarketIndexClient client;
    private final IndexInfoRepository repo;

    @Transactional
    public List<IndexInfoDto> SyncResponse() {
        final int limit = 300;
        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");

        var pr = client.callGetStockMarketIndex(1, limit);
        if (pr == null || pr.items() == null || pr.items().isEmpty()) return List.of();

        List<IndexInfoDto> out = new ArrayList<>();

        for (var it : pr.items()) {
            //값 정리
            String cls  = norm(it.idxCsf());
            String name = norm(it.idxNm());
            Integer cnt = it.epyItmsCnt();
            LocalDate bp = parseYmd(it.basPntm(), ymd);
            Integer bidx = it.basIdx();

            //null 있으면 생성/업데이트 모두 스킵해서 호출 (요구사항 개수와 같음)
            if (cls == null || name == null || cnt == null || bp == null || bidx == null) {
                continue;
            }

            // 빌더 제거 후 upsert 사용
            Optional<IndexInfo> exist = repo.findByIndexClassificationAndIndexName(cls, name);
            IndexInfo saved;
            if (exist.isPresent()) {
                IndexInfo e = exist.get();
                e.update(cnt, bp, bidx, null);              // null 무시 업데이트
                saved = repo.save(e);
            } else {
                // 생성자 직접 호출
                saved = repo.save(new IndexInfo(
                    cls,
                    name,
                    cnt,
                    bp,
                    bidx,
                    IndexSourceType.OPEN_API,
                    false
                ));
            }

            out.add(toDto(saved));
        }
        return out;
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
    private static IndexInfoDto toDto(IndexInfo e) {
        return new IndexInfoDto(
            e.getId(), e.getIndexClassification(), e.getIndexName(),
            e.getEmployedItemsCount(), e.getBasePointInTime(), e.getBaseIndex(),
            e.getSourceType(), e.isFavorite()
        );
    }
}