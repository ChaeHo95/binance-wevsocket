package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceTakerBuySellVolumeDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BinanceTakerBuySellVolumeMapper {

    void insertTakerBuySellVolumeBatch(List<BinanceTakerBuySellVolumeDTO> list);
}
