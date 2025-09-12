package com.findex.controller;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.syncjob.IndexDataOpenApiResult;
import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.service.IndexDataSyncService;
import com.findex.service.IndexInfoSyncService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sync-jobs")
public class SyncjobsInfoController {
  private final IndexInfoSyncService syncInfoService;
  private final IndexDataSyncService syncDataService;

  //지수정보 가져올 수 있는 엔드포인트
  @PostMapping("/index-infos")
  @ResponseStatus(HttpStatus.OK)
  public List<IndexInfoDto> syncAll() {
    return syncInfoService.SyncResponse();  // ← List 반환
  }
  // (신규) 지수데이터 동기화 + 출력
  @PostMapping("/index-data")
  @ResponseStatus(HttpStatus.OK)
  public List<IndexDataOpenApiResult> syncIndexData(@RequestBody @Valid IndexDataOpenApiSyncRequest req) {
    return syncDataService.syncFromOpenApi(req);
  }
}
