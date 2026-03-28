package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.model.NseSymbol;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class NseSymbolLoader {

    private final NseSymbolStore symbolStore;

    public NseSymbolLoader(NseSymbolStore symbolStore) {
        this.symbolStore = symbolStore;
    }

    @Value("${nse.data.extracted-folder}")
    private String extractedFolder;

    private final List<NseSymbol> symbols = new ArrayList<>();

    public List<NseSymbol> getAllSymbols() {
        return symbols;
    }

    @PostConstruct
    public void loadAll() throws IOException {
        Path folder = Paths.get(extractedFolder);

        if (!Files.exists(folder)) {
            throw new IllegalStateException("Extracted folder not found: " + extractedFolder);
        }

        loadByPattern(folder, "pd");
        loadByPattern(folder, "etf");
        loadByPattern(folder, "sme");

        log.info("Loaded NSE symbols: {}", symbols.size());

        // ⭐ CRITICAL: Push into store
        symbolStore.loadInitialSymbols(symbols);
        log.info("Pushed {} symbols into NseSymbolStore", symbols.size());
    }

    private void loadByPattern(Path folder, String prefix) throws IOException {
        Optional<Path> latestFile = Files.list(folder)
                .filter(p -> p.getFileName().toString().toLowerCase().startsWith(prefix))
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

        if (latestFile.isEmpty()) {
            log.warn("No file found for prefix: {}", prefix);
            return;
        }

        Path csv = latestFile.get();
        log.info("Loading from: {}", csv);

        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            String headerLine = reader.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.split(",");
            Map<String, Integer> indexMap = buildIndexMap(headers);

            Integer symbolIdx = indexMap.get("SYMBOL");

            // NEW: Support SECURITY, NAME, NAME OF COMPANY
            Integer nameIdx = indexMap.getOrDefault("NAME OF COMPANY",
                    indexMap.getOrDefault("NAME",
                            indexMap.get("SECURITY")));

            if (symbolIdx == null || nameIdx == null) {
                log.warn("Missing SYMBOL or NAME/SECURITY column in: {}", csv);
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length <= Math.max(symbolIdx, nameIdx)) continue;

                String symbol = parts[symbolIdx].trim();
                String name = parts[nameIdx].trim();

                if (symbol.isEmpty() || name.isEmpty()) continue;

                symbols.add(new NseSymbol(symbol, name));
            }
        }
    }

    private Map<String, Integer> buildIndexMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim().toUpperCase(), i);
        }
        return map;
    }
}