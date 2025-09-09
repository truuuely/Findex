package com.findex.repository;

import com.findex.entity.IndexData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexDataRepository extends JpaRepository<IndexData, Long> {
}
