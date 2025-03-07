package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.BinanceFundingRateDTO;
import com.example.binancewebsocket.mapper.BinanceFundingRateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceFundingRateService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceFundingRateService.class);
    private final BinanceFundingRateMapper fundingRateMapper;

    public BinanceFundingRateService(BinanceFundingRateMapper fundingRateMapper) {
        this.fundingRateMapper = fundingRateMapper;
    }

    /**
     * ✅ 펀딩 비율 데이터 저장
     */
    public void saveFundingRate(BinanceFundingRateDTO fundingRateDTO) {
        try {
            fundingRateMapper.insertFundingRate(fundingRateDTO);
            logger.debug("📊 펀딩 비율 저장됨: {}", fundingRateDTO);
        } catch (Exception e) {
            logger.error("❌ 펀딩 비율 저장 오류: ", e);
        }
    }
}
