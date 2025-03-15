package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Binance Futures WebSocket 캔들 데이터 DTO (Kline/Candlestick)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 불필요한 필드 무시
public class BinanceKlineDTO {

    @JsonProperty("e")
    private String eventType; // 이벤트 타입 ("kline")

    @JsonProperty("E")
    private BigInteger eventTime; // 이벤트 발생 시간 (Unix Timestamp)

    @JsonProperty("s")
    private String symbol; // 거래 심볼 (BTCUSDT 등)

    @JsonProperty("k")
    private KlineData kline = new KlineData();
    ; // ✅ 내부 객체로 매핑

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KlineData {
        @JsonProperty("t")
        private BigInteger openTime; // 캔들 시작 시간

        @JsonProperty("T")
        private BigInteger closeTime; // 캔들 종료 시간

        @JsonProperty("i")
        private String interval; // 캔들 간격 (1m, 5m 등)

        @JsonProperty("o")
        private BigDecimal openPrice; // 시가

        @JsonProperty("c")
        private BigDecimal closePrice; // 종가

        @JsonProperty("h")
        private BigDecimal highPrice; // 고가

        @JsonProperty("l")
        private BigDecimal lowPrice; // 저가

        @JsonProperty("v")
        private BigDecimal volume; // 거래량

        @JsonProperty("n")
        private BigInteger tradeCount; // 거래 횟수

        @JsonProperty("x")
        private Boolean isKlineClosed; // 캔들 종료 여부

    }

    // ✅ MyBatis용 Getter 추가

    public void setCloseTime(BigInteger closeTime) {
        this.kline.closeTime = closeTime;
    }

    public BigInteger getCloseTime() {
        return this.kline.closeTime;
    }

    public void setOpenTime(BigInteger openTime) {
        this.kline.openTime = openTime;
    }

    public BigInteger getOpenTime() {
        return this.kline.openTime;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.kline.openPrice = openPrice;
    }

    public BigDecimal getOpenPrice() {
        return this.kline.openPrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.kline.closePrice = closePrice;
    }

    public BigDecimal getClosePrice() {
        return this.kline.closePrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.kline.highPrice = highPrice;
    }

    public BigDecimal getHighPrice() {
        return this.kline.highPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.kline.lowPrice = lowPrice;
    }

    public BigDecimal getLowPrice() {
        return this.kline.lowPrice;
    }

    public void setVolume(BigDecimal volume) {
        this.kline.volume = volume;
    }

    public BigDecimal getVolume() {
        return this.kline.volume;
    }

    public void setTradeCount(BigInteger tradeCount) {
        this.kline.tradeCount = tradeCount;
    }

    public BigInteger getTradeCount() {
        return this.kline.tradeCount;
    }


    public void setIsKlineClosed(Boolean isKlineClosed) {
        this.kline.isKlineClosed = isKlineClosed;
    }

    public Boolean getIsKlineClosed() {
        return this.kline.isKlineClosed;
    }
}
