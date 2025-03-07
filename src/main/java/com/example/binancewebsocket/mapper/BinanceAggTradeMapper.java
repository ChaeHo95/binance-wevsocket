package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceAggTradeDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceAggTradeMapper {

    void insertAggTrade(BinanceAggTradeDTO aggTradeDTO);

    BinanceAggTradeDTO getLatestAggTrade(String symbol);
}
