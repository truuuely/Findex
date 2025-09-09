package com.findex.enums;

import static org.springframework.util.StringUtils.hasText;

public enum IndexSortField {
    INDEX_CLASSIFICATION((byte) 0, "indexClassification"),
    INDEX_NAME((byte) 1, "indexName"),
    EMPLOYED_ITEMS_COUNT((byte) 2, "employedItemsCount");

    public final byte key;
    public final String property;

    IndexSortField(byte key, String property) {
        this.key = key;
        this.property = property;
    }

    public static IndexSortField fromKey(byte key) {
        for (IndexSortField f : values()) {
            if (f.key == key) {
                return f;
            }
        }
        return INDEX_CLASSIFICATION;
    }

    public static IndexSortField parse(String s) {
        if (hasText(s)) return INDEX_CLASSIFICATION;
        for (IndexSortField f : values()) {
            if (f.property.equalsIgnoreCase(s) || f.name().equalsIgnoreCase(s)) {
                return f;
            }
        }
        return INDEX_CLASSIFICATION;
    }
}
