package com.findex.dto.syncjob;


import java.util.List;
import lombok.Data;

@Data
public class MarketIndexRoot {

  private Response response;

  //지수 데이터
  @Data
  public static class Response {
    private Body body;
  }

  @Data
  public static class Body {
    private Items items;
    private Integer totalCount;
    private Integer pageNo;
    private Integer numOfRows;
  }

  @Data
  public static class Items {
    private List<Item> item;
  }

  //지수 정보
  @Data
  public static class Item {
    private String idxCsf;     // 분류
    private String idxNm;      // 지수
    private Integer epyItmsCnt; // 채용 종목 수
    private String basPntm;    // 기준 시점 (yyyyMMdd)
    private Integer basIdx;    // 기준 지수 (옵션: 테이블에 있으니 같이 넣어둠)
  }
}