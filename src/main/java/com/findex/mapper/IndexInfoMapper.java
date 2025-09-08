package com.findex.mapper;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.entity.IndexInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IndexInfoMapper {

    IndexInfoDto toDto(IndexInfo indexInfo);
}
