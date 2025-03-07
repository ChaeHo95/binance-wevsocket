package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceKlineDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceKlineMapper {

    // ✅ Kline 데이터 저장
    void insertKline(BinanceKlineDTO kline);

}
