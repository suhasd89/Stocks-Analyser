# India Pine Screener Usage

Script: `india_pine_screener.pine`

## TradingView Setup

1. Open TradingView, then open **Pine Editor**.
2. Paste the contents of `india_pine_screener.pine`.
3. Save it and add it to your favorites.
4. Create or open a watchlist containing Indian stocks, preferably `NSE:` or `BSE:` symbols.
5. Open **Products > Screeners > Pine**.
6. Select your Indian market watchlist.
7. Select the saved `India Pine Screener - V20 + Trend` indicator.
8. Use daily timeframe for the default 52-week settings.

## Useful Filters

- `Scan Pass` equals `1`: all active filters passed.
- `Buy Signal` equals `1`: final bullish candidate.
- `V20 Breakout` equals `1`: V20-style momentum condition triggered.
- `Trend OK` equals `1`: price is above the fast SMA, fast SMA is above slow SMA, and price is near the 52-week high.
- `Turnover Cr` greater than your liquidity requirement.
- `From 52W High %` less than your preferred distance from the 52-week high.
- `Vol / Avg Vol` greater than `1`: volume is above the recent average.

## Defaults

- V20 move: `20%`
- Fast SMA: `50`
- Slow SMA: `200`
- 52-week high lookback: `260` daily candles
- Minimum turnover: `Rs 5 crore`
- Minimum volume ratio: `1.0`

Adjust the defaults in the indicator settings before running the Pine Screener.
