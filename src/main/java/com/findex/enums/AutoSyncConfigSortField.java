package com.findex.enums;

public enum AutoSyncConfigSortField {
    INDEX_INFO_INDEX_NAME,
    ENABLED;

    public static AutoSyncConfigSortField parse(String s) {
        if (s.equals("enable")) {
            return ENABLED;
        } else {
            return INDEX_INFO_INDEX_NAME;
        }
    }
}
