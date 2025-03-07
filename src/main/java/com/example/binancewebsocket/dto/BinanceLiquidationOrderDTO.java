package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Binance Futures WebSocket 강제 청산 데이터 DTO (Liquidation Order)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 불필요한 필드 무시
public class BinanceLiquidationOrderDTO {

    @JsonProperty("e")
    private String eventType; // ✅ 이벤트 타입 ("forceOrder")

    @JsonProperty("E")
    private BigInteger eventTime; // ✅ 이벤트 발생 시간 (Unix Timestamp)

    @JsonProperty("o")
    private LiquidationData liquidation = new LiquidationData(); // ✅ 내부 객체로 매핑

    @Data
    public static class LiquidationData {
        @JsonProperty("s")
        private String symbol; // ✅ 거래 심볼 (예: BTCUSDT)

        @JsonProperty("S")
        private String side; // ✅ 매매 방향 (BUY / SELL)

        @JsonProperty("o")
        private String orderType; // ✅ 주문 유형 (LIMIT, MARKET 등)

        @JsonProperty("f")
        private String timeInForce; // ✅ 주문 유효성 (IOC 등)

        @JsonProperty("q")
        private BigDecimal originalQuantity; // ✅ 원래 주문량

        @JsonProperty("p")
        private BigDecimal price; // ✅ 주문 가격

        @JsonProperty("ap")
        private BigDecimal averagePrice; // ✅ 체결 평균 가격

        @JsonProperty("X")
        private String orderStatus; // ✅ 주문 상태 (FILLED 등)

        @JsonProperty("l")
        private BigDecimal lastFilledQuantity; // ✅ 마지막 체결된 수량

        @JsonProperty("z")
        private BigDecimal totalFilledQuantity; // ✅ 전체 체결 수량

        @JsonProperty("T")
        private BigInteger tradeTime; // ✅ 거래 체결 시간 (Unix Timestamp)
    }
}
