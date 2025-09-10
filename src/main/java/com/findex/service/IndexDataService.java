package com.findex.service;

import com.findex.dto.indexdata.IndexDataCreateRequest;
import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.indexdata.IndexDataUpdateRequest;
import com.findex.entity.IndexData;
import com.findex.enums.IndexSourceType;
import com.findex.mapper.IndexDataMapper;
import com.findex.repository.IndexDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexDataService {
    private final IndexDataRepository indexDataRepository;
    private final IndexDataMapper indexDataMapper;

    @Transactional
    public IndexDataDto create(IndexDataCreateRequest request) {
        IndexData indexData = indexDataRepository.save(new IndexData(
                request.indexInfoId(),
                request.baseDate(),
                IndexSourceType.USER,
                request.marketPrice(),
                request.closingPrice(),
                request.highPrice(),
                request.lowPrice(),
                request.versus(),
                request.fluctuationRate(),
                request.tradingQuantity(),
                request.tradingPrice(),
                request.marketTotalAmount()
        ));

        return indexDataMapper.toDto(indexData);
    }

    @Transactional
    public IndexDataDto update(Long id, IndexDataUpdateRequest request) {
        IndexData indexData = indexDataRepository.getOrThrow(id);

        indexData.updatePrices(request.marketPrice(), request.closingPrice(), request.highPrice(), request.lowPrice());
        indexData.updateFluctuation(request.versus(), request.fluctuationRate());
        indexData.updateTrading(request.tradingQuantity(), request.tradingPrice(), request.marketTotalAmount());

        return indexDataMapper.toDto(indexData);
    }

    @Transactional
    public void delete(Long id) {
        indexDataRepository.delete(indexDataRepository.getOrThrow(id));
    }
}
