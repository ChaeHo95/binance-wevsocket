package com.example.binancewebsocket.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class LogFolderInitializer {
    private Logger logger = LoggerFactory.getLogger(LogFolderInitializer.class);

    private String LOG_PATH = System.getProperty("LOG_PATH", "./logs");

    @PostConstruct
    public void createLogDirectory() {
        File logDir = new File(LOG_PATH);
        if (!logDir.exists()) {
            if (logDir.mkdirs()) {
                logger.info("✅ 로그 디렉토리 생성됨: " + LOG_PATH);
            } else {
                logger.error("❌ 로그 디렉토리 생성 실패: " + LOG_PATH);
            }
        }
    }
}
