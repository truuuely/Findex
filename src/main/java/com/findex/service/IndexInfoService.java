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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexInfoService {

    private final IndexInfoRepository indexInfoRepository;
    private final AutoSyncConfigService autoSyncConfigService;
    private final IndexInfoMapper indexInfoMapper;

    public CursorPageResponse findAll(IndexInfoQuery query) {
        return indexInfoRepository.findAll(query);
    }

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
        autoSyncConfigService.create(indexInfo);
        return indexInfoMapper.toDto(indexInfo);
    }

    public IndexInfoDto findById(Long id) {
        return indexInfoMapper.toDto(indexInfoRepository.getOrThrow(id));
    }

    @Transactional
    public IndexInfoDto update(Long id, IndexInfoUpdateRequest req) {

        IndexInfo indexInfo = indexInfoRepository.getOrThrow(id);

        if (req.employedItemsCount() != null) {
            indexInfo.setEmployedItemsCount(req.employedItemsCount());
        }

        if (req.basePointInTime() != null) {
            indexInfo.setBasePointInTime(req.basePointInTime());
        }

        if (req.baseIndex() != null) {
            indexInfo.setBaseIndex(req.baseIndex());
        }

        if (req.favorite() != null) {
            indexInfo.setFavorite(req.favorite());
        }

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
