package com.sai.finance.finance_manager.marketdata.util;

public class SymbolMapper {

    public static String toYahooSymbol(String nseSymbol) {

        if (nseSymbol == null || nseSymbol.isBlank()) {
            return null;
        }

        nseSymbol = nseSymbol.toUpperCase();

        // 1. Handle indices
        if (nseSymbol.equals("NIFTY 50")) return "^NSEI";
        if (nseSymbol.equals("NIFTY BANK")) return "^NSEBANK";

        // 2. If already ends with .NS, do NOT append again
        if (nseSymbol.endsWith(".NS")) {
            return nseSymbol;
        }

        // 3. Default: add .NS for all NSE stocks
        return nseSymbol + ".NS";
    }
}