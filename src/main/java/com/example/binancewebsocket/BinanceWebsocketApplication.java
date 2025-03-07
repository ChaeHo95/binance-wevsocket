package com.example.binancewebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BinanceWebsocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceWebsocketApplication.class, args);
    }

}

