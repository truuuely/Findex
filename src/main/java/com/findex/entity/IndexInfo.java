package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import com.findex.enums.IndexSourceType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "index_info")
public class IndexInfo extends BaseEntity {

    private String indexClassification;

    private String indexName;

    @Setter
    private Integer employedItemsCount;

    @Setter
    private LocalDate basePointInTime;

    @Setter
    private Integer baseIndex;

    @Setter
    @Enumerated(EnumType.STRING)
    private IndexSourceType sourceType;

    @Setter
    private boolean favorite;
}
