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

}
