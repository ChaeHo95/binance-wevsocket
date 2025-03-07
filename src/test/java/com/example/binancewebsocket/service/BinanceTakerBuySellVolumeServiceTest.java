package com.example.binancewebsocket.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest()
class BinanceTakerBuySellVolumeServiceTest {

    @Autowired
    BinanceTakerBuySellVolumeService binanceTakerBuySellVolumeService;

    @Test
    void fetchAndSaveTakerBuySellVolume() {
        binanceTakerBuySellVolumeService.fetchAndSaveTakerBuySellVolume("XRPUSDT", "5m", 30);

    }
}
