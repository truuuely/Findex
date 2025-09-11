package com.findex.entity;

import static com.findex.util.StringUtil.requireNonBlank;

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
import lombok.NonNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
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

    @Builder
    public IndexInfo(
        @NonNull String indexClassification,
        @NonNull String indexName,
        @NonNull Integer employedItemsCount,
        @NonNull LocalDate basePointInTime,
        @NonNull Integer baseIndex,
        @NonNull IndexSourceType sourceType,
        @NonNull Boolean favorite
    ) {
        this.indexClassification = requireNonBlank(indexClassification);
        this.indexName = requireNonBlank(indexName);
        this.employedItemsCount = employedItemsCount;
        this.basePointInTime = basePointInTime;
        this.baseIndex = baseIndex;
        this.sourceType = sourceType;
        this.favorite = favorite;
        this.autoSyncConfig = new AutoSyncConfig(false, this);
    }

    public IndexInfo update(
        Integer employedItemsCount,
        LocalDate basePointInTime,
        Integer baseIndex,
        Boolean favorite
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
        if (favorite != null) {
            this.favorite = favorite;
        }
        return this;
    }
}
