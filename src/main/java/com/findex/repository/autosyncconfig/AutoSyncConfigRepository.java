package com.findex.repository.autosyncconfig;

import com.findex.entity.AutoSyncConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoSyncConfigRepository extends JpaRepository<AutoSyncConfig, Long>, AutoSyncConfigQueryRepository {

    @Query("select c.indexInfo.id from AutoSyncConfig c where c.enabled = true")
    List<Long> findEnabledIndexInfoIds();
}
