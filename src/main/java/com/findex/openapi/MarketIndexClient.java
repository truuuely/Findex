package com.findex.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketIndexClient {

  private final RestClient client;
  private final com.findex.config.OpenApiProperties props;
  private final ObjectMapper om;

  public PageResult callGetStockMarketIndex(int pageNo, int numOfRows) {
    if (props.serviceKey() == null || props.serviceKey().isBlank()) {
      throw new IllegalStateException("OpenAPI serviceKey is missing");
    }

    // 일부 키는 인코딩 필요할 수 있어 안전하게 인코딩
    String encodedKey = URLEncoder.encode(props.serviceKey(), StandardCharsets.UTF_8);

    String uri = UriComponentsBuilder.fromPath("/getStockMarketIndex")
        .queryParam("serviceKey", encodedKey)
        .queryParam("resultType", "json")
        .queryParam("pageNo", pageNo)
        .queryParam("numOfRows", numOfRows)
        .toUriString();

    // 문자열로 받기
    String body = client.get().uri(uri).retrieve().body(String.class);
    log.debug("OpenAPI body (masked): {}", mask(body));

    try {
      // 안전 파싱
      JsonNode root = om.readTree(body);
      JsonNode header = root.path("response").path("header");
      String code = header.path("resultCode").asText("00");
      if (!"00".equals(code)) {
        String msg = header.path("resultMsg").asText();
        throw new IllegalStateException("OpenAPI error " + code + ": " + msg);
      }

      JsonNode b = root.path("response").path("body");
      int total = b.path("totalCount").asInt(0);
      int page  = b.path("pageNo").asInt(pageNo);
      int size  = b.path("numOfRows").asInt(numOfRows);

      // items 변형 케이스 처리: 배열/단건객체/빈문자열 모두 대응
      JsonNode itemNode = b.path("items").path("item");
      List<OpenApiItem> items = new ArrayList<>();
      if (itemNode.isArray()) {
        for (JsonNode n : itemNode) items.add(convert(n));
      } else if (itemNode.isObject()) {
        items.add(convert(itemNode));
      } else {
        // "", null, missing → 데이터 없음
      }

      return new PageResult(items, total, page, size);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse OpenAPI response", e);
    }
  }

  private static OpenApiItem convert(JsonNode n) {
    return new OpenApiItem(
        text(n, "idxCsf"),
        text(n, "idxNm"),
        integer(n, "epyItmsCnt"),
        text(n, "basPntm"),
        integer(n, "basIdx")
    );
  }

  private static String text(JsonNode n, String f) {
    JsonNode v = n.get(f);
    if (v == null || v.isNull()) return null;
    String s = v.asText();
    return (s == null || s.isBlank()) ? null : s.trim();
  }

  private static Integer integer(JsonNode n, String f) {
    JsonNode v = n.get(f);
    if (v == null || v.isNull()) return null;
    // 숫자가 문자열로 올 때도 있어서 asInt 사용
    try { return Integer.valueOf(v.asText().trim()); }
    catch (Exception e) { return v.isNumber() ? v.intValue() : null; }
  }

  private static String mask(String body) {
    if (body == null) return null;
    // 과도한 마스킹 x 필요한 경우만
    return body.length() > 2000 ? body.substring(0, 2000) + "...(truncated)" : body;
  }

  // ==== 클라이언트가 반환하는 안전한 타입들 ====
  public record OpenApiItem(
      String idxCsf, String idxNm, Integer epyItmsCnt, String basPntm, Integer basIdx) {}

  public record PageResult(
      List<OpenApiItem> items, int totalCount, int pageNo, int numOfRows) {}
}