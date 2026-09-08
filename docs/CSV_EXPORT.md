# WorkTime CSV export format

**Version:** 1  
**Scope:** one calendar month

## File behavior

- Export is user-initiated from the detailed month statistics sheet.
- Android's system document picker chooses the destination and filename.
- WorkTime writes only to the URI returned by the picker.
- Export does not modify local Room data.
- No broad storage permission is required or requested.
- Encoding: UTF-8.
- Separator: comma (`,`).
- Line ending: CRLF.
- Fields containing comma, quote or line break are quoted; embedded quotes are doubled.

## Columns

| Column | Meaning |
|---|---|
| `record_type` | `metadata` or `day` |
| `period` | Exported month as ISO `YYYY-MM` |
| `currency` | ISO 4217 currency code used for monetary values |
| `date` | Local calendar date as ISO `YYYY-MM-DD`; blank for metadata row |
| `day_type` | `WORK`, `DAY_OFF`, `VACATION`, or `SICK` |
| `worked_minutes` | Paid/worked duration in integer minutes |
| `overtime_minutes` | Overtime indicator in integer minutes |
| `effective_hourly_rate` | Per-day override if present, otherwise base rate; decimal major currency units; blank when unavailable/not applicable |
| `earnings` | Deterministic earnings for that day in decimal major currency units; blank when the rate is unavailable/not applicable |
| `note` | User note, CSV-escaped without normalization |

## Metadata row

Every export contains exactly one `metadata` row immediately after the header. This keeps the requested period and currency explicit even when the month has no day entries.

Example:

```text
record_type,period,currency,date,day_type,worked_minutes,overtime_minutes,effective_hourly_rate,earnings,note
metadata,2026-09,EUR,,,,,,,
```

## Day rows

Day rows are sorted by local date ascending. Entries outside the requested month are excluded even if accidentally supplied to the exporter.

Example:

```text
day,2026-09,EUR,2026-09-08,WORK,480,0,15.00,120.00,Обычная смена
```

For non-work day types, worked/overtime minutes remain numeric zero and rate/earnings are blank unless product semantics explicitly change in a future format version.

## Money semantics

Money is stored internally as integer minor units. CSV converts rate and earnings to exact decimal major units using the selected currency's fraction digits; it does not use binary floating point or locale-dependent decimal separators.

Overtime multipliers are not inferred. Exported earnings use the same documented calculation as the in-app month summary.
