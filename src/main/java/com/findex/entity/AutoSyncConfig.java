package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.NoSuchElementException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "auto_sync_config")
public class AutoSyncConfig extends BaseEntity {
    @Column(nullable = false)
    private boolean enabled;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "index_info_id", unique = true)
    private IndexInfo indexInfo;

    public void updateEnabled(Boolean enabled) {
        if (enabled == null) {
            throw new NoSuchElementException("enabled is null");
        }
        this.enabled = enabled;
    }
}
