package com.findex.repository;

import com.findex.dto.dashboard.IndexPerformanceRawDto;
import com.findex.dto.indexinfo.IndexInfoSummaryDto;
import com.findex.entity.IndexInfo;
import com.findex.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IndexInfoRepository extends JpaRepository<IndexInfo, Long> {

  List<IndexInfo> findAllByFavorite(boolean favorite);
  Optional<IndexInfo> findByIndexClassificationAndIndexName(String indexClassification, String indexName);

  @Query("""
          SELECT new com.findex.dto.indexinfo.IndexInfoSummaryDto(
              i.id, i.indexClassification, i.indexName
          )
          FROM IndexInfo i
      """)
  List<IndexInfoSummaryDto> findAllSummaries();

  default IndexInfo getOrThrow(Long id) {
    return findById(id).orElseThrow(() ->
        new NotFoundException(
            "IndexInfo with id %s not found".formatted(id))
    );
  }

  @Query("""
          SELECT new com.findex.dto.dashboard.IndexPerformanceRawDto(
              i,
              (SELECT p1.closingPrice FROM IndexData p1 WHERE p1.indexInfoId = i.id AND p1.baseDate <= :currentDate ORDER BY p1.baseDate DESC LIMIT 1),
              (SELECT p2.closingPrice FROM IndexData p2 WHERE p2.indexInfoId = i.id AND p2.baseDate <= :beforeDate ORDER BY p2.baseDate DESC LIMIT 1)
          )
          FROM IndexInfo i
          WHERE i.favorite = true
      """)
  List<IndexPerformanceRawDto> findPerformanceRawData(@Param("currentDate") LocalDate currentDate, @Param("beforeDate") LocalDate beforeDate);
}
