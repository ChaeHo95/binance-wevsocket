package com.example.binancewebsocket.scheduler;

import com.example.binancewebsocket.service.BinanceLongShortRatioService;
import com.example.binancewebsocket.service.BinanceOpenInterestService;
import com.example.binancewebsocket.service.BinanceOpenInterestStatisticsService;
import com.example.binancewebsocket.service.BinanceTakerBuySellVolumeService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "enable.binance.scheduling", havingValue = "true")
public class BinanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BinanceScheduler.class);

    private final BinanceLongShortRatioService binanceLongShortRatioService;
    private final BinanceTakerBuySellVolumeService binanceTakerBuySellVolumeService;
    private final BinanceOpenInterestStatisticsService binanceOpenInterestStatisticsService;
    private final BinanceOpenInterestService binanceOpenInterestService;
    private final List<String> symbols;

    // 단일 ExecutorService (코어 쓰레드 수 = symbols.size() * 4, 최대 쓰레드 수 = 코어 쓰레드 수 * 2)
    private final ExecutorService executor;

    public BinanceScheduler(
            @Value("${symbols}") List<String> symbols,
            BinanceLongShortRatioService binanceLongShortRatioService,
            BinanceTakerBuySellVolumeService binanceTakerBuySellVolumeService,
            BinanceOpenInterestStatisticsService binanceOpenInterestStatisticsService,
            BinanceOpenInterestService binanceOpenInterestService) {
        this.symbols = symbols;
        this.binanceLongShortRatioService = binanceLongShortRatioService;
        this.binanceTakerBuySellVolumeService = binanceTakerBuySellVolumeService;
        this.binanceOpenInterestStatisticsService = binanceOpenInterestStatisticsService;
        this.binanceOpenInterestService = binanceOpenInterestService;

        int corePoolSize = symbols.size() * 4; // 코어 쓰레드 수: symbol 개수 * API 종류 개수 (4)
        int maxPoolSize = corePoolSize * 2;   // 최대 쓰레드 수
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
    }

    private CompletableFuture<Void> fetchDataWithRetry(String symbol, String apiType, Callable<Void> task) {
        return CompletableFuture.runAsync(() -> {
            int maxRetries = 3;
            int retryDelaySeconds = 5;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    task.call();
                    return;
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        logger.error("Failed after {} attempts: {} - symbol={}", maxRetries, apiType, symbol, e);
                        return;
                    }
                    logger.warn("Retry {}/{} for {} - symbol={}. Wait {}s...", attempt + 1, maxRetries, apiType,
                            symbol, retryDelaySeconds);
                    try {
                        TimeUnit.SECONDS.sleep(retryDelaySeconds);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted while waiting for retry: symbol={}", symbol, ie);
                        return;
                    }
                }
            }
        }, executor); // 동일한 executor 사용
    }

    @Scheduled(fixedRate = 25 * 60 * 1000, initialDelay = 1000)
    public void getTakerBuySellVolume() {
        logger.info("Starting scheduled Taker Buy/Sell Volume data fetch...");
        symbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "TakerBuySellVolume", () -> {
                    binanceTakerBuySellVolumeService.fetchAndSaveTakerBuySellVolume(symbol, "5m", 30);
                    return null;
                })
        );
    }

    @Scheduled(fixedRate = 25 * 60 * 1000, initialDelay = 1000)
    public void getLongShortRatio() {
        logger.info("Starting scheduled Long/Short Ratio data fetch...");
        symbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "LongShortRatio", () -> {
                    binanceLongShortRatioService.fetchAndSaveLongShortRatio(symbol, "5m", 30);
                    return null;
                })
        );
    }

    @Scheduled(fixedRate = 25 * 60 * 1000, initialDelay = 1000)
    public void getOpenInterestStatistics() {
        logger.info("Starting scheduled Open Interest Statistics data fetch...");
        symbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "OpenInterestStatistics", () -> {
                    binanceOpenInterestStatisticsService.fetchAndSaveOpenInterestStatistics(symbol, "5m", 30);
                    return null;
                })
        );
    }

    @Scheduled(fixedRate = 1000, initialDelay = 1000)
    public void getOpenInterest() {
        logger.info("Starting scheduled Open Interest data fetch...");
        symbols.forEach(symbol ->
                fetchDataWithRetry(symbol, "OpenInterest", () -> {
                    binanceOpenInterestService.fetchAndSaveOpenInterest(symbol);
                    return null;
                })
        );
    }

    // 스프링 Bean 종료 시점
    @PreDestroy
    public void shutdownExecutor() {
        logger.info("Shutting down executor service...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in 60s, calling shutdownNow()...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for executor service shutdown.", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Executor service shut down successfully.");
    }
}
