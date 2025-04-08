package com.example.binancewebsocket.service;

import com.example.binancewebsocket.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "enable.binance.scheduling", havingValue = "true", matchIfMissing = false)
public class BinanceWebSocketClient extends WebSocketClient {

    private final Logger logger = LoggerFactory.getLogger(BinanceWebSocketClient.class);

    // --- 의존 서비스 ---
    private final BinanceKlineService klineService;
    private final BinanceTickerService tickerService;
    private final BinanceTradeService tradeService;
    private final BinanceFundingRateService fundingRateService;
    private final BinanceAggTradeService aggTradeService;
    private final BinanceLiquidationOrderService liquidationOrderService;
    private final BinancePartialBookDepthService partialBookDepthService;

    // --- 내부 도구 ---
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- 재연결 관련 설정 및 상태 ---
    private final int MAX_RECONNECT_ATTEMPTS = 10; // 최대 재연결 시도 횟수
    private final long BASE_RECONNECT_DELAY_MS = TimeUnit.SECONDS.toMillis(5); // 기본 재연결 대기 시간 (5초)
    private final long MAX_RECONNECT_DELAY_MS = TimeUnit.SECONDS.toMillis(30); // 최대 재연결 대기 시간 (30초)

    // 재연결 시도 횟수 (스레드 안전하게 관리)
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    // 재연결 작업을 위한 스케줄러 (final로 선언, 한 번만 생성)
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "binance-reconnect-scheduler");
                thread.setDaemon(true); // 데몬 스레드로 설정하여 메인 앱 종료 시 강제 종료되도록 함
                return thread;
            }
    );


    /**
     * Binance WebSocketClient 생성자.
     * 필요한 서비스들을 주입받습니다.
     */

    public BinanceWebSocketClient(URI serverUri,
                                  BinanceKlineService klineService,
                                  BinanceTickerService tickerService,
                                  BinanceTradeService tradeService,
                                  BinanceFundingRateService fundingRateService,
                                  BinanceAggTradeService aggTradeService,
                                  BinanceLiquidationOrderService liquidationOrderService,
                                  BinancePartialBookDepthService partialBookDepthService
    ) {
        super(serverUri);
        this.klineService = klineService;
        this.tickerService = tickerService;
        this.tradeService = tradeService;
        this.fundingRateService = fundingRateService;
        this.aggTradeService = aggTradeService;
        this.liquidationOrderService = liquidationOrderService;
        this.partialBookDepthService = partialBookDepthService;
        logger.info("BinanceWebSocketClient 인스턴스 생성 완료. 재연결 스케줄러 시작됨.");
    }

    /**
     * 1분 주기로 WebSocket 상태를 체크하고, 닫혀 있으면 재연결 시도를 시작.
     * (주의: BinanceWebSocketService에서 매일 클라이언트를 재생성한다면 이 로직이 중복될 수 있음)
     */
    @Scheduled(fixedRate = 60_000)  // 1분
    public void checkAndReconnect() {
        // 현재 시간이 00:00 인 경우 스킵 (00:00 ~ 00:01 사이)
        // (이유가 명확하지 않다면 제거 고려)
        LocalTime now = LocalTime.now();
        if (now.getHour() == 0 && now.getMinute() == 0) {
            logger.debug("현재 시간이 00:00이므로 checkAndReconnect 실행을 건너<0xEB><0x8A>니다.");
            return;
        }

        logger.debug("WebSocket 연결 상태 확인 (스케줄링)..."); // 로그 레벨 조정

        if (!isWebSocketOpen()) {
            logger.warn("⚠️ WebSocket 연결이 닫혀있는 것을 확인. 재연결 시도 시작...");
            initiateReconnectSequence(); // 재연결 시퀀스 시작
        } else {
            logger.debug("✅ WebSocket 정상 작동 중."); // 로그 레벨 조정
        }
    }

    /**
     * ✅ WebSocket 연결 성공 시 호출
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("✅ Binance WebSocket 연결 성공! (Status: {}, Message: {})", handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
        // 연결 성공 시 재연결 시도 횟수 초기화
        reconnectAttempt.set(0);
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

            logger.info("📩 WebSocket 메시지 수신 [{}]:", stream);

            if (stream.contains("@kline_5m")) {
                handleKline5mMessage(data);
            } else if (stream.contains("@kline_1h")) {
                handleKline1hMessage(data);
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
                logger.warn("⚠️ 알 수 없는 데이터 수신: ");
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
            logger.info("📊 호가 데이터 저장됨");
        } catch (Exception e) {
            logger.error("❌ 호가 데이터 저장 오류: ", e);
        }
    }

    /**
     * ✅ Kline (캔들) 5분 단위 데이터 저장
     */
    private void handleKline5mMessage(JsonNode data) {
        try {
            BinanceKlineDTO klineDTO = objectMapper.treeToValue(data, BinanceKlineDTO.class);
            if (klineDTO.getIsKlineClosed()) {
                klineService.saveKline5m(klineDTO);
                logger.info("📊 Kline 5m 저장됨");
            }
        } catch (Exception e) {
            logger.error("❌ Kline 5m 저장 오류: ", e);
        }
    }

    /**
     * ✅ Kline (캔들) 1시간 단위 데이터 저장
     */
    private void handleKline1hMessage(JsonNode data) {
        try {
            BinanceKlineDTO klineDTO = objectMapper.treeToValue(data, BinanceKlineDTO.class);
            if (klineDTO.getIsKlineClosed()) {
                klineService.saveKline1h(klineDTO);
                logger.info("📊 Kline 1h 저장됨");
            }
        } catch (Exception e) {
            logger.error("❌ Kline 1h 저장 오류: ", e);
        }
    }

    /**
     * ✅ Ticker (24시간 가격 변동) 데이터 저장
     */
    private void handleTickerMessage(JsonNode data) {
        try {
            BinanceTickerDTO tickerDTO = objectMapper.treeToValue(data, BinanceTickerDTO.class);
            tickerService.saveTicker(tickerDTO);
            logger.info("📈 Ticker 저장됨");
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
            logger.info("💹 Trade 저장됨");
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
            logger.info("📦 Aggregate Trade 저장됨");
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
            logger.info("🔄 Mark Price 저장됨");
        } catch (Exception e) {
            logger.error("❌ Mark Price 저장 오류: ", e);
        }
    }

    /**
     * ✅ WebSocket 연결 종료 시 재연결 처리
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("❌ Binance WebSocket 연결 종료 감지: code={}, reason='{}', remote={}", code, reason, remote);
        // 연결 종료 시 재연결 시퀀스 시작
        initiateReconnectSequence();
    }


    /**
     * ✅ WebSocket 오류 발생 시 재연결
     */
    @Override
    public void onError(Exception ex) {
        // WebsocketNotConnectedException는 연결 시도 실패 시 발생 가능, 로깅 레벨 조절 가능
        if (ex instanceof WebsocketNotConnectedException) {
            logger.warn("❌ Binance WebSocket 오류 발생 (연결 안됨): {}", ex.getMessage());
        } else {
            logger.error("❌ Binance WebSocket 오류 발생: ", ex);
        }
        // 오류 발생 시 재연결 시퀀스 시작
        initiateReconnectSequence();
    }

    /**
     * 재연결 시퀀스를 시작합니다. 시도 횟수를 초기화하고 첫 번째 재연결 시도를 예약합니다.
     */
    private void initiateReconnectSequence() {
        // 이미 재연결 시도 중이거나 연결이 열려있으면 시작하지 않음 (선택적 중복 방지)
        if (!isWebSocketOpen() && reconnectAttempt.get() == 0) {
            logger.info("재연결 시퀀스 시작...");
            reconnectAttempt.set(0); // 재연결 시도 횟수 초기화
            scheduleReconnect();     // 첫 번째 재연결 시도 예약
        } else {
            logger.debug("이미 연결되어 있거나 재연결 시도 중이므로 새로운 시퀀스를 시작하지 않음.");
        }
    }

    /**
     * 스케줄러를 사용하여 지연 후 재연결 시도를 예약합니다.
     * 이 메소드는 재귀적으로 호출되지 않고, 실패 시 다음 시도를 다시 예약합니다.
     */
    private void scheduleReconnect() {
        // 스케줄러가 종료되었으면 더 이상 예약 불가
        if (reconnectScheduler.isShutdown()) {
            logger.warn("재연결 스케줄러가 종료되어 재연결 시도를 예약할 수 없습니다.");
            return;
        }

        // 현재 시도 횟수 확인
        int currentAttempt = reconnectAttempt.get();
        if (currentAttempt >= MAX_RECONNECT_ATTEMPTS) {
            logger.error("❌ 최대 재연결 시도 {}회를 초과. 더 이상 재시도하지 않음.", MAX_RECONNECT_ATTEMPTS);
            // 여기서 알림 발송 등 추가 조치 가능
            return;
        }

        // 지연 시간 계산 (Exponential Backoff + Max Cap)
        long delay = BASE_RECONNECT_DELAY_MS * (long) Math.pow(2, currentAttempt); // 2의 거듭제곱으로 증가
        delay = Math.min(delay, MAX_RECONNECT_DELAY_MS); // 최대 대기 시간 제한

        logger.warn("⚠️ WebSocket 재연결 {}/{}회 시도 예약 ({}ms 후)", currentAttempt + 1, MAX_RECONNECT_ATTEMPTS, delay);

        try {
            reconnectScheduler.schedule(() -> {
                // 스케줄된 작업 실행 시점에 다시 한번 상태 체크
                if (isWebSocketOpen() || Thread.currentThread().isInterrupted()) {
                    logger.info("재연결 작업 실행 시점에 이미 연결되었거나 스레드가 인터럽트되어 재연결 취소.");
                    // 이미 연결된 상태라면 시도 횟수 초기화가 필요할 수 있음 (onOpen에서 처리)
                    return;
                }

                logger.info("재연결 시도 실행 ({}차)", reconnectAttempt.get() + 1);
                try {
                    // reconnect()는 비동기, reconnectBlocking()은 동기. 환경에 맞게 선택.
                    // 여기서는 reconnectBlocking() 사용 예시 (결과를 기다림)
                    boolean success = reconnectBlocking(); // 성공 시 true 반환 가정
                    // 또는 connectBlocking() 사용 고려

                    if (success && isWebSocketOpen()) {
                        // onOpen 핸들러에서 로그 및 초기화 처리하므로 여기서는 별도 처리 불필요
                        logger.info("✅ WebSocket 재연결 성공! ({}차 시도)", reconnectAttempt.get() + 1); // onOpen에서 로깅
                    } else {
                        // 실패 시 다음 시도 예약 전 시도 횟수 증가
                        int nextAttempt = reconnectAttempt.incrementAndGet();
                        logger.warn("⚠️ 재연결 실패 ({}차). 다음 시도 예약...", nextAttempt);
                        scheduleReconnect(); // 다음 재연결 예약
                    }
                } catch (InterruptedException ie) {
                    logger.error("❌ 재연결 실행 중 (Blocking 대기) 인터럽트 발생", ie);
                    Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                    // 인터럽트 시 재시도 정책 결정 필요 (여기서는 다음 시도 예약 안 함)
                } catch (Exception e) { // reconnectBlocking 중 발생할 수 있는 다른 예외 처리
                    logger.error("❌ 재연결 실행 중 예상치 못한 예외 발생: {}", e.getMessage(), e);
                    int nextAttempt = reconnectAttempt.incrementAndGet(); // 시도 횟수 증가
                    logger.warn("⚠️ 재연결 실패 ({}차). 다음 시도 예약...", nextAttempt);
                    scheduleReconnect(); // 다음 재연결 예약
                }

            }, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // 스케줄러가 종료된 후에 작업 예약을 시도할 때 발생
            logger.error("❌ 스케줄러가 종료되어 재연결 작업을 예약할 수 없습니다.", e);
        }
    }


    /**
     * 재연결 스케줄러를 안전하게 종료합니다.
     * awaitTermination과 shutdownNow를 사용하여 최대한 정리합니다.
     */
    private synchronized void shutdownScheduler() {
        if (reconnectScheduler != null && !reconnectScheduler.isShutdown()) {
            logger.info("🛑 재연결 스케줄러 종료 시도...");
            reconnectScheduler.shutdown(); // 새 작업 예약 중단
            try {
                // 기존 작업 완료 대기 (짧은 시간)
                if (!reconnectScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.warn("재연결 스케줄러가 1초 내에 정상 종료되지 않아 강제 종료(shutdownNow) 시도...");
                    reconnectScheduler.shutdownNow(); // 실행 중 작업 인터럽트 시도
                    // 강제 종료 후 대기
                    if (!reconnectScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                        logger.error("재연결 스케줄러가 강제 종료 후에도 1초 내에 완전히 종료되지 않음.");
                    }
                } else {
                    logger.info("재연결 스케줄러가 성공적으로 종료되었습니다.");
                }
            } catch (InterruptedException ie) {
                logger.error("재연결 스케줄러 종료 대기 중 인터럽트 발생. 즉시 강제 종료.", ie);
                reconnectScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            logger.info("재연결 스케줄러가 이미 종료되었거나 null입니다.");
        }
    }

    /**
     * 이 클라이언트 인스턴스의 리소스를 정리합니다.
     * 주로 외부(BinanceWebSocketService)에서 호출됩니다.
     */
    @PreDestroy // 스프링 컨텍스트가 직접 관리할 경우에도 대비
    public synchronized void destroy() {
        logger.info("BinanceWebSocketClient 리소스 정리 시작 (destroy 호출됨)...");
        shutdownScheduler(); // 스케줄러 종료
        if (isOpen()) {
            logger.info("WebSocket 연결 종료 시도...");
            try {
                // closeBlocking()으로 동기적 종료 시도 (타임아웃 설정 가능)
                this.closeBlocking();
                logger.info("WebSocket 연결 종료 완료.");
            } catch (InterruptedException e) {
                logger.error("WebSocket 연결 종료(closeBlocking) 대기 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("WebSocket 연결 종료 중 오류 발생", e);
            }
        } else {
            logger.info("WebSocket이 이미 닫혀있거나 연결된 적 없음.");
        }
        logger.info("BinanceWebSocketClient 리소스 정리 완료.");
    }

    /**
     * WebSocket 현재 연결 상태 확인 (라이브러리 메소드 래핑)
     * synchronized 는 WebSocketClient 내부 상태 접근 시 필요할 수 있음 (라이브러리 구현 확인 필요)
     */
    public synchronized boolean isWebSocketOpen() {
        return this.isOpen();
    }

    /**
     * WebSocket 현재 닫힘 상태 확인 (라이브러리 메소드 래핑)
     */
    public synchronized boolean isWebSocketClosed() {
        return this.isClosed();
    }
}
