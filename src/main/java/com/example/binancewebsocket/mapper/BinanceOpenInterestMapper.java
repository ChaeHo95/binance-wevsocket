package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceOpenInterestDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceOpenInterestMapper {

    void insertOpenInterest(BinanceOpenInterestDto openInterestDto);
}
