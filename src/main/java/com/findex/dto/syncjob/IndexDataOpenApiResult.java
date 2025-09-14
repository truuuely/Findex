package com.findex.dto.syncjob;

import com.findex.dto.indexdata.IndexDataDto;
import java.util.List;

public record IndexDataOpenApiResult(
    String indexName,
    List<Group> groups
) {
  public record Group(
      Long indexInfoId,
      String indexClassification,
      List<IndexDataDto> rows
  ) {}
}