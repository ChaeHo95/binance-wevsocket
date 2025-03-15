package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.mapper.SymbolMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Service
public class BinanceWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketService.class);

    @Autowired
    private BinanceConfig binanceConfig;
    @Autowired
    private BinanceKlineService binanceKlineService;
    @Autowired
    private BinanceTickerService binanceTickerService;
    @Autowired
    private BinanceTradeService binanceTradeService;
    @Autowired
    private BinanceAggTradeService binanceAggTradeService;
    @Autowired
    private BinanceFundingRateService binanceFundingRateService;
    @Autowired
    private BinanceLiquidationOrderService liquidationOrderService;
    @Autowired
    private BinancePartialBookDepthService partialBookDepthService;

    @Autowired
    private SymbolMapper symbolMapper;

    private BinanceWebSocketClient webSocketClient;

    private List<String> symbols;

    @Value("${enable.binance.websocket:false}") // 기본값 false
    private boolean enableWebSocket;


    @PostConstruct
    public void initWebSocket() {
        if (!enableWebSocket) {
            logger.info("⚠ WebSocket 실행이 비활성화됨.");
            return; // 실행하지 않음
        }

        try {

            if (symbols.isEmpty()) {
                List<String> newSymbols = symbolMapper.selectAllSymbols();
                this.symbols = newSymbols;
            }

            // ✅ 구독할 심볼 리스트 설정
            List<String> markets = symbols.stream()
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .toList(); // 필요한 심볼 추가 가능

            // ✅ 동적으로 WebSocket URL 생성
            String webSocketUrl = binanceConfig.getFuturesWebSocketUrl(markets);

            // ✅ WebSocket 클라이언트 생성 및 연결
            URI webSocketUri = new URI(webSocketUrl);
            webSocketClient = new BinanceWebSocketClient(
                    webSocketUri, binanceKlineService, binanceTickerService,
                    binanceTradeService, binanceFundingRateService, binanceAggTradeService,
                    liquidationOrderService, partialBookDepthService
            );
            webSocketClient.connect();

            logger.info("✅ Binance WebSocket 연결 성공!");
        } catch (Exception e) {
            throw new RuntimeException("❌ WebSocket 초기화 실패: ", e);
        }
    }

    /**
     * 매일 자정(00시)에 DB에서 symbols 값을 업데이트합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateSymbols() {
        try {

            if (webSocketClient.isWebSocketOpen()) {
                webSocketClient.isWebSocketClosed();
            }

            List<String> newSymbols = symbolMapper.selectAllSymbols();
            this.symbols = newSymbols;
            
            List<String> markets = symbols.stream()
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .toList(); // 필요한 심볼 추가 가능

            // ✅ 동적으로 WebSocket URL 생성
            String webSocketUrl = binanceConfig.getFuturesWebSocketUrl(markets);

            // ✅ WebSocket 클라이언트 생성 및 연결
            URI webSocketUri = new URI(webSocketUrl);
            webSocketClient = new BinanceWebSocketClient(
                    webSocketUri, binanceKlineService, binanceTickerService,
                    binanceTradeService, binanceFundingRateService, binanceAggTradeService,
                    liquidationOrderService, partialBookDepthService
            );
            webSocketClient.connect();

            logger.info("Symbols updated from DB: {}", newSymbols);
            // 필요 시, WebSocket 연결 재설정 로직도 추가 가능
        } catch (Exception e) {
            logger.error("Symbols 업데이트 실패: ", e);
        }
    }
}
