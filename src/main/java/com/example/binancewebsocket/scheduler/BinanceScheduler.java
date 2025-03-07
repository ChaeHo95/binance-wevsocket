package com.example.binancewebsocket.scheduler;

import com.example.binancewebsocket.service.BinanceLongShortRatioService;
import com.example.binancewebsocket.service.BinanceTakerBuySellVolumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "enable.binance.scheduling", havingValue = "true", matchIfMissing = false)
public class BinanceScheduler {
    private Logger logger = LoggerFactory.getLogger(BinanceScheduler.class);


    @Autowired
    private BinanceLongShortRatioService binanceLongShortRatioService;

    @Autowired
    BinanceTakerBuySellVolumeService binanceTakerBuySellVolumeService;

    /**
     * ✅ WebSocket 상태 체크 및 자동 재연결 (1분마다 실행)
     */
    @Scheduled(fixedRate = 25 * 60 * 1000)
    public void getTakerBuySellVolume() {
        logger.info("⚠️ Taker Buy/Sell Volume API 호출");
        binanceTakerBuySellVolumeService.fetchAndSaveTakerBuySellVolume("XRPUSDT", "5m", 30);
    }

    @Scheduled(fixedRate = 25 * 60 * 1000)
    public void getLongShortRatio() {
        logger.info("⚠️ Long/Short Ratio API 호출 호출");
        binanceLongShortRatioService.fetchAndSaveLongShortRatio("XRPUSDT", "5m", 30);
    }
}
