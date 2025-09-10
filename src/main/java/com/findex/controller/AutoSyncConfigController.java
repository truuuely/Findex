package com.findex.controller;

import com.findex.service.AutoSyncConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller(value = "api/auto-sync-configs")
@RequiredArgsConstructor
public class AutoSyncConfigController {
    private AutoSyncConfigService autoSyncConfigService;


}
