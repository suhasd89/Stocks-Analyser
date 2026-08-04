# Indian Multibagger Discovery Strategy

Last refreshed: 2026-05-14

This is a research workflow for finding Indian small/mid-cap companies that may deserve deeper study. It is not buy/sell advice. SEBI's investor charter stresses reading documents, knowing risks, and avoiding promises of huge profits: https://investor.sebi.gov.in/Investor-charter.html

## Framework

The transcript's useful idea is not "buy fast-growing stocks." The useful idea is: find a major demand shift, map the second-order suppliers, then confirm that the numbers and cash flows support the story.

Use this funnel:

1. Theme discovery from primary sources
   - UPI/digital payments: PIB reports FY2025-26 UPI transaction value above Rs 314 lakh crore and 85% share of India's digital payments: https://www.pib.gov.in/PressReleasePage.aspx?PRID=2257087
   - Smart meters: RDSS envisages 25 crore smart meters: https://pib.gov.in/PressReleasePage.aspx?PRID=1907719
   - Rail wagons: Indian Railways reported 41,929 wagons produced in FY2024-25: https://www.pib.gov.in/PressReleasePage.aspx?PRID=2118737
   - Defence indigenisation: use MoD DAP/IDDM documents plus company order disclosures: https://www.ddpmod.gov.in/sites/default/files/2024-02/dap-2020-11-nov-21_0_0.pdf

2. Second-order map
   - Smart meters: meter OEMs, enclosures, AMI software, RF mesh, installation, testing.
   - Railways: springs, brakes, wheels, forgings, bearings, couplers, interiors.
   - Defence: simulators, counter-drone systems, radars, electro-optics, harnesses, EMS, embedded software.
   - Digital public infrastructure: payment processors, registrars, compliance platforms, identity/trust services.
   - Premium consumption: brand owners, distillers, bottlers, packaging, distribution.

3. Initial quantitative filter
   - Revenue growth above 25% YoY, with preference for 40%+ or multi-year acceleration.
   - PAT/EBITDA growth faster than revenue, showing operating leverage.
   - Margin expansion or clear mix shift into better products.
   - Mainboard small/mid-cap by default.
   - Avoid fragile liquidity, unexplained promoter pledge, heavy dilution, or stretched valuation without execution proof.

4. Quality filter
   - Operating cash flow should broadly track PAT over a cycle.
   - Receivables and inventory should not grow materially faster than sales without a convincing explanation.
   - Check auditor comments, related-party transactions, contingent liabilities, and frequent capital raises.
   - Confirm that order book converts into revenue, not only announcements.

5. Buy-and-track discipline
   - Track thesis, numbers, valuation, and invalidation points before entry.
   - Average up only after verified delivery.
   - Do not average down just because price fell.
   - Reduce/exit when growth breaks, cash conversion weakens, sector tailwind changes, valuation gets extreme, or governance weakens.

## Scoring Model

Total score: 100.

- Sector tailwind: 20
- Second-order/niche positioning: 15
- Revenue and PAT acceleration: 20
- Cash-flow and balance-sheet quality: 15
- Management/governance: 10
- Valuation versus growth: 10
- Liquidity and execution risk: 10

Classification:

- 80+: high-priority research
- 65-79: track quarterly
- 50-64: theme watch only
- Below 50: reject

## App Watchlist

The app now has a `MULTIBAGGER` watchlist at `backend/src/main/resources/watchlists/multibagger.yml`. It is intentionally a tracking list for the existing V20 scanner. V20 price signals are only timing/price-action alerts; the fundamental framework below still needs manual review.

| Symbol | Theme | Why It Is On The List | Score | Status |
| --- | --- | --- | ---: | --- |
| HPL | Smart meters | Smart meter order book and metering segment growth tied to RDSS rollout. | 78 | Track quarterly |
| GENUSPOWER | Smart meters | Large smart-metering exposure with FY25 revenue/profit acceleration. | 80 | High-priority research |
| RMC | Smart meter second-order | Metering/power infra adjacency; smaller beneficiary profile means higher execution and liquidity risk. | 75 | Track quarterly |
| ZENTEC | Defence niche | Simulators/counter-drone systems map to defence indigenisation and warfare-tech tailwinds. | 78 | Track quarterly |
| DATAPATTNS | Defence electronics | Defence electronics revenue growth and clean niche positioning. | 74 | Track quarterly |
| PARAS | Defence/space optics | Defence and space component exposure; needs order conversion tracking. | 68 | Track quarterly |
| FRONTSP | Railway second-order | Springs are a component-level railway/wagon beneficiary. Liquidity risk needs monitoring. | 72 | Track quarterly |
| JWL | Rail wagons | Direct wagon platform exposure; included as benchmark against second-order rail names. | 67 | Track quarterly |
| PGEL | EMS/consumer durables | Strong FY25 revenue growth from RACs/washing machines and contract manufacturing. | 76 | Track quarterly |
| KAYNES | EMS/electronics | High-growth EMS platform; watch valuation and working capital. | 75 | Track quarterly |
| SYRMA | EMS/electronics | EMS exposure with growth, but less explosive than the best candidates. | 64 | Theme watch |
| AVALON | EMS turnaround | FY25 operating leverage and PAT acceleration from a lower base. | 70 | Track quarterly |
| ZAGGLE | SaaS fintech | Spend-management/payment platform growth; capital-light angle fits transcript pattern. | 77 | Track quarterly |
| PROTEAN | Digital infra | Public digital infrastructure adjacency; needs faster growth confirmation. | 65 | Track quarterly |
| KFINTECH | Capital-market infra | Asset-light registrar/platform business with strong margins and steady growth. | 72 | Track quarterly |
| BLS | Gov-tech services | Visa/citizen-service outsourcing with FY25 margin expansion. | 73 | Track quarterly |
| PICCADIL | Premium consumption | Indri-led premium spirits thesis, but FY25 growth was moderate after a big rerating. | 70 | Track quarterly |
| TI | Premium spirits turnaround | Balance-sheet and brand turnaround; track acquisition integration and growth quality. | 66 | Track quarterly |

