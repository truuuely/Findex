package com.findex.entity;

import com.findex.entity.base.BaseEntity;
import com.findex.enums.IndexSourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "index_data")
public class IndexData extends BaseEntity {

    @Column(nullable = false)
    private Long indexInfoId;

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IndexSourceType sourceType;

    @Column(precision = 20, scale = 2)
    private BigDecimal marketPrice;

    @Column(precision = 20, scale = 2)
    private BigDecimal closingPrice;

    @Column(precision = 20, scale = 1)
    private BigDecimal highPrice;

    @Column(precision = 20, scale = 1)
    private BigDecimal lowPrice;

    @Column(precision = 20, scale = 1)
    private BigDecimal versus;

    @Column(precision = 20, scale = 1)
    private BigDecimal fluctuationRate;

    private Long tradingQuantity;
    private Long tradingPrice;
    private Long marketTotalAmount;

    public void updatePrices(BigDecimal marketPrice, BigDecimal closingPrice, BigDecimal highPrice, BigDecimal lowPrice) {
        if (marketPrice != null) {
            this.marketPrice = marketPrice;
        }
        if (closingPrice != null) {
            this.closingPrice = closingPrice;
        }
        if (highPrice != null) {
            this.highPrice = highPrice;
        }
        if (lowPrice != null) {
            this.lowPrice = lowPrice;
        }
    }

    public void updateFluctuation(BigDecimal versus, BigDecimal fluctuationRate) {
        if (versus != null) {
            this.versus = versus;
        }
        if (fluctuationRate != null) {
            this.fluctuationRate = fluctuationRate;
        }
    }

    public void updateTrading(Long tradingQuantity, Long tradingPrice, Long marketTotalAmount) {
        if (tradingQuantity != null) {
            this.tradingQuantity = tradingQuantity;
        }
        if (tradingPrice != null) {
            this.tradingPrice = tradingPrice;
        }
        if (marketTotalAmount != null) {
            this.marketTotalAmount = marketTotalAmount;
        }
    }
}
