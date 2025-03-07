package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BinanceLongShortRatioDTO {

    @JsonProperty("symbol")  // JSON 필드명과 매핑
    private String symbol;

    @JsonProperty("longShortRatio")  // JSON 필드명과 매핑
    private BigDecimal longShortRatio;

    @JsonProperty("longAccount")  // JSON 필드명과 매핑
    private BigDecimal longAccountRatio;

    @JsonProperty("shortAccount")  // JSON 필드명과 매핑
    private BigDecimal shortAccountRatio;

    @JsonProperty("timestamp")  // JSON 필드명과 매핑
    private BigInteger timestamp;
}
