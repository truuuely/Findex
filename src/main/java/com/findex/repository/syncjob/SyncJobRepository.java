package com.findex.repository.syncjob;

import com.findex.entity.SyncJob;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long>, SyncJobQueryRepository {}