## Double-Check Sources

Primary company and exchange/aggregator sources used for the v1 watchlist:

- HPL Electric FY2024-25 annual report: https://investor.hplindia.com/photos/investor-pdf/Annual-Report-2024-25.pdf
- Genus Power FY2024-25 annual report: https://genuspower.com/wp-content/uploads/2025/09/AR_2025.pdf
- RMC Switchgears FY2024-25 annual report: https://rmcindia.in/wp-content/uploads/2025/11/ANNUAL-REPORT-2024-25-.pdf
- Zen Technologies annual reports: https://www.zentechnologies.com/annual-reports
- Data Patterns financial reports: https://www.datapatternsindia.com/investors/financials.php
- Paras Defence investors page: https://parasdefence.com/investors
- Frontier Springs FY2024-25 annual report: https://frontiersprings.co.in/downloads/corporate-announcements/2025-26/Annual-Report-including-AGM-Notice-for-FY-2024-25.pdf
- Jupiter Wagons FY2024-25 annual report: https://jupiterwagons.com/annual-report-for-the-fy-2024-25/
- PG Electroplast FY2024-25 annual report: https://pgel.in/assets/images/annula_report/PGEL_Annual_Report_2024-25.pdf
- Kaynes Technology FY2024-25 annual report: https://www.kaynestechnology.co.in/doc/Regulation-46-of-sebi-lodr-regulation/Annual%20Report%20FY2024-25.pdf
- Syrma SGS FY2024-25 annual report: https://www.syrmasgs.com/investor-relations/wp-content/uploads/2025/08/Annual-Report-2025.pdf
- Avalon Technologies investors page: https://www.avalontec.com/en-au/investors/
- Zaggle FY2024-25 annual report: https://ir.zaggle.in/wp-content/uploads/2025/08/annual-report-2024-25.pdf
- Protean eGov financial reports: https://www.proteantech.in/financial-reports/
- KFin Technologies FY2024-25 annual report: https://investor.kfintech.com/wp-content/uploads/2025/08/KFintech_Annual-Report_FY-2024-25.pdf
- BLS International FY25 performance release: https://www.blsinternational.com/assets/pdf/press-release/Press-Release-Financial-Performance-of-BLS-International-Services-Limited-for-Quarter-4-ended-on-March-31-2025-issued-on-May-15-2025.pdf
- Piccadily annual reports/results page: https://piccadily.com/annual-report
- Tilaknagar FY2024-25 annual report via NSE archive: https://nsearchives.nseindia.com/corporate/gthakur_08092025192621_TI_Annual_Report_2024_25.pdf

Useful cross-check aggregators:

- StockAnalysis India pages for annual revenue/history checks: https://stockanalysis.com/quote/nse/
- Equitymaster annual report analysis pages for quick FY25 comparisons: https://www.equitymaster.com/research-it/annual-results-analysis/

## Deliberate Exclusions And Rechecks

- NPST: strong transcript example, but treat as a recheck because it started on SME/Emerge and has had a major rerating. Include only if the user's universe expands beyond mainboard small/mid-cap.
- DCXINDIA: defence EMS theme fits, but FY25 revenue and profit declined; keep out until growth resumes.
- GLOBUSSPR: premium spirits theme fits, but FY25 profit quality was weak versus the desired acceleration filter.
- SULA: premium consumption theme fits, but recent growth does not meet the high-growth filter.
- RKFORGE: rail/forging theme fits, but FY25 growth was not strong enough for this specific multibagger screen.

## Quarterly Review Checklist

For every result season, update each candidate with:

- Revenue growth, EBITDA growth, PAT growth, and margin movement.
- Operating cash flow versus PAT.
- Receivable days and inventory days.
- Order book and execution commentary.
- Promoter holding/pledge and auditor notes.
- Valuation versus expected growth.
- Whether the original thesis is stronger, unchanged, weaker, or broken.
