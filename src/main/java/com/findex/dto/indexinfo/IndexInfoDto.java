package com.findex.dto.indexinfo;

import com.findex.enums.IndexSourceType;
import java.time.LocalDate;

public record IndexInfoDto(
    Long id,
    String indexClassification,
    String indexName,
    Integer employedItemsCount,
    LocalDate basePointInTime,
    Integer baseIndex,
    IndexSourceType sourceType,
    boolean favorite
) {

}
