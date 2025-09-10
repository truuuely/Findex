package com.findex.controller;

import com.findex.dto.indexinfo.IndexInfoCreateRequest;
import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.indexinfo.IndexInfoQuery;
import com.findex.dto.indexinfo.IndexInfoSummaryDto;
import com.findex.dto.indexinfo.IndexInfoUpdateRequest;
import com.findex.dto.response.CursorPageResponse;
import com.findex.service.IndexInfoService;
import com.findex.service.IndexInfoSyncService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sync-jobs")
public class SyncjobsInfoController {
  private final IndexInfoSyncService syncService;

  //지수정보 가져올 수 있는 엔드포인트
  @PostMapping("/index-infos")
  @ResponseStatus(HttpStatus.OK)
  public List<IndexInfoDto> syncAll() {
    return syncService.SyncResponse();  // ← List 반환
  }

}
