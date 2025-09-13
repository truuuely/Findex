package com.findex.dto.autoSyncConfig;

public record AutoSyncConfigDto(
    Long id,
    Long indexInfoId,
    String indexClassification,
    String indexName,
    boolean enabled
) {}
