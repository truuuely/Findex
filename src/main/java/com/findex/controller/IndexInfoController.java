package com.findex.controller;

import com.findex.dto.indexinfo.IndexInfoCreateRequest;
import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.indexinfo.IndexInfoSummaryDto;
import com.findex.dto.indexinfo.IndexInfoUpdateRequest;
import com.findex.service.IndexInfoService;
import com.findex.service.IndexInfoSyncService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/index-infos")
public class IndexInfoController {

    private final IndexInfoService indexInfoService;
    private final IndexInfoSyncService syncService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndexInfoDto create(@RequestBody @Valid IndexInfoCreateRequest req) {
        return indexInfoService.create(req);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public IndexInfoDto findById(@PathVariable Long id) {
        return indexInfoService.findById(id);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public IndexInfoDto update(@PathVariable Long id, @RequestBody @Valid IndexInfoUpdateRequest req) {
        return indexInfoService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        indexInfoService.delete(id);
    }

    @GetMapping("/summaries")
    @ResponseStatus(HttpStatus.OK)
    public List<IndexInfoSummaryDto> findAllSummaries() {
        return indexInfoService.findAllSummaries();
    }

    //지수정보 가져올 수 있는 엔드포인트
    @PostMapping("/openapi/sync")
    @ResponseStatus(HttpStatus.OK)
    public SyncResponse syncAll() {
        var r = syncService.syncAll();
        return new SyncResponse(r.fetched(), r.inserted(), r.updated());
    }

    public record SyncResponse(
        int fetched,              // Open API에서 받아온 총 개수
        int inserted,    // 삽입 또는 OPEN_API행 갱신 개수
        int updated      // 원래 데이터가 존재해 덮어씌운 개수
    ) {}
}
