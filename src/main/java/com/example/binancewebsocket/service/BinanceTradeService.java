package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.BinanceTradeDTO;
import com.example.binancewebsocket.mapper.BinanceTradeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceTradeService {

    private Logger logger = LoggerFactory.getLogger(BinanceTradeService.class);
    private final BinanceTradeMapper binanceTradeMapper;

    public BinanceTradeService(BinanceTradeMapper binanceTradeMapper) {
        this.binanceTradeMapper = binanceTradeMapper;
    }

    /**
     * 📌 거래 데이터 저장
     */
    public void saveTrade(BinanceTradeDTO tradeDTO) {
        try {
            binanceTradeMapper.insertTrade(tradeDTO);
            logger.debug("✅ Trade 데이터 저장 완료: {}", tradeDTO);
        } catch (Exception e) {
            logger.error("❌ Trade 데이터 저장 실패: {}", e.getMessage());
        }
    }
}
