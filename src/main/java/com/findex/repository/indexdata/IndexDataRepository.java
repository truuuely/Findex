package com.findex.repository.indexdata;

import com.findex.entity.IndexData;
import com.findex.exception.NotFoundException;
import com.findex.dto.syncjob.IndexDataJoinedRow;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public interface IndexDataRepository extends JpaRepository<IndexData, Long>, IndexDataQueryRepository {
  @Query("SELECT d.closingPrice FROM IndexData d, IndexInfo i WHERE d.indexInfoId = i.id AND i.id = :indexInfoId AND d.baseDate <= :baseDate ORDER BY d.baseDate DESC")
  List<BigDecimal> findClosingPrice(@Param("indexInfoId") Long indexInfoId,
      @Param("baseDate") LocalDate baseDate, Pageable pageable);

  Optional<IndexData> findByIndexInfoIdAndBaseDate(Long indexInfoId, LocalDate baseDate);

  default IndexData getOrThrow(Long id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("IndexData with id %s not found".formatted(id)));
  }

  @Query("SELECT d FROM IndexData d WHERE d.indexInfoId = :indexInfoId AND d.baseDate > :startDate ORDER BY d.baseDate ASC")
  List<IndexData> findChartData(@Param("indexInfoId") Long indexInfoId, @Param("startDate") LocalDate startDate);

  @Query("""
SELECT new com.findex.dto.syncjob.IndexDataJoinedRow(
  i.id, i.indexClassification, i.indexName,
    d.id, d.baseDate, d.sourceType,
    d.marketPrice, d.closingPrice, d.highPrice, d.lowPrice,
    d.versus, d.fluctuationRate,
    d.tradingQuantity, d.tradingPrice, d.marketTotalAmount
  )
  FROM IndexData d, IndexInfo i
  WHERE i.id = d.indexInfoId
    AND i.id = :indexInfoId
    AND d.baseDate BETWEEN :from AND :to
  ORDER BY d.baseDate DESC
""")
  List<IndexDataJoinedRow> findJoinedSortedByIndexId(@Param("indexInfoId") Long indexInfoId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}