package com.findex.service;

import com.findex.dto.indexinfo.IndexInfoCreateRequest;
import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.indexinfo.IndexInfoQuery;
import com.findex.dto.indexinfo.IndexInfoSummaryDto;
import com.findex.dto.indexinfo.IndexInfoUpdateRequest;
import com.findex.dto.response.CursorPageResponse;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.mapper.IndexInfoMapper;
import com.findex.repository.indexinfo.IndexInfoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexInfoService {

    private final IndexInfoRepository indexInfoRepository;
    private final IndexInfoMapper indexInfoMapper;

    public CursorPageResponse findAll(IndexInfoQuery query) {
        return indexInfoRepository.findAll(query);
    }

    @Transactional
    public IndexInfoDto create(IndexInfoCreateRequest request) {
        IndexInfo indexInfo = indexInfoRepository.save(
                IndexInfo.builder()
                    .indexClassification(request.indexClassification())
                    .indexName(request.indexName())
                    .employedItemsCount(request.employedItemsCount())
                    .basePointInTime(request.basePointInTime())
                    .baseIndex(request.baseIndex())
                    .sourceType(IndexSourceType.USER)
                    .favorite(request.favorite() != null && request.favorite())
                    .build()
                );

        return indexInfoMapper.toDto(indexInfo);
    }

    public IndexInfoDto findById(Long id) {
        return indexInfoMapper.toDto(indexInfoRepository.getOrThrow(id));
    }

    @Transactional
    public IndexInfoDto update(Long id, IndexInfoUpdateRequest request) {

        IndexInfo indexInfo = indexInfoRepository.getOrThrow(id);

        indexInfo.update(
            request.employedItemsCount(),
            request.basePointInTime(),
            request.baseIndex(),
            request.favorite()
        );

        return indexInfoMapper.toDto(indexInfo);
    }

    @Transactional
    public void delete(Long id) {
        indexInfoRepository.delete(indexInfoRepository.getOrThrow(id));
    }

    public List<IndexInfoSummaryDto> findAllSummaries() {
        return indexInfoRepository.findAllSummaries();
    }
}
