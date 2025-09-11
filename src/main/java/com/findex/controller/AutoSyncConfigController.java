package com.findex.controller;

import com.findex.dto.autoSyncConfig.AutoSyncConfigDto;
import com.findex.dto.autoSyncConfig.AutoSyncConfigQuery;
import com.findex.dto.autoSyncConfig.AutoSyncConfigUpdateRequest;
import com.findex.dto.response.CursorPageResponse;
import com.findex.service.AutoSyncConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auto-sync-configs")
@RequiredArgsConstructor
public class AutoSyncConfigController {
    private final AutoSyncConfigService autoSyncConfigService;

    @PatchMapping("/{id}")
    public AutoSyncConfigDto update(@PathVariable Long id, @RequestBody @Valid AutoSyncConfigUpdateRequest request) {
        return autoSyncConfigService.update(id, request);
    }

    @GetMapping
    public CursorPageResponse getAll(@ModelAttribute AutoSyncConfigQuery query) {
        return autoSyncConfigService.getAll(query);
    }
}
