package com.findex.dto.autoSyncConfig;

import jakarta.validation.constraints.NotNull;

public record AutoSyncConfigUpdateRequest(
        @NotNull
        boolean enabled
) {
}
