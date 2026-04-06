package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.model.MarketStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class MarketStatusService {

    // NSE timings (IST)
    private static final LocalTime PRE_OPEN_START = LocalTime.of(9, 0);
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);
    private static final LocalTime POST_CLOSE_END = LocalTime.of(16, 0);

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Getter
    private final Set<MonthDay> holidays = new HashSet<>();

    public MarketStatusService() {
        // Add fixed-date holidays (example set, adjust as needed)
        holidays.add(MonthDay.of(1, 26));   // Republic Day
        holidays.add(MonthDay.of(8, 15));   // Independence Day
        holidays.add(MonthDay.of(10, 2));   // Gandhi Jayanti
        holidays.add(MonthDay.of(5,1)); // Labours day
        // Add more NSE holidays as needed
    }

    public MarketStatus getCurrentStatus() {
        ZonedDateTime nowIst = ZonedDateTime.now(IST);
        DayOfWeek dow = nowIst.getDayOfWeek();
        LocalTime time = nowIst.toLocalTime();
        MonthDay md = MonthDay.from(nowIst.toLocalDate());

        // Weekend
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return MarketStatus.WEEKEND;
        }

        // Holiday
        if (holidays.contains(md)) {
            return MarketStatus.HOLIDAY;
        }

        // Time-based status
        if (time.isBefore(PRE_OPEN_START)) {
            return MarketStatus.CLOSED;
        } else if (!time.isBefore(PRE_OPEN_START) && time.isBefore(MARKET_OPEN)) {
            return MarketStatus.PRE_OPEN;
        } else if (!time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE)) {
            return MarketStatus.OPEN;
        } else if (!time.isBefore(MARKET_CLOSE) && time.isBefore(POST_CLOSE_END)) {
            return MarketStatus.POST_CLOSE;
        } else {
            return MarketStatus.CLOSED;
        }
    }

    public boolean isMarketOpenNow() {
        return getCurrentStatus() == MarketStatus.OPEN;
    }
}