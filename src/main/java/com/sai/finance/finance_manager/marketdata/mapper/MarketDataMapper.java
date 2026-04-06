package com.sai.finance.finance_manager.marketdata.mapper;

import com.sai.finance.finance_manager.marketdata.dto.TickMessageDto;
import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import org.springframework.stereotype.Component;

@Component
public class MarketDataMapper {

    public TickMessageDto toTickMessage(String symbol, SymbolState state) {


        TickMessageDto dto = new TickMessageDto();

        dto.setSymbol(symbol.replace(".NS", ""));

        dto.setTickPrice(state.getLastTickPrice());
        dto.setRealPrice(state.getLastRealPrice());

        dto.setOpen(state.getOpen());
        dto.setHigh(state.getHigh());
        dto.setLow(state.getLow());
        dto.setClose(state.getClose());
        dto.setPreviousClose(state.getPreviousClose());

        dto.setChange(state.getChange());
        dto.setChangePercent(state.getChangePercent());

        dto.setVolume(state.getVolume());
        dto.setBidPrice(state.getBidPrice());
        dto.setAskPrice(state.getAskPrice());
        dto.setSpread(state.getSpread());

        dto.setTimestamp(System.currentTimeMillis());

        return dto;
    }
}