package com.example.binancewebsocket.utils;


import com.example.binancewebsocket.dto.BinancePartialBookDepthDTO;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderBookEntryDeserializer extends JsonDeserializer<List<BinancePartialBookDepthDTO.OrderBookEntry>> {
    @Override
    public List<BinancePartialBookDepthDTO.OrderBookEntry> deserialize(JsonParser jsonParser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        List<BinancePartialBookDepthDTO.OrderBookEntry> orderBookEntries = new ArrayList<>();

        if (node.isArray()) {
            for (JsonNode entry : node) {
                if (entry.isArray() && entry.size() == 2) {
                    BigDecimal price = new BigDecimal(entry.get(0).asText());
                    BigDecimal quantity = new BigDecimal(entry.get(1).asText());
                    orderBookEntries.add(new BinancePartialBookDepthDTO.OrderBookEntry(price, quantity));
                }
            }
        }
        return orderBookEntries;
    }
}
