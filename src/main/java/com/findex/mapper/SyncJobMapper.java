package com.findex.mapper;

import com.findex.dto.syncjob.SyncJobDto;
import com.findex.entity.SyncJob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SyncJobMapper {

    SyncJobDto toDto(SyncJob syncJob);
}
