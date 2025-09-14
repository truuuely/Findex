package com.findex.controller;

import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.syncjob.IndexDataOpenApiResult;
import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.service.IndexDataSyncService;
import com.findex.service.IndexInfoSyncService;
import jakarta.servlet.http.HttpServletRequest;
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


  @PostMapping("/index-data")
  @ResponseStatus(HttpStatus.OK)
  public List<IndexDataOpenApiResult> syncIndexData(
      @RequestBody @Valid IndexDataOpenApiSyncRequest req,
      HttpServletRequest httpRequest
  ) {
    String worker = resolveClientIp(httpRequest);
    return syncDataService.syncFromOpenApi(req, worker); // IP 넘김
  }

  private String resolveClientIp(HttpServletRequest request) {
    String ip = null;

    // 프록시 헤더 우선
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      ip = xff.split(",")[0].trim(); // 첫 IP
    } else {
      String xrip = request.getHeader("X-Real-IP");
      if (xrip != null && !xrip.isBlank()) ip = xrip.trim();
    }

    // 없으면 RemoteAddr
    if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();

    // 정규화
    if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
    if (ip.startsWith("::ffff:")) ip = ip.substring(7);      // IPv4-mapped
    int pct = ip.indexOf('%'); if (pct > 0) ip = ip.substring(0, pct); // zoneId

    return ip;
  }
}
