package com.findex.enums;

import java.util.Arrays;

public enum IndexDataExportHeader {
    BASE_DATE("기준일자"),
    MARKET_PRICE("시가"),
    CLOSING_PRICE("종가"),
    HIGH_PRICE("고가"),
    LOW_PRICE("저가"),
    VERSUS("전일대비등락"),
    FLUCTUATION_RATE("등락률"),
    TRADING_QUANTITY("거래량"),
    TRADING_PRICE("거래대금"),
    MARKET_TOTAL_AMOUNT("시가총액");

    private final String name;

    IndexDataExportHeader(String name) {
        this.name = name;
    }

    public static String[] getNames() {
        return Arrays.stream(IndexDataExportHeader.values())
                .map(indexDataExportHeader -> indexDataExportHeader.name)
                .toArray(String[]::new);
    }
}

