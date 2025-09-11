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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Builder //추가
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
        this.indexClassification = indexClassification;
        this.indexName = indexName;
        this.employedItemsCount = employedItemsCount;
        this.basePointInTime = basePointInTime;
        this.baseIndex = baseIndex;
        this.sourceType = sourceType;
        this.favorite = favorite;
        this.autoSyncConfig = new AutoSyncConfig(false, this);
    }
}
