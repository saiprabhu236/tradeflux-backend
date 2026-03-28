package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.model.NseSymbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NseSymbolStore {

    private List<NseSymbol> symbols = new ArrayList<>();

    // Called by scheduler
    public void updateSymbols(List<NseSymbol> newSymbols) {
        this.symbols = new ArrayList<>(newSymbols);
    }

    // Called at startup (optional)
    public void loadInitialSymbols(List<NseSymbol> initial) {
        this.symbols = new ArrayList<>(initial);
    }

    // Search endpoint uses this
    public List<NseSymbol> search(String query) {
        String q = query.toLowerCase();

        log.info("Searching for: {}", q);
        log.info("Total symbols loaded: {}", symbols.size());

        List<NseSymbol> results = symbols.stream()
                .filter(s -> {
                    boolean match = s.symbol().toLowerCase().contains(q)
                            || s.name().toLowerCase().contains(q);

                    if (match) {
                        log.info("Matched: {} - {}", s.symbol(), s.name());
                    }

                    return match;
                })
                .collect(Collectors.toList());

        log.info("Total matches: {}", results.size());
        return results;
    }

    public List<NseSymbol> getAll() {
        return symbols;
    }
}