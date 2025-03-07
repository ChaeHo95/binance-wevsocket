package com.example.binancewebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceTakerBuySellVolumeDTO {

    @JsonProperty("symbol")  // JSON 필드명과 매핑
    private String symbol;

    @JsonProperty("buySellRatio")  // JSON 필드명과 매핑
    private BigDecimal buySellRatio;

    @JsonProperty("buyVol")  // JSON 필드명과 매핑
    private BigDecimal buyVolume;

    @JsonProperty("sellVol")  // JSON 필드명과 매핑
    private BigDecimal sellVolume;

    @JsonProperty("timestamp")  // JSON 필드명과 매핑
    private BigInteger timestamp;
}
