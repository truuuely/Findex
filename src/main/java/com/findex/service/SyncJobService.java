package com.findex.service;

import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.SyncJobQuery;
import com.findex.repository.syncjob.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncJobService {

    private final SyncJobRepository syncJobRepository;

    public CursorPageResponse findAll(SyncJobQuery query) {
        return syncJobRepository.findAll(query);
    }
}
