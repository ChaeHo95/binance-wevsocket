package com.example.binancewebsocket.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BinanceLongShortRatioServiceTest {

    @Autowired
    private BinanceLongShortRatioService binanceLongShortRatioService;

    @Test
    void fetchAndSaveLongShortRatio() {
        binanceLongShortRatioService.fetchAndSaveLongShortRatio("XRPUSDT", "5m", 30);
    }
}
