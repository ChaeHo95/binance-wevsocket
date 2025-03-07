package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.dto.BinanceTakerBuySellVolumeDTO;
import com.example.binancewebsocket.mapper.BinanceTakerBuySellVolumeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BinanceTakerBuySellVolumeService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceTakerBuySellVolumeService.class);
    private final BinanceTakerBuySellVolumeMapper mapper;
    private final WebClient webClient = WebClient.create();
    private BinanceConfig binanceConfig;

    @Autowired
    public BinanceTakerBuySellVolumeService(BinanceConfig binanceConfig,
                                            BinanceTakerBuySellVolumeMapper mapper) {
        this.binanceConfig = binanceConfig;
        this.mapper = mapper;
    }

    public void fetchAndSaveTakerBuySellVolume(String symbol, String period, int limit) {
        try {
            long endTime = Instant.now().toEpochMilli();
            long startTime = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();

            String url = String.format("%s/futures/data/takerlongshortRatio?symbol=%s&period=%s&startTime=%d&endTime=%d&limit=%d",
                    binanceConfig.getBinanceApiUri(), symbol, period, startTime, endTime, limit);

            logger.info("Fetching Taker Buy/Sell Volume data from Binance API: {}", url);

            List<BinanceTakerBuySellVolumeDTO> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<BinanceTakerBuySellVolumeDTO>>() {
                    })  // 리스트 변환
                    .block();
            for (BinanceTakerBuySellVolumeDTO dto : response) {
                dto.setSymbol(symbol);
            }
            if (response != null && !response.isEmpty()) {
                response.forEach(mapper::insertTakerBuySellVolume);
                logger.info("Successfully saved {} Taker Buy/Sell Volume records", response.size());
            } else {
                logger.warn("No Taker Buy/Sell Volume data retrieved from Binance API for {}", symbol);
            }

        } catch (WebClientResponseException e) {
            logger.error("Binance API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error fetching Taker Buy/Sell Volume data: {}", e.getMessage());
        }
    }
}
