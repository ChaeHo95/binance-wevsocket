package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.BinanceKlineDTO;
import com.example.binancewebsocket.mapper.BinanceKlineMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BinanceKlineService {

    private Logger logger = LoggerFactory.getLogger(BinanceKlineService.class);
    private final BinanceKlineMapper binanceKlineMapper;

    /**
     * 📌 캔들 데이터 저장
     */
    public void saveKline5m(BinanceKlineDTO klineDTO) {
        try {
            binanceKlineMapper.insertKline5m(klineDTO);
            logger.debug("✅ Kline 5m 데이터 저장 완료: {}", klineDTO);
        } catch (Exception e) {
            logger.error("❌ Kline 5m 데이터 저장 실패: {}", e.getMessage());
        }
    }

    public void saveKline1h(BinanceKlineDTO klineDTO) {
        try {
            binanceKlineMapper.insertKline1h(klineDTO);
            logger.debug("✅ Kline 1h 데이터 저장 완료: {}", klineDTO);
        } catch (Exception e) {
            logger.error("❌ Kline 1h 데이터 저장 실패: {}", e.getMessage());
        }
    }
}
