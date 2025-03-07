package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Binance Futures Funding Rate DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceFundingRateDTO {

    @JsonProperty("s")
    private String symbol; // 거래 심볼 (BTCUSDT 등)

    @JsonProperty("r")
    private BigDecimal fundingRate; // 펀딩 비율

    @JsonProperty("E")
    private BigInteger fundingTime; // 펀딩 적용 시간 (Unix Timestamp)

    @JsonProperty("p")
    private BigDecimal markPrice; // 마켓 가격 (펀딩 시점)
}
