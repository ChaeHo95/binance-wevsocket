package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.dto.BinanceOpenInterestDto;
import com.example.binancewebsocket.mapper.BinanceOpenInterestMapper;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Service
public class BinanceOpenInterestService {

    private Logger logger = LoggerFactory.getLogger(BinanceOpenInterestService.class);
    private BinanceOpenInterestMapper mapper;
    private WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                            .responseTimeout(Duration.ofSeconds(10))
            ))
            .build();
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
