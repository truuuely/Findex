package com.findex.controller;

import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.dto.syncjob.SyncJobDto;
import com.findex.dto.syncjob.SyncJobQuery;
import com.findex.service.SyncJobService;
import com.findex.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sync-jobs")
public class SyncJobController {

    private final SyncJobService syncJobService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CursorPageResponse findAll(@ModelAttribute SyncJobQuery query) {
        return syncJobService.findAll(query);
    }

    @PostMapping("/index-infos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Cacheable(cacheNames = "indexInfoCache", key = "'syncIndexInfo'", sync = true)
    public List<SyncJobDto> syncIndexInfo(HttpServletRequest httpRequest) {
        String worker = ClientIpResolver.resolve(httpRequest);
        return syncJobService.syncIndexInfo(worker);
    }

    @PostMapping("/index-data")
    @ResponseStatus(HttpStatus.OK)
    @Cacheable(
        value = "indexDataCache",
        key = "T(com.findex.util.CacheKeys).syncIndexData(#req)",
        sync = true)
    public List<SyncJobDto> syncIndexData(
        @RequestBody @Valid IndexDataOpenApiSyncRequest req,
        HttpServletRequest httpRequest
    ) {
        String worker = ClientIpResolver.resolve(httpRequest);
        return syncJobService.syncIndexData(req, worker);
    }
}
