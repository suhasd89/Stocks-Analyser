# HADC HA Swing Scripts

Files:

- `hadc_ha_swing_indicator.pine`: chart indicator with B/S/H/R labels and alert conditions.
- `hadc_ha_swing_strategy.pine`: Strategy Tester companion using the same signal engine.

## How To Use

1. Open TradingView Pine Editor.
2. Paste `hadc_ha_swing_indicator.pine`, save, and add it to the chart.
3. Use a daily chart for positional trades, or keep `Signal timeframe` set to `D`.
4. Create alerts on `HADC Strong Buy`, `HADC Strong Sell`, `HADC Hold / Partial Profit`, and `HADC Reversal Risk`.
5. Set alerts to trigger on bar close. The PDF specifically says trades should be taken after the selected candle closes.

## Signal Meaning

- Deep green `B`: strong buy, trend start.
- Light green `B`: buy trend continuation or hold long.
- Deep red `S`: strong sell, bearish trend start or long exit.
- Light red `S`: bearish trend continuation or hold short.
- `H`: consolidation/hold zone. Consider partial profit booking.
- `R`: reversal risk. Consider larger reduction or exit.

## Backtesting Notes

Use `hadc_ha_swing_strategy.pine` on a normal candlestick chart, not a Heikin Ashi chart. The script internally requests Heikin Ashi values for signals, but Strategy Tester fills should stay on real market candles. TradingView documents that Heikin Ashi OHLC values are synthetic and are not suitable as actual order prices.

Suggested first test settings for daily Indian equities:

- `Trade direction`: `Long only`
- `Minimum ADX for trend`: `18`
- `Fast EMA`: `21`
- `Slow EMA`: `55`
- `ATR reversal multiplier`: `2.5`
- `Use H/R partial exits`: enabled
- `Pyramid on continuation B/S`: disabled initially

For Nifty 100 swing trades, start with these defaults, then tune ADX and ATR multiplier symbol-by-symbol. Increase ADX or ATR multiplier to reduce signals; decrease them to get earlier but noisier entries.
