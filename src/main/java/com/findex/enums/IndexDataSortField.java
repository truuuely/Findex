package com.findex.enums;

public enum IndexDataSortField {
    BASE_DATE("baseDate"),
    MARKET_PRICE("marketPrice"),
    CLOSING_PRICE("closingPrice"),
    HIGH_PRICE("highPrice"),
    LOW_PRICE("lowPrice"),
    VERSUS("versus"),
    FLUCTUATION_RATE("fluctuationRate"),
    TRADING_QUANTITY("tradingQuantity"),
    TRADING_PRICE("tradingPrice"),
    MARKET_TOTAL_AMOUNT("marketTotalAmount");

    private final String field;

    IndexDataSortField(String field) {
        this.field = field;
    }

    public static IndexDataSortField from(String field) {
        for (IndexDataSortField value : values()) {
            if (value.field.equals(field)) {
                return value;
            }
        }

        return BASE_DATE;
    }
}
