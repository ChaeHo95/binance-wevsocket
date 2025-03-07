package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceFundingRateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BinanceFundingRateMapper {

    /**
     * ✅ Funding Rate 데이터 저장
     * 중복 데이터가 존재할 경우, 최신 데이터로 업데이트
     *
     * @param fundingRateDTO 저장할 Funding Rate 데이터 객체
     */
    void insertFundingRate(BinanceFundingRateDTO fundingRateDTO);

    /**
     * ✅ 특정 심볼의 최신 Funding Rate 조회
     *
     * @param symbol 거래 심볼 (예: BTCUSDT)
     * @return 해당 심볼의 가장 최근 Funding Rate 데이터
     */
    BinanceFundingRateDTO getLatestFundingRate(@Param("symbol") String symbol);

}
