package com.findex.mapper;

import com.findex.dto.indexdata.IndexDataDto;
import com.findex.entity.IndexData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IndexDataMapper {
    IndexDataDto toDto(IndexData indexData);
}
