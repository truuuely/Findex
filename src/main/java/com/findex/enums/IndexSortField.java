package com.findex.enums;

import static org.springframework.util.StringUtils.hasText;

public enum IndexSortField {
    INDEX_CLASSIFICATION("indexClassification"),
    INDEX_NAME("indexName"),
    EMPLOYED_ITEMS_COUNT("employedItemsCount");

    public final String property;

    IndexSortField(String property) {
        this.property = property;
    }

    public static IndexSortField parse(String s) {
        if (!hasText(s)) return INDEX_CLASSIFICATION;
        for (IndexSortField f : values()) {
            if (f.property.equalsIgnoreCase(s) || f.name().equalsIgnoreCase(s)) {
                return f;
            }
        }
        return INDEX_CLASSIFICATION;
    }
}
