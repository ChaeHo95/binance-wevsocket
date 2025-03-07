package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Binance Futures WebSocket 24시간 티커 데이터 DTO (시장 가격 변동 정보)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 불필요한 필드 무시
public class BinanceTickerDTO {
    @JsonProperty("e")
    private String eventType; // 이벤트 타입 ("24hrTicker")

    @JsonProperty("E")
    private BigInteger eventTime; // 이벤트 발생 시간 (Unix Timestamp)

    @JsonProperty("s")
    private String symbol; // 거래 심볼 (BTCUSDT 등)

    @JsonProperty("p")
    private BigDecimal priceChange; // 가격 변동

    @JsonProperty("P")
    private BigDecimal priceChangePercent; // 변동률 (%)

    @JsonProperty("w")
    private BigDecimal weightedAvgPrice; // 가중 평균 가격

    @JsonProperty("c")
    private BigDecimal lastPrice; // 마지막 체결 가격

    @JsonProperty("o")
    private BigDecimal openPrice; // 24시간 전 시가

    @JsonProperty("h")
    private BigDecimal highPrice; // 24시간 최고가

    @JsonProperty("l")
    private BigDecimal lowPrice; // 24시간 최저가

    @JsonProperty("v")
    private BigDecimal volume; // 24시간 거래량
}
