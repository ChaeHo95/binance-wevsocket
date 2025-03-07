package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.dto.BinanceLongShortRatioDTO;
import com.example.binancewebsocket.mapper.BinanceLongShortRatioMapper;
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
public class BinanceLongShortRatioService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceLongShortRatioService.class);
    private final BinanceLongShortRatioMapper mapper;
    private final WebClient webClient = WebClient.create();
    private BinanceConfig binanceConfig;

    @Autowired
    public BinanceLongShortRatioService(BinanceConfig binanceConfig,
                                        BinanceLongShortRatioMapper mapper) {
        this.binanceConfig = binanceConfig;
        this.mapper = mapper;
    }

    public void fetchAndSaveLongShortRatio(String symbol, String period, int limit) {
        try {
            long endTime = Instant.now().toEpochMilli();
            long startTime = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();

            String url = String.format("%s/futures/data/globalLongShortAccountRatio?symbol=%s&period=%s&startTime=%d&endTime=%d&limit=%d",
                    binanceConfig.getBinanceApiUri(), symbol, period, startTime, endTime, limit);

            logger.info("Fetching Long/Short Ratio data from Binance API: {}", url);

            List<BinanceLongShortRatioDTO> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<BinanceLongShortRatioDTO>>() {
                    })  // 리스트 변환
                    .block();

            if (response != null && !response.isEmpty()) {
                response.forEach(mapper::insertLongShortRatio);
                logger.info("Successfully saved {} Long/Short Ratio records for {}", response.size(), symbol);
            } else {
                logger.warn("No Long/Short Ratio data retrieved from Binance API for {}", symbol);
            }

        } catch (WebClientResponseException e) {
            logger.error("Binance API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error fetching Long/Short Ratio data: {}", e.getMessage());
        }
    }
}
