package com.example.binancewebsocket.dto;

import com.example.binancewebsocket.utils.OrderBookEntryDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Binance Futures WebSocket 호가 데이터 DTO (Partial Book Depth)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 불필요한 필드 무시
public class BinancePartialBookDepthDTO {

    @JsonProperty("e")
    private String eventType; // 이벤트 타입 ("depthUpdate")

    @JsonProperty("E")
    private BigInteger eventTime; // 이벤트 발생 시간 (Unix Timestamp)

    @JsonProperty("T")
    private BigInteger transactionTime; // 트랜잭션 시간 (Unix Timestamp)

    @JsonProperty("s")
    private String symbol; // 거래 심볼 (BTCUSDT 등)

    @JsonProperty("U")
    private BigInteger firstUpdateId; // 이벤트 내 첫 번째 업데이트 ID

    @JsonProperty("u")
    private BigInteger finalUpdateId; // 이벤트 내 마지막 업데이트 ID

    @JsonProperty("pu")
    private BigInteger previousUpdateId; // 이전 이벤트의 마지막 업데이트 ID

    @JsonProperty("b")
    @JsonDeserialize(using = OrderBookEntryDeserializer.class) // ✅ 커스텀 역직렬화 적용
    private List<OrderBookEntry> bids; // 매수 주문 목록

    @JsonProperty("a")
    @JsonDeserialize(using = OrderBookEntryDeserializer.class) // ✅ 커스텀 역직렬화 적용
    private List<OrderBookEntry> asks; // 매도 주문 목록

    @Data
    public static class OrderBookEntry {
        private BigDecimal price; // 매수/매도 가격
        private BigDecimal quantity; // 매수/매도 수량

        public OrderBookEntry(BigDecimal price, BigDecimal quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }
}
