package com.findex.repository;

import com.findex.entity.IndexData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IndexDataRepository extends JpaRepository<IndexData, Long> {

  @Query("SELECT d.closingPrice FROM IndexData d, IndexInfo i WHERE d.indexInfoId = i.id AND i.id = :indexInfoId AND d.baseDate <= :baseDate ORDER BY d.baseDate DESC")
  List<BigDecimal> findClosingPrice(@Param("indexInfoId") Long indexInfoId, @Param("baseDate") LocalDate baseDate, Pageable pageable);
}
