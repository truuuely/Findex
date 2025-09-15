package com.findex.dto.syncjob;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record NormalizedIndexInfoItem (
    String indexClassification,
    String indexName,
    Integer employedItemsCount,
    LocalDate basePointInTime,
    Integer baseIndex
) {

    public static NormalizedIndexInfoItem from(OpenApiIndexInfoItem item) {
        final String indexClassification = normIndexInfo(item.idxCsf());
        final String indexName = normIndexInfo(item.idxNm());
        final Integer employedItemsCount = item.epyItmsCnt();
        final LocalDate basePointInTime = parseDate(item.basPntm());
        final Integer baseIndex = item.basIdx();

        if (indexClassification == null || indexName == null ||
            employedItemsCount == null || basePointInTime == null || baseIndex == null) {
            return null;
        }

        return new NormalizedIndexInfoItem(indexClassification, indexName, employedItemsCount, basePointInTime, baseIndex);
    }

    private static String normIndexInfo(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parseDate(String s) {
        try {
            return (s == null || s.isBlank())
                ? null
                : LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
