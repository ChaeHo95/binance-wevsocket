package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.dto.BinanceOpenInterestDto;
import com.example.binancewebsocket.mapper.BinanceOpenInterestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class BinanceOpenInterestService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceOpenInterestService.class);
    private final BinanceOpenInterestMapper mapper;
    private final WebClient webClient = WebClient.create();
    private BinanceConfig binanceConfig;

    @Autowired
    public BinanceOpenInterestService(BinanceConfig binanceConfig,
                                      BinanceOpenInterestMapper mapper) {
        this.binanceConfig = binanceConfig;
        this.mapper = mapper;
    }

    public void fetchAndSaveOpenInterest(String symbol) {
        try {
            String url = String.format("%s/fapi/v1/openInterest?symbol=%s",
                    binanceConfig.getBinanceApiUri(), symbol);

            logger.info("Fetching Open Interest data from Binance API: {}", url);

            BinanceOpenInterestDto response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(BinanceOpenInterestDto.class)
                    .block();

            if (response != null) {
                mapper.insertOpenInterest(response);
                logger.info("Successfully saved Open Interest data for symbol: {}", symbol);
            } else {
                logger.warn("No Open Interest data retrieved from Binance API for {}", symbol);
            }

        } catch (WebClientResponseException e) {
            logger.error("Binance API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error fetching Open Interest data: {}", e.getMessage());
        }
    }
}
