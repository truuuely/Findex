package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import com.findex.enums.IndexSourceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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

    @OneToOne(
        mappedBy = "indexInfo",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private AutoSyncConfig autoSyncConfig;

    public IndexInfo(
        String indexClassification,
        String indexName,
        Integer employedItemsCount,
        LocalDate basePointInTime,
        Integer baseIndex,
        IndexSourceType sourceType,
        boolean favorite
    ) {
        this.indexClassification = Objects.requireNonNull(indexClassification, "indexClassification is null");
        this.indexName = Objects.requireNonNull(indexName, "indexName is null");
        this.employedItemsCount = Objects.requireNonNull(employedItemsCount, "employedItemsCount is null");
        this.basePointInTime = Objects.requireNonNull(basePointInTime, "basePointInTime is null");
        this.baseIndex = Objects.requireNonNull(baseIndex, "baseIndex is null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType is null");
        this.favorite = favorite;
        this.autoSyncConfig = new AutoSyncConfig(false, this);
    }

    public IndexInfo update(
        Integer employedItemsCount,
        LocalDate basePointInTime,
        Integer baseIndex,
        boolean favorite
    ) {
        if (employedItemsCount != null) {
            this.employedItemsCount = employedItemsCount;
        }
        if (basePointInTime != null) {
            this.basePointInTime = basePointInTime;
        }
        if (baseIndex != null) {
            this.baseIndex = baseIndex;
        }
        this.favorite = favorite;
        return this;
    }
}
