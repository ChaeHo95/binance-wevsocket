package com.example.binancewebsocket.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Binance 주문서 항목 데이터 DTO
 */
@Data
public class BinanceOrderBookEntryDTO {

    private String orderType;   // 주문 타입 (BID 또는 ASK)
    private BigDecimal price;   // 가격
    private BigDecimal quantity; // 수량

    // 생성자, getter, setter는 Lombok의 @Data로 자동 생성됨
}
