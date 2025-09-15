package com.findex.openapi;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findex.config.OpenApiProperties;
import com.findex.dto.syncjob.OpenApiIndexDataItem;
import com.findex.dto.syncjob.OpenApiIndexInfoItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketIndexClient {

    private final RestClient restClient;
    private final OpenApiProperties openApiProperties;
    private final ObjectMapper objectMapper;

    private static final String[] PATH_SEG = {"GetMarketIndexInfoService", "getStockMarketIndex"};
    private static final String PATH = "/GetMarketIndexInfoService/getStockMarketIndex";
    private static final String RESULT_TYPE_JSON = "json";
    private static final int DEFAULT_NUM_OF_ROWS = 500;

    public List<OpenApiIndexInfoItem> fetchIndexInfo() {
        String uri = UriComponentsBuilder
            .fromPath(PATH)
            .queryParam("serviceKey", openApiProperties.serviceKey())
            .queryParam("resultType", RESULT_TYPE_JSON)
            .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
            .toUriString();


        String body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(body);

            JsonNode header = root.path("response").path("header");
            String code = header.path("resultCode").asText("00");
            if (!"00".equals(code)) {
                String msg = header.path("resultMsg").asText();
                throw new IllegalStateException("OpenAPI error %s: %s".formatted(code, msg));
            }

            JsonNode itemNode = root.path("response").path("body").path("items").path("item");

            List<OpenApiIndexInfoItem> items = new ArrayList<>();
            if (itemNode.isArray()) {
                for (JsonNode n : itemNode) items.add(convertInfo(n));
            } else if (itemNode.isObject()) {
                items.add(convertInfo(itemNode));
            }
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAPI response: getStockMarketIndex", e);
        }
    }

    // 지수데이터
    public List<OpenApiIndexDataItem> fetchIndexData(String idxNm, LocalDate from, LocalDate to) {
        String body = restClient.get().uri(uriBuilder -> {
            UriBuilder ub = uriBuilder
                .pathSegment(PATH_SEG)
                .queryParam("serviceKey", openApiProperties.serviceKey())
                .queryParam("resultType", RESULT_TYPE_JSON)
                .queryParam("numOfRows", DEFAULT_NUM_OF_ROWS)
                .queryParam("beginBasDt", from.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .queryParam("endBasDt",   to.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            if (StringUtils.hasText(idxNm)) ub.queryParam("idxNm", idxNm);
            return ub.build();
        }).retrieve().body(String.class);

        try {
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("response").path("header").path("resultCode").asText("00");
            if (!"00".equals(code)) {
                String msg = root.path("response").path("header").path("resultMsg").asText();
                throw new IllegalStateException("OpenAPI error " + code + ": " + msg);
            }
            JsonNode itemNode = root.path("response").path("body").path("items").path("item");
            List<OpenApiIndexDataItem> items = new ArrayList<>();
            if (itemNode == null || itemNode.isNull()) {
                return items;
            }
            if (itemNode.isArray()) {
                for (JsonNode n : itemNode) {
                    OpenApiIndexDataItem i = convertData(n);
                    if (i != null){
                        items.add(i);
                    }
                }
            } else if (itemNode.isObject()) {
                OpenApiIndexDataItem i = convertData(itemNode);
                if (i != null){
                    items.add(i);
                }
            }
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAPI response: getStockMarketIndexInfo", e);
        }
    }

    private static OpenApiIndexInfoItem convertInfo(JsonNode n) {
        return new OpenApiIndexInfoItem(
            text(n, "idxCsf"),
            text(n, "idxNm"),
            integer(n, "epyItmsCnt"),
            text(n, "basPntm"),
            integer(n, "basIdx")
        );
    }

    private static OpenApiIndexDataItem convertData(JsonNode n) {
        LocalDate baseDate = parseDate(text(n, "basDt", "baseDate", "trdDd"));
        if (baseDate == null) {
            return null;
        }
        return new OpenApiIndexDataItem(
            text(n, "idxCsf"),
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
        try { return new BigDecimal(s.replace(",", "")); }
        catch (Exception e) { return null; }
    }

    private static Long along(JsonNode n, String... fields) {
        String s = text(n, fields);
        if (s == null) return null;
        try { return Long.valueOf(s.replace(",", "")); }
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
