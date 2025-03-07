package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinancePartialBookDepthDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface BinancePartialBookDepthMapper {

    // ✅ Partial Book Depth 데이터 저장
    void insertPartialBookDepth(BinancePartialBookDepthDTO bookDepthDTO);

    // ✅ 호가 데이터(Bid & Ask) 저장
    void insertOrderBookEntries(@Param("transactionTime") BigInteger transactionTime,
                                @Param("bids") List<BinancePartialBookDepthDTO.OrderBookEntry> bids,
                                @Param("asks") List<BinancePartialBookDepthDTO.OrderBookEntry> asks);


}
