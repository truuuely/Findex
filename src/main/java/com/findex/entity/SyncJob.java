package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import com.findex.enums.JobType;
import com.findex.enums.SyncJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "sync_job")
public class SyncJob extends BaseEntity {

    @Column(name = "index_info_id", nullable = false)
    private Long indexInfoId; // FK (IndexInfo). 관계 매핑 불필요 — 기존 스타일과 동일(Long 보관)

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", length = 10, nullable = false)
    private JobType jobType; // ENUM(INDEX_INFO, INDEX_DATA)

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate; // 대상날짜 (범위 요청이면 관례상 'to' 저장)

    @Column(name = "worker", length = 100, nullable = false)
    private String worker; // 요청자 IP 등

    @Column(name = "job_time", nullable = false)
    private LocalDateTime jobTime; // 작업 일시

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10, nullable = false)
    private SyncJobStatus result; // ENUM(SUCCESS, FAILED)

    // 편의 생성자
    public SyncJob(Long indexInfoId, JobType jobType, LocalDate targetDate, String worker, SyncJobStatus result) {
        this.indexInfoId = indexInfoId;
        this.jobType = jobType;
        this.targetDate = targetDate;
        this.worker = (worker == null || worker.isBlank()) ? "unknown" : worker;
        this.jobTime = LocalDateTime.now();
        this.result = result;
    }
}
