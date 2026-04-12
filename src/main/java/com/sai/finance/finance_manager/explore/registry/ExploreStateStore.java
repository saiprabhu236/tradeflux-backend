package com.sai.finance.finance_manager.explore.registry;

import com.sai.finance.finance_manager.explore.model.ExploreSymbolState;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExploreStateStore {

    private final Map<String, ExploreSymbolState> stateMap = new ConcurrentHashMap<>();

    public ExploreSymbolState getOrCreate(String symbol) {
        return stateMap.computeIfAbsent(symbol, ExploreSymbolState::new);
    }

    public ExploreSymbolState get(String symbol) {
        return stateMap.get(symbol);
    }

    public Collection<ExploreSymbolState> getAll() {
        return stateMap.values();
    }
}