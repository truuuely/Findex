package com.findex.repository;

import com.findex.entity.IndexData;
import com.findex.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexDataRepository extends JpaRepository<IndexData, Long> {

    default IndexData getOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new NotFoundException("IndexData with id %s not found".formatted(id)));
    }
}
