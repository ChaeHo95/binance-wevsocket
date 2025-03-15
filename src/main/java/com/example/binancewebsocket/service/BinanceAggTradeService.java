package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.BinanceAggTradeDTO;
import com.example.binancewebsocket.mapper.BinanceAggTradeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceAggTradeService {

    private final BinanceAggTradeMapper aggTradeMapper;
    private Logger logger = LoggerFactory.getLogger(BinanceAggTradeService.class);

    public BinanceAggTradeService(BinanceAggTradeMapper aggTradeMapper) {
        this.aggTradeMapper = aggTradeMapper;
    }

    /**
     * ✅ Aggregate Trade 저장
     */
    public void saveAggTrade(BinanceAggTradeDTO aggTradeDTO) {
        try {
            aggTradeMapper.insertAggTrade(aggTradeDTO);
            logger.debug("📊 Aggregate Trade 저장됨: {}", aggTradeDTO);
        } catch (Exception e) {
            logger.error("❌ Aggregate Trade 저장 오류: ", e);
        }
    }

    /**
     * ✅ 최신 Aggregate Trade 조회
     */
}
