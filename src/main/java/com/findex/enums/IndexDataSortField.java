package com.findex.enums;

public enum IndexDataSortField {
    BASE_DATE,
    MARKET_PRICE,
    CLOSING_PRICE,
    HIGH_PRICE,
    LOW_PRICE,
    VERSUS,
    FLUCTUATION_RATE,
    TRADING_QUANTITY,
    TRADING_PRICE,
    MARKET_TOTAL_AMOUNT;

    public static IndexDataSortField from(String raw) {
        return switch (raw) {
            case "marketPrice" -> MARKET_PRICE;
            case "closingPrice" -> CLOSING_PRICE;
            case "highPrice" -> HIGH_PRICE;
            case "lowPrice" -> LOW_PRICE;
            case "versus" -> VERSUS;
            case "fluctuationRate" -> FLUCTUATION_RATE;
            case "tradingQuantity" -> TRADING_QUANTITY;
            case "tradingPrice" -> TRADING_PRICE;
            default -> BASE_DATE;
        };
    }
}
