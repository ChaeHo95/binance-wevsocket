package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "enable.binance.scheduling", havingValue = "true", matchIfMissing = false)
public class BinanceWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketClient.class);


    private final BinanceKlineService klineService;
    private final BinanceTickerService tickerService;
    private final BinanceTradeService tradeService;
    private final BinanceFundingRateService fundingRateService;
    private final BinanceAggTradeService aggTradeService;
    private final BinanceLiquidationOrderService liquidationOrderService;
    private final BinancePartialBookDepthService partialBookDepthService;


    private final ObjectMapper objectMapper;

    // ✅ 재연결 관련 변수
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY = TimeUnit.SECONDS.toMillis(5);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Value("${enable.binance.websocket}") // 기본값 false
    private boolean enableWebSocket;

    /**
     * ✅ Binance WebSocketClient 생성자
     */
    public BinanceWebSocketClient(URI serverUri,
                                  BinanceKlineService klineService,
                                  BinanceTickerService tickerService,
                                  BinanceTradeService tradeService,
                                  BinanceFundingRateService fundingRateService,
                                  BinanceAggTradeService aggTradeService,
                                  BinanceLiquidationOrderService liquidationOrderService,
                                  BinancePartialBookDepthService partialBookDepthService,
                                  ObjectMapper objectMapper) {
        super(serverUri);
        this.klineService = klineService;
        this.tickerService = tickerService;
        this.tradeService = tradeService;
        this.fundingRateService = fundingRateService;
        this.aggTradeService = aggTradeService;
        this.objectMapper = objectMapper;
        this.liquidationOrderService = liquidationOrderService;
        this.partialBookDepthService = partialBookDepthService;
    }

    /**
     * ✅ 1분 주기로 WebSocket 상태를 체크하고, 닫혀 있으면 재연결.
     * - @Scheduled가 동작하려면 @EnableScheduling 필요
     * - 또한 @ConditionalOnProperty 설정에 의해 'enable.binance.scheduling=true' 여야 Bean이 등록됨
     */
    @Scheduled(fixedRate = 60_000)  // 1분
    public synchronized void checkAndReconnect() {
        logger.info("⚠️ WebSocket 작동 확인 (스케줄링 동작).");

        // application.properties 에서 enable.binance.websocket=true 로 설정되어 있어야 true
        if (!enableWebSocket) {
            logger.info("⚠ WebSocket 실행이 비활성화됨. (enableWebSocket=false)");
            return;
        }

        // 현재 WebSocket이 열려 있는지 확인
        if (!isWebSocketOpen()) {
            logger.warn("⚠️ WebSocket이 닫혀 있음. 재연결 시도...");
            reconnectWithDelay();
        } else {
            logger.info("✅ WebSocket 정상 작동 중.");
        }
    }

    /**
     * ✅ WebSocket 연결 성공 시 호출
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("✅ Binance WebSocket 연결 성공!");
    }

    /**
     * ✅ WebSocket 메시지 수신 처리
     */
    @Override
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // ✅ `stream` 필드 존재 여부 확인 후 처리
            if (!root.has("stream")) {
                logger.warn("⚠️ WebSocket 메시지에 'stream' 필드가 없음: {}", message);
                return;
            }

            String stream = root.get("stream").asText();
            JsonNode data = root.get("data");

            // ✅ `data` 필드 존재 여부 확인
            if (data == null) {
                logger.warn("⚠️ WebSocket 메시지에 'data' 필드가 없음: {}", message);
                return;
            }

            logger.info("📩 WebSocket 메시지 수신 [{}]: {}", stream, data.toString());

            if (stream.contains("@kline")) {
                handleKlineMessage(data);
            } else if (stream.contains("@ticker")) {
                handleTickerMessage(data);
            } else if (stream.contains("@trade")) {
                handleTradeMessage(data);
            } else if (stream.contains("@aggTrade")) {
                handleAggTradeMessage(data);
            } else if (stream.contains("@markPrice")) {
                handleMarkPriceMessage(data);
            } else if (stream.contains("@forceOrder")) {
                handleLiquidationOrderMessage(data);
            } else if (stream.contains("@depth")) {
                handlePartialBookDepthMessage(data);
            } else {
                logger.warn("⚠️ 알 수 없는 데이터 수신: {}", stream);
            }
        } catch (Exception e) {
            logger.error("❌ WebSocket 메시지 처리 오류: ", e);
        }
    }

    /**
     * ✅ 강제 청산 정보 저장 (Liquidation Order Streams)
     * 해당 메서드는 @forceOrder 스트림을 통해 수신된 강제 청산 데이터를 저장합니다.
     */
    private void handleLiquidationOrderMessage(JsonNode data) {
        try {
            BinanceLiquidationOrderDTO liquidationOrder = objectMapper.treeToValue(data, BinanceLiquidationOrderDTO.class);
            liquidationOrderService.saveLiquidationOrder(liquidationOrder);
            logger.info("🔥 강제 청산 정보 저장됨: {}", liquidationOrder);
        } catch (Exception e) {
            logger.error("❌ 강제 청산 정보 저장 오류: ", e);
        }
    }

    /**
     * ✅ 호가 데이터 저장 (Partial Book Depth Streams)
     * 해당 메서드는 @depth 스트림을 통해 수신된 호가 데이터를 저장합니다.
     */
    private void handlePartialBookDepthMessage(JsonNode data) {
        try {
            BinancePartialBookDepthDTO partialBookDepth = objectMapper.treeToValue(data, BinancePartialBookDepthDTO.class);
            partialBookDepthService.savePartialBookDepth(partialBookDepth);
            logger.info("📊 호가 데이터 저장됨: {}", partialBookDepth);
        } catch (Exception e) {
            logger.error("❌ 호가 데이터 저장 오류: ", e);
        }
    }

    /**
     * ✅ Kline (캔들) 데이터 저장
     */
    private void handleKlineMessage(JsonNode data) {
        try {
            BinanceKlineDTO klineDTO = objectMapper.treeToValue(data, BinanceKlineDTO.class);
            if (klineDTO.getIsKlineClosed()) {
                klineService.saveKline(klineDTO);
                logger.info("📊 Kline 저장됨: {}", klineDTO);
            }
        } catch (Exception e) {
            logger.error("❌ Kline 저장 오류: ", e);
        }
    }

    /**
     * ✅ Ticker (24시간 가격 변동) 데이터 저장
     */
    private void handleTickerMessage(JsonNode data) {
        try {
            BinanceTickerDTO tickerDTO = objectMapper.treeToValue(data, BinanceTickerDTO.class);
            tickerService.saveTicker(tickerDTO);
            logger.info("📈 Ticker 저장됨: {}", tickerDTO);
        } catch (Exception e) {
            logger.error("❌ Ticker 저장 오류: ", e);
        }
    }

    /**
     * ✅ Trade (거래 체결 정보) 데이터 저장
     */
    private void handleTradeMessage(JsonNode data) {
        try {
            BinanceTradeDTO tradeDTO = objectMapper.treeToValue(data, BinanceTradeDTO.class);
            tradeService.saveTrade(tradeDTO);
            logger.info("💹 Trade 저장됨: {}", tradeDTO);
        } catch (Exception e) {
            logger.error("❌ Trade 저장 오류: ", e);
        }
    }

    /**
     * ✅ Aggregate Trade (묶음 거래) 데이터 저장
     */
    private void handleAggTradeMessage(JsonNode data) {
        try {
            BinanceAggTradeDTO aggTradeDTO = objectMapper.treeToValue(data, BinanceAggTradeDTO.class);
            aggTradeService.saveAggTrade(aggTradeDTO);
            logger.info("📦 Aggregate Trade 저장됨: {}", aggTradeDTO);
        } catch (Exception e) {
            logger.error("❌ Aggregate Trade 저장 오류: ", e);
        }
    }

    /**
     * ✅ Mark Price (시장 가격 및 펀딩 비율) 데이터 저장
     */
    private void handleMarkPriceMessage(JsonNode data) {
        try {
            BinanceFundingRateDTO fundingRateDTO = objectMapper.treeToValue(data, BinanceFundingRateDTO.class);
            fundingRateService.saveFundingRate(fundingRateDTO);
            logger.info("🔄 Mark Price 저장됨: {}", fundingRateDTO);
        } catch (Exception e) {
            logger.error("❌ Mark Price 저장 오류: ", e);
        }
    }

    /**
     * ✅ WebSocket 연결 종료 시 재연결 처리
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("❌ Binance WebSocket 연결 종료: {} {} {} ", code, reason, remote);
        reconnectWithDelay();
    }


    /**
     * ✅ WebSocket 오류 발생 시 재연결
     */
    @Override
    public void onError(Exception ex) {
        logger.error("❌ Binance WebSocket 오류 발생: ", ex);
        reconnectWithDelay();
    }


    /**
     * 외부에서 최초로 재연결을 시작할 때 호출하는 메서드.
     * attempt=0 으로 시작한다.
     */
    private void reconnectWithDelay() {
        reconnectWithDelay(0);
    }

    /**
     * 재귀적으로 재연결을 시도하는 메서드
     *
     * @param attempt 현재까지 재연결 시도 횟수
     */
    private void reconnectWithDelay(int attempt) {
        // 이미 열려 있다면 재연결 불필요
        if (isWebSocketOpen()) {
            logger.info("🔗 WebSocket이 이미 열려 있음. 스케줄러 종료 처리.");
            shutdownScheduler();
            return;
        }

        // 최대 재연결 횟수를 초과
        if (attempt >= MAX_RECONNECT_ATTEMPTS) {
            logger.error("❌ 최대 재연결 시도 {}회를 초과. 더 이상 재시도하지 않음.", MAX_RECONNECT_ATTEMPTS);
            shutdownScheduler();
            return;
        }

        // 스케줄러가 null이거나 종료됐으면 새로 생성
        if (scheduler == null || scheduler.isShutdown()) {
            logger.info("⚙️ 스케줄러가 null 또는 종료됨. 새로 생성합니다.");
            scheduler = Executors.newScheduledThreadPool(1);
        }

        // (attempt+1)에 따른 지연시간, 최대 30초 제한
        long delay = RECONNECT_DELAY * (attempt + 1);
        delay = Math.min(delay, TimeUnit.SECONDS.toMillis(30));

        logger.warn("⚠️ WebSocket 닫힘. 재연결 {}/{}회 시도 예약 (대기: {}ms 후)",
                attempt + 1, MAX_RECONNECT_ATTEMPTS, delay);

        // 여기서부터 스케줄링 (delay 뒤 한 번 실행)
        scheduler.schedule(() -> {
            // 1) 스레드 인터럽트 체크
            if (Thread.currentThread().isInterrupted()) {
                logger.error("❌ 재연결 중 스레드가 인터럽트됨. 기존 스레드풀 종료 후 재생성.");
                shutdownScheduler();  // 기존 스케줄러 종료

                // 여전히 재연결을 계속 원한다면, 새 스레드풀 만들고 다시 시도
                // (attempt를 그대로 넘겨서 이번 시도를 다시 한번 수행할 수도 있고,
                //  attempt+1로 넘길 수도 있음. 여기서는 동일 시도 횟수로 다시 시도 예시)
                reconnectWithDelay(attempt);
                return;
            }

            // 2) 재연결 로직 시도
            try {
                reconnect(); // org.java_websocket.client.WebSocketClient의 reconnect()
            } catch (Exception e) {
                logger.error("❌ 재연결 시도 중 예외 발생: {}", e.getMessage(), e);
            }

            // 3) 재연결 성공 여부 확인
            if (isWebSocketOpen()) {
                logger.info("✅ WebSocket 재연결 성공! ({}차 시도). 스케줄러 종료 처리.", attempt + 1);
                shutdownScheduler(); // 여기서 스케줄러 종료 (원한다면 유지 가능)
            } else {
                logger.warn("⚠️ 재연결 실패 ({}차). 다음 시도 예약...", attempt + 1);
                reconnectWithDelay(attempt + 1); // 다음 시도
            }

        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 스케줄러 안전 종료
     */
    private synchronized void shutdownScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.info("🛑 스케줄러를 종료합니다.");
            scheduler.shutdown();
            // shutdownNow()는 큐에 남은 작업을 즉시 중단하고, 현재 실행 중인 스레드에도 인터럽트를 걸어줍니다.
            scheduler = null;  // 이후 재사용 시 새로 할당해야 함
        }
    }

    /**
     * ✅ WebSocket 현재 상태 확인
     */
    public synchronized boolean isWebSocketOpen() {
        return this.isOpen();
    }

    public boolean isWebSocketClosed() {
        return this.isClosed();
    }

}
