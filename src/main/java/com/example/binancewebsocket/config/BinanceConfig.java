package com.example.binancewebsocket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class BinanceConfig {

    private Logger logger = LoggerFactory.getLogger(BinanceConfig.class);

    private EnvConfig envConfig;

    @Autowired
    public BinanceConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    /**
     * ✅ Binance Futures WebSocket 기본 URL 설정
     */
    @Bean
    public URI binanceWebSocketUri() {
        String wsUrl = envConfig.getBinanceWsUrl();

        try {
            URI uri = new URI(wsUrl);
            logger.info("✅ BINANCE_WS_URL 로드 성공: {}", uri);
            return uri;
        } catch (URISyntaxException e) {
            logger.error("❌ 잘못된 WebSocket URL 형식: {}", wsUrl);
            throw new RuntimeException("올바른 Binance WebSocket URL을 설정해주세요.", e);
        }
    }

    /**
     * ✅ Binance Futures API 기본 URL 설정
     */
    public String getBinanceApiUri() {
        String url = envConfig.getBinanceBaseUrl();

        try {
            new URI(url);
            logger.info("✅ BINANCE_BASE_URL 로드 성공: {}", url);
            return url;
        } catch (URISyntaxException e) {
            logger.error("❌ BINANCE_BASE_URL 호출 실패: {}", url);
            throw new RuntimeException("올바른 Binance API URL을 설정해주세요.", e);
        }
    }

    /**
     * ✅ 구독할 심볼을 바탕으로 Futures WebSocket URL 생성
     *
     * @param markets 구독할 심볼 리스트 (예: ["btcusdt", "ethusdt"])
     * @return WebSocket URL
     */
    public String getFuturesWebSocketUrl(List<String> markets) {
        String baseUrl = envConfig.getBinanceWsUrl();

        if (markets == null || markets.isEmpty()) {
            logger.warn("⚠️ 구독할 심볼이 없습니다. 기본값으로 'btcusdt'를 사용합니다.");
            markets = List.of("btcusdt"); // 기본 심볼 설정
        }

        // Binance Futures의 스트림 형식에 맞게 변환
        String streams = markets.stream()
                .flatMap(market -> List.of(
                        market + "@trade",     // ✅ 선물 개별 거래 정보
                        market + "@aggTrade",      // ✅ 선물 집계 거래 정보
                        market + "@markPrice",     // ✅ 선물 시장 가격 (펀딩비 포함)
                        market + "@kline_1m",      // ✅ 1분봉 캔들 데이터
                        market + "@ticker",        // ✅ 24시간 티커 데이터
                        market + "@forceOrder",    // ✅ 강제 청산 정보
                        market + "@depth10@100ms"   // ✅ PARTIAL BOOK DEPTH (20개 레벨, 100ms 간격)
                ).stream())
                .collect(Collectors.joining("/"));

        String fullUrl = baseUrl + "/stream?streams=" + streams;
        logger.info("✅ Futures WebSocket URL 생성됨: {}", fullUrl);
        return fullUrl;
    }
}
