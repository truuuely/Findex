package com.findex.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findex.config.OpenApiProperties;
import com.findex.dto.syncjob.OpenApiIndexDataItem;
import com.findex.dto.syncjob.OpenApiIndexDataResponse;
import com.findex.dto.syncjob.OpenApiIndexInfoItem;
import com.findex.dto.syncjob.OpenApiIndexInfoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class MarketIndexClient {

    private final RestClient client;
    private final OpenApiProperties props;
    private final ObjectMapper om;

    private static final int NUM_OF_ROWS = 300;

    // 지수정보
    public OpenApiIndexInfoResponse callGetStockMarketIndex() {
        String uri = UriComponentsBuilder
            .fromPath("/GetMarketIndexInfoService/getStockMarketIndex")
            .queryParam("serviceKey", props.serviceKey())
            .queryParam("resultType", "json")
            .queryParam("numOfRows", NUM_OF_ROWS)
            .toUriString();

        String body = client.get().uri(uri).retrieve().body(String.class);
        try {
            JsonNode root = om.readTree(body);
            String code = root.path("response").path("header").path("resultCode").asText("00");
            if (!"00".equals(code)) {
                String msg = root.path("response").path("header").path("resultMsg").asText();
                throw new IllegalStateException("OpenAPI error " + code + ": " + msg);
            }
            JsonNode b = root.path("response").path("body");
            int total = b.path("totalCount").asInt(0);
            int size = b.path("numOfRows").asInt(NUM_OF_ROWS);

            JsonNode itemNode = b.path("items").path("item");
            List<OpenApiIndexInfoItem> items = new ArrayList<>();
            if (itemNode.isArray()) {
                for (JsonNode n : itemNode) items.add(convertInfo(n));
            } else if (itemNode.isObject()) {
                items.add(convertInfo(itemNode));
            }
            return new OpenApiIndexInfoResponse(items, total, size);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAPI response", e);
        }
    }

    // 지수데이터
    public OpenApiIndexDataResponse callGetStockMarketIndexData(
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
        if (idxCsf != null && !idxCsf.isBlank()) ub.queryParam("idxCsf", idxCsf);

        String uri = ub.toUriString();
        String body = client.get().uri(uri).retrieve().body(String.class);

        try {
            JsonNode root = om.readTree(body);
            String code = root.path("response").path("header").path("resultCode").asText("00");
            if (!"00".equals(code)) {
                String msg = root.path("response").path("header").path("resultMsg").asText();
                throw new IllegalStateException("OpenAPI error " + code + ": " + msg);
            }
            JsonNode b = root.path("response").path("body");
            int total = b.path("totalCount").asInt(0);
            int size = b.path("numOfRows").asInt(numOfRows);

            JsonNode itemNode = b.path("items").path("item");
            List<OpenApiIndexDataItem> items = new ArrayList<>();
            if (itemNode.isArray())      for (JsonNode n : itemNode) items.add(convertData(n));
            else if (itemNode.isObject()) items.add(convertData(itemNode));

            return new OpenApiIndexDataResponse(items, total, size);
        } catch (Exception e) {
            throw new IllegalStateException("Failed baseDateTo parse OpenAPI index-data response", e);
        }
    }

    // 파싱
    private static OpenApiIndexInfoItem convertInfo(JsonNode n) {
        return new OpenApiIndexInfoItem(
            text(n, "idxCsf"),
            text(n, "idxNm"),
            integer(n, "epyItmsCnt"),
            text(n, "basPntm"),
            integer(n, "basIdx")
        );
    }

    // basDt/가격 필드 alias 커버 + idxNm 포함
    private static OpenApiIndexDataItem convertData(JsonNode n) {
        LocalDate baseDate = parseDate(text(n, "basDt", "baseDate", "trdDd"));
        return new OpenApiIndexDataItem(
            text(n, "idxNm"),
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

    // 헬퍼
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

    private static BigDecimal decimal(JsonNode n, String... fields) {
        String s = text(n, fields);
        if (s == null) return null;
        try { return new BigDecimal(s.replaceAll(",", "")); }
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
}
