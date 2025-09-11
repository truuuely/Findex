package com.findex.enums;

public enum AutoSyncConfigSortField {
    INDEX_INFO_INDEX_NAME("indexInfo.indexName"),
    ENABLED("enable");

    private final String field;

    AutoSyncConfigSortField(String field) {
        this.field = field;
    }

    public static AutoSyncConfigSortField parse(String s) {
        if (s == null || s.isBlank()) return INDEX_INFO_INDEX_NAME;
        for (AutoSyncConfigSortField t : values()) {
            if (t.field.equalsIgnoreCase(s) || t.name().equalsIgnoreCase(s))
                return t;
        }
        return INDEX_INFO_INDEX_NAME;
    }
}
