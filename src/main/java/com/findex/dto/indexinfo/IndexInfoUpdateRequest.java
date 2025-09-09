package com.findex.dto.indexinfo;

import java.time.LocalDate;

public record IndexInfoUpdateRequest(

    Integer employedItemsCount,
    LocalDate basePointInTime,
    Integer baseIndex,
    Boolean favorite
) {

}
