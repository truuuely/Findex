package com.findex.service;

import com.findex.dto.indexinfo.IndexInfoCreateRequest;
import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.mapper.IndexInfoMapper;
import com.findex.repository.IndexInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexInfoService {

    private final IndexInfoRepository indexInfoRepository;

    private final IndexInfoMapper indexInfoMapper;

    @Transactional
    public IndexInfoDto create(IndexInfoCreateRequest req) {
        IndexInfo indexInfo = indexInfoRepository.save(
                new IndexInfo(
                    req.indexClassification(),
                    req.indexName(),
                    req.employedItemsCount(),
                    req.basePointInTime(),
                    req.baseIndex(),
                    IndexSourceType.USER,
                    req.favorite() != null && req.favorite()
                )
        );

        return indexInfoMapper.toDto(indexInfo);
    }
}
