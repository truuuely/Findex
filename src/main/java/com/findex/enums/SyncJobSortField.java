package com.findex.enums;

public enum SyncJobSortField {
    JOB_TYPE,
    TARGET_DATE;

    public static SyncJobSortField from(String raw) {
        if (raw.equals("targetDate")) {
            return TARGET_DATE;
        }
        return JOB_TYPE;
    }
}
