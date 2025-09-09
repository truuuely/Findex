package com.findex.repository.indexinfo;

import com.findex.dto.indexinfo.IndexInfoSummaryDto;
import com.findex.entity.IndexInfo;
import com.findex.exception.NotFoundException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndexInfoRepository extends JpaRepository<IndexInfo, Long>, IndexInfoQueryRepository {

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
