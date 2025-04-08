package com.example.binancewebsocket.scheduler;

import com.example.binancewebsocket.mapper.SymbolMapper;
import com.example.binancewebsocket.service.BinanceLongShortRatioService;
import com.example.binancewebsocket.service.BinanceOpenInterestService;
import com.example.binancewebsocket.service.BinanceOpenInterestStatisticsService;
import com.example.binancewebsocket.service.BinanceTakerBuySellVolumeService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "enable.binance.scheduling", havingValue = "true")
public class BinanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BinanceScheduler.class);
    private static final int API_CALL_TYPES = 4; // 호출하는 API 종류 개수
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 5;

    private static final long SCHEDULE_RATE_MINUTES = 25; // 분 단위 스케줄 주기 (Taker Volume, Long/Short, OI Stats)
    private static final long OPEN_INTEREST_RATE_SECONDS = 10; // 초 단위 스케줄 주기 (Open Interest) - **주의: 빈번한 호출은 API 제한 및 부하 유발 가능**

    @Autowired
    private BinanceLongShortRatioService binanceLongShortRatioService;
    @Autowired
    private BinanceTakerBuySellVolumeService binanceTakerBuySellVolumeService;
    @Autowired
    private BinanceOpenInterestStatisticsService binanceOpenInterestStatisticsService;
    @Autowired
    private BinanceOpenInterestService binanceOpenInterestService;
    @Autowired
    private SymbolMapper symbolMapper;

    // 스레드 안전성을 위해 AtomicReference 사용
    private final AtomicReference<List<String>> symbolsRef = new AtomicReference<>(List.of());
    private volatile ExecutorService executor; // volatile 키워드 추가

    @PostConstruct
    public void initialize() {
        logger.info("BinanceScheduler 초기화 시작.");
        try {
            updateSymbolsList(); // symbols 리스트 초기화
            reconfigureExecutorService(); // ExecutorService 설정
            logger.info("BinanceScheduler 초기화 성공. Symbols: {}", symbolsRef.get().size());
        } catch (Exception e) {
            logger.error("BinanceScheduler 초기화 중 오류 발생", e);
        }
    }

    /**
     * 매일 자정(00시)에 DB에서 symbols 값을 업데이트하고 ExecutorService를 재설정합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void refreshSymbolsAndExecutor() {
        logger.info("매일 자정 Symbols 리스트 및 ExecutorService 재설정 시작.");
        try {
            updateSymbolsList();
            reconfigureExecutorService(); // 심볼 개수 변경에 따라 ExecutorService 재설정
            logger.info("Symbols 리스트 및 ExecutorService 재설정 완료. Symbols: {}", symbolsRef.get().size());
        } catch (Exception e) {
            logger.error("Symbols 업데이트 및 ExecutorService 재설정 실패", e);
        }
    }

    // DB에서 심볼 목록을 가져와 AtomicReference 업데이트
    private void updateSymbolsList() {
        try {
            List<String> newSymbols = symbolMapper.selectAllSymbols();
            if (newSymbols == null || newSymbols.isEmpty()) {
                logger.warn("DB에서 조회된 symbol 리스트가 비어있거나 null입니다.");
                this.symbolsRef.set(List.of());
            } else {
                this.symbolsRef.set(List.copyOf(newSymbols)); // 불변 리스트로 설정
                logger.info("DB에서 Symbols 업데이트 완료. 개수: {}", newSymbols.size());
            }
        } catch (Exception e) {
            logger.error("DB에서 Symbols 조회 중 오류 발생", e);
        }
    }

    // ExecutorService를 현재 symbols 개수에 맞게 (재)설정
    private void reconfigureExecutorService() {
        ExecutorService oldExecutor = this.executor; // 이전 Executor 참조 저장

        int symbolCount = symbolsRef.get().size();
        if (symbolCount == 0) {
            logger.warn("Symbols 리스트가 비어있어 기본 ExecutorService를 설정합니다 (1 core, 1 max).");
            this.executor = Executors.newSingleThreadExecutor();
        } else {
            int corePoolSize = Math.max(1, symbolCount * API_CALL_TYPES); // 최소 1 보장
            int maxPoolSize = corePoolSize * 2;
            logger.info("ExecutorService 재설정 중... Core: {}, Max: {}", corePoolSize, maxPoolSize);
            this.executor = new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1000),
                    Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            logger.info("ExecutorService 재설정 완료.");
        }

        // 이전 ExecutorService가 존재하면 안전하게 종료
        if (oldExecutor != null && !oldExecutor.isShutdown()) {
            shutdownExecutorService(oldExecutor, "이전 ExecutorService");
        }
    }

    // 데이터 가져오기 작업을 비동기 및 재시도 로직과 함께 실행
    private CompletableFuture<Void> fetchDataWithRetry(String symbol, String apiType, Runnable task) {
        ExecutorService currentExecutor = this.executor;
        if (currentExecutor == null || currentExecutor.isShutdown()) {
            logger.warn("{} API 호출 위한 ExecutorService가 준비되지 않았거나 종료되었습니다. Symbol: {}", apiType, symbol);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    task.run();
                    return; // 성공 시 종료
                } catch (Exception e) {
                    logger.warn("API 호출 실패 ({}/{}): {} - Symbol: {}. {}초 후 재시도...",
                            attempt, MAX_RETRIES, apiType, symbol, RETRY_DELAY_SECONDS, e);
                    if (attempt == MAX_RETRIES) {
                        logger.error("최대 재시도 ({}) 실패: {} - Symbol: {}", MAX_RETRIES, apiType, symbol, e);
                        break;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        logger.error("재시도 대기 중 인터럽트 발생: {} - Symbol: {}", apiType, symbol, ie);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, currentExecutor);
    }

    // --- 스케줄링된 데이터 가져오기 메소드들 (상수 사용) ---

    // Taker Buy/Sell Volume
    @Scheduled(fixedRate = SCHEDULE_RATE_MINUTES, timeUnit = TimeUnit.MINUTES, initialDelay = 1000)
    public void fetchTakerBuySellVolume() {
        logger.info("Taker Buy/Sell Volume 데이터 가져오기 시작...");
        List<String> currentSymbols = symbolsRef.get();
        currentSymbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "TakerBuySellVolume", () ->
                        binanceTakerBuySellVolumeService.fetchAndSaveTakerBuySellVolume(symbol, "5m", 30)
                )
        );
        logger.info("Taker Buy/Sell Volume 데이터 가져오기 요청 완료 ({} symbols).", currentSymbols.size());
    }

    // Long/Short Ratio
    @Scheduled(fixedRate = SCHEDULE_RATE_MINUTES, timeUnit = TimeUnit.MINUTES, initialDelay = 2000)
    public void fetchLongShortRatio() {
        logger.info("Long/Short Ratio 데이터 가져오기 시작...");
        List<String> currentSymbols = symbolsRef.get();
        currentSymbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "LongShortRatio", () ->
                        binanceLongShortRatioService.fetchAndSaveLongShortRatio(symbol, "5m", 30)
                )
        );
        logger.info("Long/Short Ratio 데이터 가져오기 요청 완료 ({} symbols).", currentSymbols.size());
    }

    // Open Interest Statistics
    @Scheduled(fixedRate = SCHEDULE_RATE_MINUTES, timeUnit = TimeUnit.MINUTES, initialDelay = 3000)
    public void fetchOpenInterestStatistics() {
        logger.info("Open Interest Statistics 데이터 가져오기 시작...");
        List<String> currentSymbols = symbolsRef.get();
        currentSymbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "OpenInterestStatistics", () ->
                        binanceOpenInterestStatisticsService.fetchAndSaveOpenInterestStatistics(symbol, "5m", 30)
                )
        );
        logger.info("Open Interest Statistics 데이터 가져오기 요청 완료 ({} symbols).", currentSymbols.size());
    }

    // Open Interest
    @Scheduled(fixedRate = OPEN_INTEREST_RATE_SECONDS, timeUnit = TimeUnit.SECONDS, initialDelay = 4000)
    public void fetchOpenInterest() {
        logger.info("Open Interest 데이터 가져오기 시작..."); // 빈번하므로 DEBUG 레벨 고려
        List<String> currentSymbols = symbolsRef.get();
        currentSymbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "OpenInterest", () ->
                        binanceOpenInterestService.fetchAndSaveOpenInterest(symbol)
                )
        );
        logger.info("Open Interest 데이터 가져오기 요청 완료 ({} symbols).", currentSymbols.size());
    }

    // 스프링 Bean 종료 시 ExecutorService 종료 처리
    @PreDestroy
    public void cleanup() {
        logger.info("BinanceScheduler 종료 시작...");
        shutdownExecutorService(this.executor, "메인 ExecutorService");
        logger.info("BinanceScheduler 종료 완료.");
    }

    // ExecutorService를 안전하게 종료하는 헬퍼 메소드
    private void shutdownExecutorService(ExecutorService executorToShutdown, String name) {
        if (executorToShutdown != null && !executorToShutdown.isShutdown()) {
            logger.info("{} 종료 시도...", name);
            executorToShutdown.shutdown();
            try {
                if (!executorToShutdown.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.warn("{}가 60초 내에 정상적으로 종료되지 않았습니다. 강제 종료(shutdownNow)를 시도합니다.", name);
                    List<Runnable> droppedTasks = executorToShutdown.shutdownNow();
                    logger.warn("{} 강제 종료 시 중단된 작업 수: {}", name, droppedTasks.size());
                    if (!executorToShutdown.awaitTermination(60, TimeUnit.SECONDS)) {
                        logger.error("{}가 강제 종료 후에도 60초 내에 완전히 종료되지 않았습니다.", name);
                    }
                } else {
                    logger.info("{}가 성공적으로 종료되었습니다.", name);
                }
            } catch (InterruptedException e) {
                logger.error("{} 종료 대기 중 인터럽트 발생. 즉시 강제 종료합니다.", name, e);
                executorToShutdown.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            logger.info("{}가 이미 종료되었거나 null입니다.", name);
        }
    }
}
