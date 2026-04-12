package com.sai.finance.finance_manager.explore.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;

@Component
public class ExploreUniverseLoader {

    private List<String> symbols;

    @PostConstruct
    public void load() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = new ClassPathResource("explore-universe.json").getInputStream();
        this.symbols = mapper.readValue(is, new TypeReference<List<String>>() {});
    }

    public List<String> getSymbols() {
        return symbols;
    }
}