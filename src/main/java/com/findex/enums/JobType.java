package com.findex.enums;

public enum JobType {
    INDEX_INFO,
    INDEX_DATA;

    public static JobType from(String raw) {
        if (raw.equals("indexData")) {
            return INDEX_DATA;
        }
        return INDEX_INFO;
    }
}
