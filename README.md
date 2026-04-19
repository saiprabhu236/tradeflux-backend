
<p align="left">

<img src="https://img.shields.io/badge/Java-17-red?logo=openjdk" />
<img src="https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot" />
<img src="https://img.shields.io/badge/PostgreSQL-Database-blue?logo=postgresql" />
<img src="https://img.shields.io/badge/WebSocket-Real--Time-orange?logo=websocket" />
<img src="https://img.shields.io/badge/License-MIT-yellow" />
<img src="https://img.shields.io/github/stars/saiprabhu236/tradeflux-backend?style=social" />
<img src="https://img.shields.io/github/forks/saiprabhu236/tradeflux-backend?style=social" />

</p>


# TradeFlux Backend — Trading Simulation Platform  
A production‑grade backend for a **paper‑trading platform** built with **Spring Boot**, featuring **real NSE market data (15‑minute delayed)**, **virtual money**, and a **full simulated trading engine**.

This project demonstrates strong backend engineering skills in:

- Real‑time systems  
- Market data ingestion  
- WebSocket broadcasting  
- Trading engine design  
- Clean architecture  
- Security (JWT + rate limiting)  
- Database modeling (PostgreSQL)  
- High‑performance in‑memory computation  

---

## 1. Executive Summary

The TradeFlux Backend is a **full trading backend simulation** that uses:

- **Real NSE prices** (via Yahoo Finance, delayed by ~15 minutes)  
- **Synthetic tick engine** for live movement  
- **Virtual money** for paper trading  
- **Simulated order execution** (market, limit, SL, SL‑M)  
- **Portfolio + holdings + P&L**  
- **Explore module** with 500‑stock universe  
- **Watchlist, sparkline charts, metrics, OHLC**  
- **Rate‑limited, JWT‑secured APIs**  

This backend is designed to feel like a **Zerodha/Groww‑style trading system**, but without real brokerage integration.

---

## 2. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java |
| Framework | Spring Boot |
| Security | Spring Security, JWT |
| Database | PostgreSQL |
| ORM | JPA / Hibernate |
| Real‑Time | WebSocket |
| Market Data | Yahoo Finance (unofficial API) |
| Rate Limiting | Bucket4j |
| Build Tool | Maven |

---

## 3. Architecture Overview

The system follows a **clean, layered architecture**:

Presentation Layer

REST Controllers

WebSocket Handlers

Service Layer

Business Logic

Validation

Workflows

Domain Layer

Entities

DTOs

Domain Models

Data Layer

JPA Repositories

PostgreSQL

Integration Layer

Yahoo Finance Client

Schedulers

Tick Engine

WebSocket Broadcaster

Rate Limiting Filter

Code

See `ARCHITECTURE.md` for diagrams and detailed flows.

---

## 4. Market Data System (Real + Simulated)

### 4.1 MarketDataService (Real Price Fetcher)
Used for:

- Stock detail page  
- Charts  
- Metrics  
- Watchlist  
- Holdings  
- Portfolio  

**Key points:**

- Fetches **real NSE prices for every stock available in market** from Yahoo Finance  
- Prices are **delayed by ~15 minutes** (Yahoo restriction)  
- Uses `YahooFinanceClient`  
- Caches responses (TTL 5–60 seconds)  
- Provides:
  - LTP  
  - Previous close  
  - OHLC  
  - Volume  
  - 52‑week high/low  

---

### 4.2 Explore Module (500‑Stock Mini‑Universe)
Used for:

- Top gainers  
- Top losers  
- Trending  
- Sector movers  
- Themes (EV, Infra, Make in India, etc.)  

**Architecture:**

- 500 NSE symbols loaded from `universe.json`  
- Scheduler runs **every 120ms**  
- Fetches **1 symbol per cycle**  
- Updates all 500 symbols **every 60 seconds**  
- Avoids Yahoo rate limits  
- Synthetic ticks provide **live movement**  

**Data Flow:**

YahooScheduler (120ms)
↓
SymbolStateStore (500 stocks)
↓
TickEngine (1s)
↓
SnapshotService (change%, OHLC)
↓
ExploreService
↓
ExploreController

Code

---

## 5. Real‑Time Engine

### TickEngine (1‑second interval)
Simulates live price movement using:

- Volatility curve  
- Random walk  
- Mean reversion  
- Spread simulation  
- Circuit limits  

### WebSocket Broadcasting
- Per‑session subscriptions  
- No duplicate sessions  
- No memory leaks  
- Sends:
  - tickPrice  
  - bid/ask  
  - spread  
  - change%  
  - timestamp  

---

## 6. Trading Engine (Orders + Execution)

### Supported Order Types
- Market  
- Limit  
- Stop‑Loss (SL)  
- Stop‑Loss Market (SL‑M)  

### Matching Engine
Runs every second:

@Scheduled(fixedRate = 1000)
matchOrders()

Code

Executes based on:

- LTP  
- Limit price  
- Trigger price  

### Wallet Integration
- BUY → debit wallet  
- SELL → credit wallet  

### Holdings Integration
- Weighted average price  
- Auto‑delete when qty = 0  
- Real‑time P&L  

### Portfolio Module
- Total portfolio value  
- Today’s P&L  
- Unrealized P&L  
- Per‑stock breakdown  

---

## 7. Wallet Module

- Auto‑created on registration  
- Immutable transaction ledger  
- Derived balance  
- Deposit / withdraw  
- Transaction history with filters  

---

## 8. Watchlist Module

- Add/remove symbols  
- Sparkline (1‑day candles)  
- Sorting  
- Name resolution  
- Real‑time updates via tickPrice  

---

## 9. Security & Rate Limiting

### JWT Authentication
- Stateless  
- Access + Refresh tokens  
- BCrypt password hashing  

### Rate Limiting (Bucket4j)
- Global IP‑based limit  
- 20 requests/min (configurable)  
- Applied **before** JWT filter  
- Protects:
  - OTP  
  - Auth  
  - Market  
  - Orders  
  - Portfolio  
  - Explore  

---

## 10. Database (PostgreSQL Only)

Tables include:

- users  
- otp  
- refresh_tokens  
- wallet  
- wallet_transactions  
- orders  
- holdings  

See `DATABASE_SCHEMA.md` for full ER diagram.

---

## 11. Development Timeline

1. Auth + OTP  
2. JWT + Refresh Tokens  
3. Wallet Ledger  
4. Market Data Core  
5. Real‑Time Engine  
6. Explore Module  
7. Watchlist  
8. Holdings  
9. Portfolio  
10. Orders + Matching Engine  
11. Rate Limiting  
12. Documentation + Cleanup  

---

## 12. How to Run

### Prerequisites
- Java 17+  
- Maven  
- PostgreSQL running locally  

### Commands

``bash
mvn clean install
mvn spring-boot:run
13. API Documentation
See:

API_DOC.md


Author
  -- Sai Prabhu Dasari  
  -- Backend Engineer — Trading & Market Data Systems
