package com.sai.finance.finance_manager.explore.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ThemeUniverseLoader {

    private final Map<String, List<String>> themeToSymbols;

    public ThemeUniverseLoader(ObjectMapper objectMapper) {
        this.themeToSymbols = loadThemes(objectMapper);
    }

    private Map<String, List<String>> loadThemes(ObjectMapper objectMapper) {
        try {
            ClassPathResource resource = new ClassPathResource("themes.json");

            try (InputStream is = resource.getInputStream()) {
                Map<String, List<String>> data =
                        objectMapper.readValue(is, new TypeReference<Map<String, List<String>>>() {});
                System.out.println("Loaded themes: " + data.keySet());
                return data;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public Set<String> getAllThemes() {
        return themeToSymbols.keySet();
    }

    public List<String> getSymbolsForTheme(String theme) {
        return themeToSymbols.getOrDefault(theme, Collections.emptyList());
    }

    public boolean themeExists(String theme) {
        return themeToSymbols.containsKey(theme);
    }
}
