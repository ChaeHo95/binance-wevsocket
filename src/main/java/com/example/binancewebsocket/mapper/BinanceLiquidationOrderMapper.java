package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceLiquidationOrderDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceLiquidationOrderMapper {
    /**
     * 📌 강제 청산 주문 데이터 저장
     */
    void insertLiquidationOrder(BinanceLiquidationOrderDTO liquidationOrderDTO);
}
