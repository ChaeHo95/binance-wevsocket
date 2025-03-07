package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceLongShortRatioDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceLongShortRatioMapper {

    void insertLongShortRatio(BinanceLongShortRatioDTO ratioDTO);


}
