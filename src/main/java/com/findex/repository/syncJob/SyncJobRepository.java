package com.findex.repository.syncJob;

import com.findex.dto.syncjob.SyncJobRowDto;
import com.findex.entity.SyncJob;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

  // 최근 작업 내역
  @Query("""
      SELECT new com.findex.dto.syncjob.SyncJobRowDto(
        j.jobType, i.indexName, j.targetDate, j.worker, j.jobTime, j.result
      )
      FROM SyncJob j JOIN IndexInfo i ON i.id = j.indexInfoId
      ORDER BY j.jobTime DESC
    """)
  List<SyncJobRowDto> findRecent(Pageable pageable);
}