package com.findex.enums;


public enum AutoSyncConfigSortField {
    INDEX_INFO_INDEX_NAME,
    ENABLED;

    public static AutoSyncConfigSortField from(String raw) {
        if ("enabled".equals(raw)) {
            return ENABLED;
        }
        return INDEX_INFO_INDEX_NAME;
    }
}
