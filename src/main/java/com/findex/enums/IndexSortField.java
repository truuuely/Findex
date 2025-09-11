package com.findex.enums;

public enum IndexSortField {
    INDEX_CLASSIFICATION,
    INDEX_NAME,
    EMPLOYED_ITEMS_COUNT;

    public static IndexSortField from(String raw) {
        return switch (raw) {
            case "indexName" -> INDEX_NAME;
            case "employedItemsCount" -> EMPLOYED_ITEMS_COUNT;
            default -> INDEX_CLASSIFICATION;
        };
    }
}
