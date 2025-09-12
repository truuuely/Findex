package com.findex.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findex.config.OpenApiProperties;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
  private final OpenApiProperties props;
  private final ObjectMapper om;

  // [A] 지수정보(목록)
  public PageResult callGetStockMarketIndex(int pageNo, int numOfRows) {
    String uri = UriComponentsBuilder
        .fromPath("/GetMarketIndexInfoService/getStockMarketIndex")
        .queryParam("serviceKey", props.serviceKey())
        .queryParam("resultType", "json")
        .queryParam("pageNo", pageNo)
        .queryParam("numOfRows", numOfRows)
        .toUriString();

    String body = client.get().uri(uri).retrieve().body(String.class);
    log.info("[index-info] GET {}", uri);
    log.debug("OpenAPI body(masked): {}", mask(body));
    try {
      JsonNode root = om.readTree(body);
      String code = root.path("response").path("header").path("resultCode").asText("00");
      if (!"00".equals(code)) {
        String msg = root.path("response").path("header").path("resultMsg").asText();
        throw new IllegalStateException("OpenAPI error " + code + ": " + msg);
      }
      JsonNode b = root.path("response").path("body");
      int total = b.path("totalCount").asInt(0);
      int page = b.path("pageNo").asInt(pageNo);
      int size = b.path("numOfRows").asInt(numOfRows);

      JsonNode itemNode = b.path("items").path("item");
      List<OpenApiItem> items = new ArrayList<>();
      if (itemNode.isArray()) {
        for (JsonNode n : itemNode) items.add(convertInfo(n));
      } else if (itemNode.isObject()) {
        items.add(convertInfo(itemNode));
      }
      return new PageResult(items, total, page, size);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse OpenAPI response", e);
    }
  }

  // [B] 지수데이터(기간/지수명)  — 같은 InfoService 사용
  public IndexDataPageResult callGetStockMarketIndexData(
      String idxNm, String idxCsf, LocalDate from, LocalDate to, int pageNo, int numOfRows) {

    DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
    UriComponentsBuilder ub = UriComponentsBuilder
        .fromPath("/GetMarketIndexInfoService/getStockMarketIndex")
        .queryParam("serviceKey", props.serviceKey())
        .queryParam("resultType", "json")
        .queryParam("pageNo", pageNo)
        .queryParam("numOfRows", numOfRows)
        .queryParam("beginBasDt", from.format(ymd))
        .queryParam("endBasDt", to.format(ymd));

    if (idxNm  != null && !idxNm.isBlank())  ub.queryParam("idxNm", idxNm);
    if (idxCsf != null && !idxCsf.isBlank()) ub.queryParam("idxCsf", idxCsf); // ✅ 분류까지 전달

    String uri = ub.toUriString();
    String body = client.get().uri(uri).retrieve().body(String.class);
    log.info("[index-data] GET {}", uri);

    try {
      JsonNode root = om.readTree(body);
      String code = root.path("response").path("header").path("resultCode").asText("00");
      if (!"00".equals(code)) {
        String msg = root.path("response").path("header").path("resultMsg").asText();
        throw new IllegalStateException("OpenAPI error " + code + ": " + msg);
      }
      JsonNode b = root.path("response").path("body");
      int total = b.path("totalCount").asInt(0);
      int page = b.path("pageNo").asInt(pageNo);
      int size = b.path("numOfRows").asInt(numOfRows);

      JsonNode itemNode = b.path("items").path("item");
      List<OpenApiIndexDataItem> items = new ArrayList<>();
      if (itemNode.isArray())      for (JsonNode n : itemNode) items.add(convertData(n));
      else if (itemNode.isObject()) items.add(convertData(itemNode));

      return new IndexDataPageResult(items, total, page, size);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse OpenAPI index-data response", e);
    }
  }

  // ===== 파서들 =====
  private static OpenApiItem convertInfo(JsonNode n) {
    return new OpenApiItem(
        text(n, "idxCsf"),
        text(n, "idxNm"),
        integer(n, "epyItmsCnt"),
        text(n, "basPntm"),
        integer(n, "basIdx")
    );
  }

  // basDt/가격 필드 alias 커버 + idxNm 포함 (fallback 필터용)
  private static OpenApiIndexDataItem convertData(JsonNode n) {
    LocalDate baseDate = parseDate(text(n, "basDt", "baseDate", "trdDd"));
    return new OpenApiIndexDataItem(
        text(n, "idxNm"), // ✅ indexName 포함
        baseDate,
        decimal(n, "mkp", "marketPrice"),
        decimal(n, "clpr", "closingPrice"),
        decimal(n, "hipr", "highPrice"),
        decimal(n, "lopr", "lowPrice"),
        decimal(n, "vs", "versus"),
        decimal(n, "fltRt", "fluctuationRate", "rate"),
        along(n, "trqu", "tradingQuantity"),
        along(n, "trPrc", "tradingPrice"),
        along(n, "lstgMrktTotAmt", "mktTotAmt", "marketTotalAmount")
    );
  }


  // ===== 헬퍼들(각 1개씩만!) =====
  private static String text(JsonNode n, String... fields) {
    for (String f : fields) {
      JsonNode v = n.get(f);
      if (v != null && !v.isNull()) {
        String s = v.asText();
        if (s != null && !s.isBlank()) return s.trim();
      }
    }
    return null;
  }

  private static Integer integer(JsonNode n, String... fields) {
    String s = text(n, fields);
    if (s == null) return null;
    try { return Integer.valueOf(s.replaceAll(",", "")); }
    catch (Exception e) { return null; }
  }

  private static java.math.BigDecimal decimal(JsonNode n, String... fields) {
    String s = text(n, fields);
    if (s == null) return null;
    try { return new java.math.BigDecimal(s.replaceAll(",", "")); }
    catch (Exception e) { return null; }
  }

  private static Long along(JsonNode n, String... fields) {
    String s = text(n, fields);
    if (s == null) return null;
    try { return Long.valueOf(s.replaceAll(",", "")); }
    catch (Exception e) { return null; }
  }

  private static LocalDate parseDate(String s) {
    if (s == null) return null;
    String t = s.replaceAll("[^0-9]", "");
    if (t.length() == 8) {
      return LocalDate.of(
          Integer.parseInt(t.substring(0, 4)),
          Integer.parseInt(t.substring(4, 6)),
          Integer.parseInt(t.substring(6, 8))
      );
    }
    return null;
  }

  private static String mask(String body) {
    if (body == null) return null;
    return body.length() > 2000 ? body.substring(0, 2000) + "...(truncated)" : body;
  }

  // ===== 반환 타입 =====
  public record OpenApiItem(
      String idxCsf, String idxNm, Integer epyItmsCnt, String basPntm, Integer basIdx) {}

  public record PageResult(
      List<OpenApiItem> items, int totalCount, int pageNo, int numOfRows) {}

  // === 반환 타입 (indexName 추가!) ===
  public record OpenApiIndexDataItem(
      String indexName,
      LocalDate baseDate,
      java.math.BigDecimal marketPrice,
      java.math.BigDecimal closingPrice,
      java.math.BigDecimal highPrice,
      java.math.BigDecimal lowPrice,
      java.math.BigDecimal versus,
      java.math.BigDecimal fluctuationRate,
      Long tradingQuantity,
      Long tradingPrice,
      Long marketTotalAmount
  ) {}

  public record IndexDataPageResult(
      List<OpenApiIndexDataItem> items, int totalCount, int pageNo, int numOfRows) {}
}