package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceOpenInterestStatisticsDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BinanceOpenInterestStatisticsMapper {

    void insertOpenInterestStatisticsBatch(List<BinanceOpenInterestStatisticsDto> list);
}
