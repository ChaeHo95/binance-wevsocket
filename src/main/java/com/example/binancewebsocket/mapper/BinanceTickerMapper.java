package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceTickerDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BinanceTickerMapper {

    // ✅ Ticker 데이터 저장
    void insertTicker(BinanceTickerDTO ticker);

    // ✅ 특정 심볼의 최신 Ticker 데이터 가져오기
    BinanceTickerDTO getLatestTicker(@Param("symbol") String symbol);
}
