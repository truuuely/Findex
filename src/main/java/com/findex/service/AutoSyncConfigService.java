package com.findex.service;

import com.findex.dto.autoSyncConfig.AutoSyncConfigDto;
import com.findex.dto.autoSyncConfig.AutoSyncConfigUpdateRequest;
import com.findex.entity.AutoSyncConfig;
import com.findex.exception.NotFoundException;
import com.findex.mapper.AutoSyncConfigMapper;
import com.findex.repository.AutoSyncConfigRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoSyncConfigService {
    private final AutoSyncConfigRepository autoSyncConfigRepository;
    private final AutoSyncConfigMapper autoSyncConfigMapper;

    @Transactional
    public AutoSyncConfigDto update(Long id, AutoSyncConfigUpdateRequest request) {
        AutoSyncConfig autoSyncConfig = autoSyncConfigRepository.findById(id).orElseThrow(
                () -> new NotFoundException("AutoSyncConfig with id %s not found".formatted(id)));
        autoSyncConfig.setEnabled(request.enabled());

        return autoSyncConfigMapper.toDto(autoSyncConfig);
    }
}
