package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import com.findex.enums.IndexSourceType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Builder //추가
@Table(name = "index_info")
public class IndexInfo extends BaseEntity {

    private String indexClassification;

    private String indexName;

    private Integer employedItemsCount;

    private LocalDate basePointInTime;

    private Integer baseIndex;

    @Enumerated(EnumType.STRING)
    private IndexSourceType sourceType;

    private boolean favorite;
}
