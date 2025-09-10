package com.findex.repository;

import com.findex.dto.indexinfo.IndexInfoSummaryDto;
import com.findex.entity.IndexInfo;
import java.util.List;
import com.findex.exception.NotFoundException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndexInfoRepository extends JpaRepository<IndexInfo, Long> {

  List<IndexInfo> findAllByFavoriteIsTrue();
  List<IndexInfo> findAllByFavorite(boolean favorite);
  //키 검색 메서드 중복 데이터 덮어씌우기
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
}
