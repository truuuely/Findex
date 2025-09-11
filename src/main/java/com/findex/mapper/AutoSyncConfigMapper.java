package com.findex.mapper;

import com.findex.dto.autoSyncConfig.AutoSyncConfigDto;
import com.findex.entity.AutoSyncConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AutoSyncConfigMapper {
    @Mapping(source = "indexInfo.id", target = "indexInfoId")
    @Mapping(source = "indexInfo.indexClassification", target = "indexClassification")
    @Mapping(source = "indexInfo.indexName", target = "indexName")
    AutoSyncConfigDto toDto(AutoSyncConfig autoSyncConfig);
}
