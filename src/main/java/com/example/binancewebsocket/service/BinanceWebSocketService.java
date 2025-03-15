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
            logger.info("Binance WebSocket 초기화 시작.");

            // symbols 리스트가 null이거나 비어 있으면 DB에서 조회
            if (this.symbols == null || this.symbols.isEmpty()) {
                logger.info("symbols 리스트가 null이거나 비어 있습니다. DB에서 symbols를 조회합니다.");
                List<String> newSymbols = symbolMapper.selectAllSymbols();
                this.symbols = newSymbols;
                logger.info("DB에서 조회한 symbols: {}", newSymbols);
            } else {
                logger.info("기존 symbols 값 사용: {}", symbols);
            }

            // 모든 심볼을 소문자로 변환하여 markets 리스트 구성
            List<String> markets = symbols.stream()
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .toList();

            // BinanceConfig를 이용해 동적으로 WebSocket URL 생성
            String webSocketUrl = binanceConfig.getFuturesWebSocketUrl(markets);
            logger.info("생성된 WebSocket URL: {}", webSocketUrl);

            // WebSocket 클라이언트 생성 및 연결
            URI webSocketUri = new URI(webSocketUrl);
            webSocketClient = new BinanceWebSocketClient(
                    webSocketUri, binanceKlineService, binanceTickerService,
                    binanceTradeService, binanceFundingRateService, binanceAggTradeService,
                    liquidationOrderService, partialBookDepthService
            );
            webSocketClient.connect();

            logger.info("✅ Binance WebSocket 연결 성공! 사용 symbols: {}", symbols);
        } catch (Exception e) {
            logger.error("❌ WebSocket 초기화 실패: ", e);
            throw new RuntimeException("❌ WebSocket 초기화 실패: ", e);
        }
    }

    /**
     * 매일 자정(00시)에 DB에서 symbols 값을 업데이트하고 WebSocket을 재연결합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateSymbols() {
        try {
            logger.info("Symbols 업데이트 작업 시작.");

            // 새 symbols 값 조회
            List<String> newSymbols = symbolMapper.selectAllSymbols();
            logger.info("DB에서 새로 조회한 symbols: {}", newSymbols);
            this.symbols = newSymbols;

            // 기존 WebSocket 연결 상태 확인 및 종료 처리
            if (webSocketClient != null && webSocketClient.isWebSocketOpen()) {
                logger.info("기존 WebSocket 연결이 열려 있습니다. 재연결을 위해 기존 연결을 종료합니다.");
                webSocketClient.close();
            }

            // 새 심볼 값으로 markets 구성
            List<String> markets = symbols.stream()
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .toList();

            // 동적으로 WebSocket URL 생성
            String webSocketUrl = binanceConfig.getFuturesWebSocketUrl(markets);
            logger.info("업데이트된 WebSocket URL: {}", webSocketUrl);

            // 새 WebSocket 클라이언트 생성 및 연결
            URI webSocketUri = new URI(webSocketUrl);
            webSocketClient = new BinanceWebSocketClient(
                    webSocketUri, binanceKlineService, binanceTickerService,
                    binanceTradeService, binanceFundingRateService, binanceAggTradeService,
                    liquidationOrderService, partialBookDepthService
            );
            webSocketClient.connect();

            logger.info("✅ Symbols 업데이트 및 WebSocket 재연결 완료: {}", newSymbols);
        } catch (Exception e) {
            logger.error("Symbols 업데이트 실패: ", e);
        }
    }
}
