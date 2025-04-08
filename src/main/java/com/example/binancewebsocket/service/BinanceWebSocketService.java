package com.example.binancewebsocket.service;

import com.example.binancewebsocket.config.BinanceConfig;
import com.example.binancewebsocket.mapper.SymbolMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class BinanceWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketService.class);

    // --- 의존성 주입 ---
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

    // --- 설정값 ---
    @Value("${enable.binance.websocket:false}") // application.properties 등에서 설정, 기본값 false
    private boolean enableWebSocket;

    // --- 내부 상태 ---
    private volatile BinanceWebSocketClient webSocketClient; // volatile 추가로 가시성 확보
    private List<String> symbols; // 조회된 심볼 리스트 캐싱

    /**
     * 서비스 초기화 시 WebSocket 연결 시도
     */
    @PostConstruct
    public void initWebSocket() {
        if (!enableWebSocket) {
            logger.info("⚠ WebSocket 실행이 비활성화됨 (enable.binance.websocket=false).");
            return;
        }

        logger.info("Binance WebSocket 초기화 시작...");
        try {
            loadSymbolsFromDB(); // 심볼 로딩 로직 분리
            connectWebSocket();  // WebSocket 연결 로직 분리
            logger.info("✅ Binance WebSocket 초기화 및 연결 성공! 사용 symbols: {}", symbols.size());
        } catch (Exception e) {
            // 초기화 실패 시 RuntimeException 발생시켜 애플리케이션 컨텍스트 로딩 중단
            // 만약 앱 실행을 계속해야 한다면, RuntimeException 대신 에러 로그만 남기도록 수정 필요
            logger.error("❌ WebSocket 초기화 실패. 애플리케이션 시작에 문제가 발생할 수 있습니다.", e);
            throw new RuntimeException("❌ WebSocket 초기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 애플리케이션 종료 시 WebSocket 클라이언트 리소스 정리
     */
    @PreDestroy
    public void shutdownWebSocket() {
        logger.info("BinanceWebSocketService 종료 시작...");
        if (webSocketClient != null) {
            logger.info("WebSocket 클라이언트 리소스 정리 시도...");
            try {
                // BinanceWebSocketClient에 destroy() 메소드가 구현되어 있다고 가정
                webSocketClient.destroy();
                logger.info("WebSocket 클라이언트 리소스 정리 완료.");
            } catch (Exception e) {
                logger.error("❌ WebSocket 클라이언트 리소스 정리 중 오류 발생", e);
            }
        }
        logger.info("BinanceWebSocketService 종료 완료.");
    }

    /**
     * 매일 자정(00시)에 DB에서 symbols 값을 업데이트하고 WebSocket을 재연결.
     * 동적 구독/구독 해지가 가능하다면 이 방식보다 효율적임. (API 확인 필요)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateSymbolsAndReconnect() {
        if (!enableWebSocket) {
            logger.info("⚠ WebSocket이 비활성화되어 Symbols 업데이트 및 재연결 작업을 건너<0xEB><0x8A>니다.");
            return;
        }

        logger.info("Symbols 업데이트 및 WebSocket 재연결 작업 시작...");

        BinanceWebSocketClient oldClient = this.webSocketClient; // 이전 클라이언트 참조 저장

        try {
            // 1. 새 심볼 목록 로드
            loadSymbolsFromDB();

            // 2. 새 WebSocket 클라이언트 연결 (기존 연결과 교체)
            //    (주의: connect()는 비동기일 수 있으므로, 즉시 연결 완료를 보장하지 않음)
            connectWebSocket();
            logger.info("✅ Symbols 업데이트 및 새 WebSocket 클라이언트 연결 시도 완료. 사용 symbols: {}", symbols.size());

            // 3. 이전 WebSocket 클라이언트 리소스 정리
            //    (새 클라이언트 연결 시도 *후에* 이전 클라이언트 정리)
            if (oldClient != null) {
                logger.info("이전 WebSocket 클라이언트 인스턴스 리소스 정리 시작.");
                try {
                    // BinanceWebSocketClient에 destroy() 메소드가 구현되어 있다고 가정
                    oldClient.destroy();
                    logger.info("이전 WebSocket 클라이언트 인스턴스 리소스 정리 완료.");
                } catch (Exception destroyEx) {
                    logger.error("❌ 이전 WebSocket 클라이언트 리소스 정리 중 오류 발생", destroyEx);
                    // 로깅 후 계속 진행
                }
            }
        } catch (Exception e) {
            logger.error("❌ Symbols 업데이트 및 WebSocket 재연결 작업 중 오류 발생", e);
            // 심각한 오류 시, 현재 webSocketClient 참조를 이전 상태(oldClient)로 복원하는 로직 고려 가능
            // 또는, 실패 알림 등의 추가 조치 고려
        }
    }

    /**
     * DB에서 최신 심볼 목록을 로드하여 내부 'symbols' 필드를 업데이트합니다.
     */
    private void loadSymbolsFromDB() {
        logger.info("DB에서 symbols 목록 로드 시도...");
        try {
            List<String> newSymbols = symbolMapper.selectAllSymbols();
            // 조회 결과가 null 이거나 비어있는 경우 로깅 및 기존 값 유지 또는 빈 리스트로 처리
            if (newSymbols == null || newSymbols.isEmpty()) {
                logger.warn("DB에서 조회된 symbols 목록이 비어있거나 null입니다. 기존 목록을 사용하거나 빈 목록으로 대체합니다.");
                // 기존 목록 유지: this.symbols = Objects.requireNonNullElse(this.symbols, List.of());
                // 빈 목록으로 설정: this.symbols = List.of();
                // 여기서는 빈 목록으로 설정하는 것으로 가정
                this.symbols = List.of();
            } else {
                this.symbols = newSymbols;
                logger.info("DB에서 {}개의 symbols 로드 완료.", newSymbols.size());
                logger.debug("로드된 symbols: {}", newSymbols); // DEBUG 레벨로 상세 로깅
            }
        } catch (Exception e) {
            logger.error("❌ DB에서 symbols 목록 로드 중 오류 발생", e);
            // 오류 발생 시 기존 symbols 목록을 유지하거나, 빈 리스트로 설정하는 등의 정책 필요
            // 여기서는 기존 목록 유지를 위해 별도 처리 안 함 (오류만 로깅)
            if (this.symbols == null) { // 초기화 시 오류 발생하면 빈 리스트라도 할당
                this.symbols = List.of();
            }
        }
    }

    /**
     * 현재 'symbols' 목록을 기반으로 WebSocket URL을 생성하고,
     * 새로운 BinanceWebSocketClient 인스턴스를 생성하여 연결을 시도합니다.
     * 기존 webSocketClient 참조는 새 인스턴스로 교체됩니다.
     *
     * @throws Exception WebSocket URL 생성 또는 클라이언트 생성/연결 중 발생할 수 있는 예외
     */
    private void connectWebSocket() throws Exception {
        if (this.symbols == null || this.symbols.isEmpty()) {
            logger.warn("symbols 목록이 비어있어 WebSocket에 연결할 수 없습니다.");
            // 기존 연결이 있다면 종료 처리
            if (this.webSocketClient != null) {
                logger.info("기존 WebSocket 클라이언트 종료 시도 (symbols 없음).");
                this.webSocketClient.destroy(); // destroy()로 확실히 정리
                this.webSocketClient = null; // 참조 제거
            }
            return;
        }

        // 1. markets 리스트 구성 (소문자 변환)
        List<String> markets = symbols.stream()
                .map(String::toLowerCase) // Locale.ROOT 불필요 시 제거 가능
                .toList();

        // 2. WebSocket URL 생성
        String webSocketUrl = binanceConfig.getFuturesWebSocketUrl(markets);
        logger.info("생성된 WebSocket URL (길이: {}): {}...", webSocketUrl.length(), webSocketUrl.substring(0, Math.min(webSocketUrl.length(), 100))); // 너무 길면 일부만 로깅
        URI webSocketUri = new URI(webSocketUrl);

        // 3. 새 WebSocket 클라이언트 인스턴스 생성
        //    (주의: BinanceWebSocketClient 내부에 자체적인 스케줄링 로직이 있다면,
        //     이 서비스에서 매일 재생성하는 경우 해당 로직이 중복될 수 있으므로 검토 필요)
        logger.info("새 BinanceWebSocketClient 인스턴스 생성...");
        BinanceWebSocketClient newClient = new BinanceWebSocketClient(
                webSocketUri, binanceKlineService, binanceTickerService,
                binanceTradeService, binanceFundingRateService, binanceAggTradeService,
                liquidationOrderService, partialBookDepthService
        );

        // 4. 새 클라이언트 연결 시도 (connect()는 비동기일 수 있음)
        logger.info("새 WebSocket 클라이언트 연결 시도...");
        newClient.connect(); // connect() 호출

        // 5. 현재 webSocketClient 참조를 새 클라이언트로 교체
        this.webSocketClient = newClient;
        logger.info("WebSocket 클라이언트 참조가 새 인스턴스로 업데이트됨.");
    }
}
