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
     * ✅ WebSocket 상태 체크 및 자동 재연결 (1분마다 실행)
     */
    @Scheduled(fixedRate = 1 * 60 * 1000)
    public synchronized void checkAndReconnect() {
        logger.info("⚠️ WebSocket 작동 확인.");
        if (!enableWebSocket) {
            logger.info("⚠ WebSocket 실행이 비활성화됨.");
            return; // 실행하지 않음
        }


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


    private void reconnectWithDelay() {
        for (int reconnectAttempts = 0; reconnectAttempts < MAX_RECONNECT_ATTEMPTS; reconnectAttempts++) {

            // 현재 WebSocket 연결 상태 로그
            if (isWebSocketOpen()) {
                logger.info("🔗 WebSocket이 현재 연결된 상태입니다.");
                return; // 이미 연결되어 있으면 재연결 불필요
            } else {
                logger.warn("⚠️ WebSocket이 닫혀 있음. 재연결 시도...");
            }

            // 지수적으로 대기 시간 증가, 최대 30초 (30000ms)로 제한
            long delay = RECONNECT_DELAY * (reconnectAttempts + 1);
            delay = Math.min(delay, 30000);  // 최대 30초로 제한

            // 재연결 시도 로그 출력
            logger.info("⏳ WebSocket 재연결 시도 중... 시도 {} / {}. 대기 시간: {}ms", reconnectAttempts + 1, MAX_RECONNECT_ATTEMPTS, delay);

            try {
                // 인터럽트 상태 확인
                if (Thread.currentThread().isInterrupted()) {
                    logger.error("❌ 재연결 중 스레드가 인터럽트되었습니다. 작업을 중단합니다.");
                    break;  // 인터럽트 상태이면 종료
                }

                // 지연 후 재연결 시도
                Thread.sleep(delay);
                reconnect(); // WebSocket 재연결

                // 재연결 성공 여부 확인
                if (isWebSocketOpen()) {
                    logger.info("✅ WebSocket 재연결 성공! 시도 {} / {}", reconnectAttempts + 1, MAX_RECONNECT_ATTEMPTS);
                    return; // 성공적으로 재연결 되었으면 종료
                } else {
                    logger.warn("⚠️ WebSocket이 아직 닫혀 있음. 다음 재연결 시도 준비...");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // 인터럽트 상태 다시 설정
                logger.error("❌ 재연결 중단됨: 스레드 인터럽트 예외 발생. 시도 {} / {}", reconnectAttempts + 1, MAX_RECONNECT_ATTEMPTS, e);
                break; // InterruptedException 발생 시 루프 종료
            }
        }

        // 최대 재연결 시도 횟수를 초과한 경우 로그 남기기
        logger.error("❌ 최대 재연결 시도 횟수 초과. WebSocket 연결 종료.");
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
