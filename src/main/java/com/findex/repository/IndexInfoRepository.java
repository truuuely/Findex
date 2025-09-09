package com.findex.repository;

import com.findex.entity.IndexInfo;
import com.findex.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexInfoRepository extends JpaRepository<IndexInfo, Long> {

    default IndexInfo getOrThrow(Long id) {
        return findById(id).orElseThrow(() ->
            new NotFoundException(
                "IndexInfo with id %s not found".formatted(id))
        );
    }
}
