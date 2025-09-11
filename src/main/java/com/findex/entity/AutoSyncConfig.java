package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "auto_sync_config")
public class AutoSyncConfig extends BaseEntity {
    @Setter
    @Column(nullable = false)
    private boolean enabled;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "index_info_id", unique = true)
    private IndexInfo indexInfo;
}
