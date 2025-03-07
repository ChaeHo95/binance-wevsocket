package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Binance Futures WebSocket 거래 데이터 DTO (실시간 체결 정보)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 불필요한 필드 무시
public class BinanceTradeDTO {
    @JsonProperty("e")
    private String eventType; // 이벤트 타입 ("trade")

    @JsonProperty("E")
    private BigInteger eventTime; // 이벤트 발생 시간 (Unix Timestamp)

    @JsonProperty("s")
    private String symbol; // 거래 심볼 (BTCUSDT 등)

    @JsonProperty("t")
    private BigInteger tradeId; // 개별 거래 ID

    @JsonProperty("p")
    private BigDecimal price; // 체결 가격

    @JsonProperty("q")
    private BigDecimal quantity; // 체결 수량

    @JsonProperty("T")
    private BigInteger tradeTime; // 거래 체결 시간 (Unix Timestamp)

    @JsonProperty("m")
    private Boolean buyerMaker; // 매수자가 메이커인지 여부
}
