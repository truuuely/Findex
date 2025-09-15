package com.findex.repository.syncjob;

import com.findex.entity.SyncJob;
import com.findex.enums.JobType;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long>, SyncJobQueryRepository {

    @Query("""
        select max(s.targetDate)
          from SyncJob s
         where s.indexInfoId = :indexInfoId
           and s.jobType     = :jobType
           and s.worker      = :worker
    """)
    LocalDate findLastAutoTargetDate(
        @Param("indexInfoId") Long indexInfoId,
        @Param("jobType") JobType jobType,
        @Param("worker") String worker
    );
}
