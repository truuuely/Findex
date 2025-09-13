package com.findex.repository.indexdata;

import com.findex.entity.IndexData;
import com.findex.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IndexDataRepository extends JpaRepository<IndexData, Long>, IndexDataQueryRepository {

  default IndexData getOrThrow(Long id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("IndexData with id %s not found".formatted(id)));
  }

  @Query("SELECT d FROM IndexData d WHERE d.indexInfoId = :indexInfoId AND d.baseDate > :startDate ORDER BY d.baseDate ASC")
  List<IndexData> findChartData(@Param("indexInfoId") Long indexInfoId, @Param("startDate") LocalDate startDate);
}
