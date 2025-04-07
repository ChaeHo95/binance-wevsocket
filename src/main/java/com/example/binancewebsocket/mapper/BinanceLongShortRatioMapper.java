package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceLongShortRatioDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BinanceLongShortRatioMapper {

    void insertLongShortRatioBatch(List<BinanceLongShortRatioDTO> list);


}
