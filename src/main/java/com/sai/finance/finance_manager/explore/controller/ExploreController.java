package com.sai.finance.finance_manager.explore.controller;

import com.sai.finance.finance_manager.explore.dto.SectorOverviewDto;
import com.sai.finance.finance_manager.explore.dto.ThemeOverviewDto;
import com.sai.finance.finance_manager.explore.registry.ExploreStateStore;
import com.sai.finance.finance_manager.explore.service.ExploreService;
import com.sai.finance.finance_manager.explore.dto.ExploreStockDto;
import com.sai.finance.finance_manager.explore.service.SectorService;
import com.sai.finance.finance_manager.explore.service.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/explore")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;
    private final ExploreStateStore stateStore;
    private final SectorService sectorService;
    private final ThemeService themeService;

    // ------------------ Explore Lists ------------------

    @GetMapping("/top-gainers")
    public List<ExploreStockDto> getTopGainers(@RequestParam(defaultValue = "20") int limit) {
        return exploreService.getTopGainers(limit);
    }

    @GetMapping("/top-losers")
    public List<ExploreStockDto> getTopLosers(@RequestParam(defaultValue = "20") int limit) {
        return exploreService.getTopLosers(limit);
    }

    @GetMapping("/health")
    public String health() {
        int count = stateStore.getAll().size();
        return "Explore Module OK — Loaded symbols: " + count;
    }

    @GetMapping("/most-active")
    public List<ExploreStockDto> mostActive(@RequestParam(defaultValue = "20") int limit) {
        return exploreService.getMostActive(limit);
    }

    @GetMapping("/trending")
    public List<ExploreStockDto> trending(@RequestParam(defaultValue = "20") int limit) {
        return exploreService.getTrending(limit);
    }

    @GetMapping("/52week-high")
    public List<ExploreStockDto> weekHigh(@RequestParam(defaultValue = "20") int limit) {
        return exploreService.get52WeekHigh(limit);
    }

    @GetMapping("/52week-low")
    public List<ExploreStockDto> weekLow(@RequestParam(defaultValue = "20") int limit) {
        return exploreService.get52WeekLow(limit);
    }

    // ------------------ Sector Endpoints ------------------

    @GetMapping("/sector")
    public List<Map<String, Object>> listSectors() {
        return sectorService.getAllSectors();
    }

    @GetMapping("/sector/{sector}")
    public SectorOverviewDto sectorDetails(@PathVariable String sector) {
        return sectorService.getSectorOverview(sector);
    }

    // ------------------ Theme Endpoints ------------------

    @GetMapping("/themes")
    public ResponseEntity<List<String>> getAllThemes() {
        return ResponseEntity.ok(themeService.getAllThemes());
    }

    @GetMapping("/themes/{theme}")
    public ResponseEntity<ThemeOverviewDto> getTheme(@PathVariable String theme) {
        return themeService.getThemeOverview(theme)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
