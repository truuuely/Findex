package com.findex.dto.autosyncconfig;

public record AutoSyncConfigDto(
    Long id,
    Long indexInfoId,
    String indexClassification,
    String indexName,
    boolean enabled
) {}
