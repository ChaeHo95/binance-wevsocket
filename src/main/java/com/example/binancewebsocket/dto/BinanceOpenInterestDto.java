package com.example.binancewebsocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BinanceOpenInterestDto {
    private String symbol;
    private BigDecimal openInterest;
    private BigInteger time;
}
