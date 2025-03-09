package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceOpenInterestStatisticsDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceOpenInterestStatisticsMapper {

    void insertOpenInterestStatistics(BinanceOpenInterestStatisticsDto statisticsDto);
}
