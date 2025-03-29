package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.BinanceLiquidationOrderDTO;
import com.example.binancewebsocket.mapper.BinanceLiquidationOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceLiquidationOrderService {

    private final Logger logger = LoggerFactory.getLogger(BinanceLiquidationOrderService.class);
    private final BinanceLiquidationOrderMapper binanceLiquidationOrderMapper;

    public BinanceLiquidationOrderService(BinanceLiquidationOrderMapper binanceLiquidationOrderMapper) {
        this.binanceLiquidationOrderMapper = binanceLiquidationOrderMapper;
    }

    /**
     * 📌 강제 청산 주문 데이터 저장
     */
    public void saveLiquidationOrder(BinanceLiquidationOrderDTO liquidationOrderDTO) {
        try {
            binanceLiquidationOrderMapper.insertLiquidationOrder(liquidationOrderDTO);
            logger.debug("✅ 강제 청산 주문 저장 완료");
        } catch (Exception e) {
            logger.error("❌ 강제 청산 주문 저장 실패: {}", e.getMessage());
        }
    }
}
