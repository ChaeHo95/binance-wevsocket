package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Binance Futures WebSocket Aggregate Trade DTO (AggTrade)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceAggTradeDTO {

    @JsonProperty("e")
    private String eventType; // 이벤트 타입 ("aggTrade")

    @JsonProperty("E")
    private BigInteger eventTime; // 이벤트 발생 시간 (Unix Timestamp)

    @JsonProperty("s")
    private String symbol; // 거래 심볼 (BTCUSDT 등)

    @JsonProperty("a")
    private BigInteger aggTradeId; // Aggregate Trade ID

    @JsonProperty("p")
    private BigDecimal price; // 체결 가격

    @JsonProperty("q")
    private BigDecimal quantity; // 체결 수량

    @JsonProperty("f")
    private BigInteger firstTradeId = BigInteger.ZERO;
    ; // 첫 번째 개별 거래 ID

    @JsonProperty("l")
    private BigInteger lastTradeId = BigInteger.ZERO;
    ; // 마지막 개별 거래 ID

    @JsonProperty("T")
    private BigInteger tradeTime; // 거래 발생 시간 (Unix Timestamp)

    @JsonProperty("m")
    private Boolean buyerMaker; // 매수자가 메이커인지 여부 (true: 메이커, false: 테이커)
}
