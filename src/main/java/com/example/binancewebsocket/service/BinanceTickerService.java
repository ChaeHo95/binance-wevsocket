package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.BinanceTickerDTO;
import com.example.binancewebsocket.mapper.BinanceTickerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceTickerService {

    private Logger logger = LoggerFactory.getLogger(BinanceTickerService.class);
    private final BinanceTickerMapper binanceTickerMapper;

    public BinanceTickerService(BinanceTickerMapper binanceTickerMapper) {
        this.binanceTickerMapper = binanceTickerMapper;
    }

    /**
     * 📌 Ticker 데이터 저장
     */
    public void saveTicker(BinanceTickerDTO tickerDTO) {
        try {
            binanceTickerMapper.insertTicker(tickerDTO);
            logger.debug("✅ Ticker 데이터 저장 완료");
        } catch (Exception e) {
            logger.error("❌ Ticker 데이터 저장 실패: {}", e.getMessage());
        }
    }
}
