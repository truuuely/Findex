package com.findex.repository;

import com.findex.entity.IndexInfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexInfoRepository extends JpaRepository<IndexInfo, Long> {

  List<IndexInfo> findAllByFavoriteIsTrue();
  List<IndexInfo> findAllByFavorite(boolean favorite);
}
