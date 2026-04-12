package com.sai.finance.finance_manager.explore.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class SectorUniverseLoader {

    private final Map<String, List<String>> sectorToSymbols = new HashMap<>();
    private final Map<String, String> symbolToSector = new HashMap<>();

    @PostConstruct
    public void load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("sector-mapping.json");

        try (InputStream is = resource.getInputStream()) {
            Map<String, List<String>> data =
                    mapper.readValue(is, new TypeReference<Map<String, List<String>>>() {});
            sectorToSymbols.clear();
            sectorToSymbols.putAll(data);

            symbolToSector.clear();
            data.forEach((sector, symbols) ->
                    symbols.forEach(sym -> symbolToSector.put(sym, sector)));
        }
    }

    public Set<String> getAllSectors() {
        return Collections.unmodifiableSet(sectorToSymbols.keySet());
    }

    public List<String> getSymbolsForSector(String sector) {
        return sectorToSymbols.getOrDefault(sector, List.of());
    }

    public Optional<String> getSectorForSymbol(String symbol) {
        return Optional.ofNullable(symbolToSector.get(symbol));
    }
}