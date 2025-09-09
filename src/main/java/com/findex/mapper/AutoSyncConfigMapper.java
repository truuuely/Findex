package com.findex.mapper;

import com.findex.dto.autoSyncConfig.AutoSyncConfigDto;
import com.findex.entity.AutoSyncConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AutoSyncConfigMapper {
    AutoSyncConfigDto toDto(AutoSyncConfig autoSyncConfig);
}
