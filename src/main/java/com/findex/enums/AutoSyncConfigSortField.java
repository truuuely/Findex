package com.findex.enums;

public enum AutoSyncConfigSortField {
    INDEX_INFO_INDEX_NAME("indexInfo.indexName"),
    ENABLED("enable");

    AutoSyncConfigSortField(String s) {
    }

    public static AutoSyncConfigSortField parse(String s) {
        if (s == null || s.isBlank()) return INDEX_INFO_INDEX_NAME;
        for (AutoSyncConfigSortField t : AutoSyncConfigSortField.values()) {
            if (t.name().equalsIgnoreCase(s))
                return t;
        }
        return INDEX_INFO_INDEX_NAME;
    }
}
