package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.dto.BinanceOpenInterestStatisticsDto;
import com.example.binancewebsocket.mapper.BinanceOpenInterestStatisticsMapper;
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
public class BinanceOpenInterestStatisticsService {

    private Logger logger = LoggerFactory.getLogger(BinanceOpenInterestStatisticsService.class);
    private final BinanceOpenInterestStatisticsMapper mapper;
    private final WebClient webClient = WebClient.create();
    private BinanceConfig binanceConfig;

    @Autowired
    public BinanceOpenInterestStatisticsService(BinanceConfig binanceConfig,
                                                BinanceOpenInterestStatisticsMapper mapper) {
        this.binanceConfig = binanceConfig;
        this.mapper = mapper;
    }


    public void fetchAndSaveOpenInterestStatistics(String symbol, String period, int limit) {
        try {
            long endTime = Instant.now().toEpochMilli();
            long startTime = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli(); //최근 1시간 데이터 가져옴

            String url = String.format("%s/futures/data/openInterestHist?symbol=%s&period=%s&startTime=%d&endTime=%d&limit=%d",
                    binanceConfig.getBinanceApiUri(), symbol, period, startTime, endTime, limit);

            logger.info("Fetching Open Interest Statistics data from Binance API: {}", url);

            List<BinanceOpenInterestStatisticsDto> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<BinanceOpenInterestStatisticsDto>>() {
                    })
                    .block();

            if (response != null && !response.isEmpty()) {
                response.forEach(mapper::insertOpenInterestStatistics);
                logger.info("Successfully saved {} Open Interest Statistics records", response.size());
            } else {
                logger.warn("No Open Interest Statistics data retrieved from Binance API for {}", symbol);
            }

        } catch (WebClientResponseException e) {
            logger.error("Binance API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error fetching Open Interest Statistics data: {}", e.getMessage());
        }
    }
}
