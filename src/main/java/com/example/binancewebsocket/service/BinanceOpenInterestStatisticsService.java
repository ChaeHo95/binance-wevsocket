package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.dto.BinanceOpenInterestStatisticsDto;
import com.example.binancewebsocket.mapper.BinanceOpenInterestStatisticsMapper;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BinanceOpenInterestStatisticsService {

    private Logger logger = LoggerFactory.getLogger(BinanceOpenInterestStatisticsService.class);
    private BinanceOpenInterestStatisticsMapper mapper;
    private WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                            .responseTimeout(Duration.ofSeconds(10))
            ))
            .build();
    private BinanceConfig binanceConfig;

    @Autowired
    public BinanceOpenInterestStatisticsService(BinanceConfig binanceConfig,
                                                BinanceOpenInterestStatisticsMapper mapper) {
        this.binanceConfig = binanceConfig;
        this.mapper = mapper;
    }

    // 트랜잭션 적용
    @Transactional
    // 데드락 발생 시 최대 3번, 10초 간격으로 재시도
    @Retryable(retryFor = DeadlockLoserDataAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 10000))
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
                mapper.insertOpenInterestStatisticsBatch(response);
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
