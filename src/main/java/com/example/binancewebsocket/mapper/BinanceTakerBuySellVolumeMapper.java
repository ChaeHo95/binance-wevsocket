package com.example.binancewebsocket.mapper;

import com.example.binancewebsocket.dto.BinanceTakerBuySellVolumeDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceTakerBuySellVolumeMapper {

    void insertTakerBuySellVolume(BinanceTakerBuySellVolumeDTO volumeDTO);

}
