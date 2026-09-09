# WorkTime — implementation roadmap

**Дата:** 2026-09-10

## Definition of Done

Этап считается завершённым только когда одновременно выполнены:

- пользовательский flow работает, а не только нарисован;
- данные/состояние корректны после recreation/relaunch;
- есть проверки критической логики;
- UI не ломается на заявленной window/font matrix;
- CI не красный;
- документация отражает фактическое поведение.

## Phase 0 — Research & foundation

**Статус: выполнено / поддерживается по мере разработки**

- [x] аудит главного референса;
- [x] анализ аналогов и отзывов;
- [x] продуктовые принципы;
- [x] MVP/P1/P2 scope;
- [x] UX/UI spec;
- [x] architecture/technical spec;
- [x] Android 2026 target/toolchain baseline.

## Phase 1 — First vertical slice

**Цель:** приложение уже полезно для простого личного учёта.

### Foundation

- [x] Gradle/AGP/Kotlin/Compose project bootstrap;
- [x] Gradle Wrapper 9.6.1 с проверкой официальных SHA-256;
- [x] package/application/theme/resources;
- [x] edge-to-edge;
- [x] CI build/lint/unit tests;
- [x] устанавливаемый debug APK как artifact успешного CI build.

### Domain/data

- [x] `WorkDay` model;
- [x] Room database/DAO/entity;
- [x] repository;
- [x] month grid generator;
- [x] month summary calculator;
- [x] deterministic money calculator;
- [x] validation tests.

### UI

- [x] month pager;
- [x] month header;
- [x] compact summary;
- [x] calendar grid;
- [x] today/entry states;
- [x] explicit shortcut back to current month;
- [x] day edit bottom sheet;
- [x] save/update entry;
- [x] delete entry with Snackbar Undo;
- [x] Russian strings;
- [x] system light/dark support.

### Reliability

- [x] relaunch keeps data — verified on API 36 emulator with real Room/AppContainer;
- [x] configuration recreation keeps visible month/editor draft — verified on API 36 emulator;
- [x] invalid overtime cannot be saved;
- [x] persistence failure has user-safe error path;
- [x] Room schema migrations `1 -> 2 -> 3 -> 4 -> 5` preserve legacy rows, add nullable day-rate override/day type, move records to profile-scoped composite keys and add per-profile payment fields; migration paths are covered by instrumentation tests.

## Phase 2 — Settings + polished core

- [x] secondary payment settings surface without unnecessary navigation hierarchy;
- [x] default hourly rate;
- [x] currency;
- [x] settings persistence across Activity relaunch verified against real DataStore/Room profile storage;
- [x] actual earnings in summary;
- [x] optional day rate override with persistence and deterministic mixed-rate month calculation;
- [x] Navigation 3 applicability reviewed — deliberately not introduced while the app has one root destination plus modal surfaces; revisit when a real destination stack exists;
- [ ] manual predictive Back gesture verification — current Material 3 sheets use platform behavior and the root Activity does not intercept Back;
- [x] large-font polish — calendar, editor, payment settings, pattern generator and statistics have automated 200% font-scale coverage;
- [x] automated Compose accessibility checks for critical calendar/editor/settings/statistics surfaces on API 36;
- [ ] manual TalkBack pass;
- [x] expanded two-pane layout;
- [ ] screenshot tests — deferred while official Compose Preview Screenshot Testing remains experimental and its current alpha tooling has an active renderer regression;
- [x] UI tests for save/edit/delete/undo/relaunch/recreation/settings persistence/day-rate persistence.

## Phase 3 — Work patterns

- [x] day types: work/off/vacation/sick;
- [x] presets for 8/10/12h/custom shifts;
- [x] optional start/end + break calculation, including overnight shifts;
- [x] shift pattern generator (`2/2`, `3/3`, `5/2`, custom);
- [x] preview before bulk apply;
- [x] generated rows preserve existing records;
- [x] undo bulk changes removes only rows inserted by that bulk operation;
- [x] no auto-fill block on primary calendar screen — generator remains a secondary action.

## Phase 4 — Reports and data portability

- [x] detailed month statistics;
- [x] detailed year statistics with 12-month breakdown;
- [x] CSV month export;
- [x] XLSX month export — dependency-free production OOXML writer, opened and validated by Apache POI 5.5.1 in JVM tests only;
- [x] PDF month report — one-page A4 report generated with platform `PdfDocument` and validated with `PdfRenderer`;
- [ ] complete Android share/document-provider verification — exports and backup use Activity Result document contracts, but manual round-trip through representative external document providers is still pending;
- [x] versioned full backup/restore of all work profiles, WorkDay records, active profile and payment settings through the system document picker;
- [x] backup decoder remains backward-compatible with WTBK v1/v2; encoder writes current WTBK v3 with optional per-profile payment;
- [x] restore validates the entire backup before mutation and requires explicit destructive confirmation;
- [x] Room bulk replacement is transactional; cross-store runtime failure uses compensating rollback for Room + DataStore snapshots;
- [x] deterministic backup codec/compatibility tests, invalid-format rejection and rollback tests;
- [x] export correctness tests: deterministic month filtering/sorting, effective per-day rate/earnings, CSV escaping, XLSX workbook compatibility and PDF report-model validation;
- [x] automated PDF validity check through Android `PdfRenderer`;
- [x] automated check that the app requests no broad storage permission.

## Phase 5 — Convenience surfaces

Only after core metrics/usability are stable:

- [x] app widget — Glance 1.2.0, read-only current-month summary with adaptive compact/expanded content and tap-through to the app;
- [x] quick-add action — dynamic launcher shortcut «Сегодня» reuses the existing single Activity and opens the real editor for the current date;
- [x] optional reminder — opt-in local daily notification with persisted time, runtime notification permission, inexact alarm scheduling, reboot/time-zone rescheduling and no exact-alarm special access;
- [x] calendar/date shortcuts — tapping the month title opens the stable Material 3 DatePicker for 1900–2199 and jumps directly into the existing editor for the confirmed date;
- [x] calendar search/filter — secondary full-height Material 3 sheet searches existing entries in the visible month and active profile by trimmed case-insensitive note text, filters by day type, sorts by date and opens the existing day editor without changing Room schema or primary calendar flow;
- [x] optional timer/check-in/out — persisted local shift session without a foreground service; survives Activity relaunch, limits duration to the existing 24:00 editor model and only pre-fills worked duration for explicit user confirmation/save.

## Phase 6 — Multiple work profiles

- [x] schema migration to profile/workplace entity;
- [x] profile switching without cluttering day entry;
- [x] independent rates/currency where sensible;
- [x] combined vs per-profile month/year reports with currency-separated earnings;
- [x] migration tests from single-profile database;
- [x] backup/restore preserves all profiles, active selection and per-profile payment while reading legacy single-profile backups.

Combined reports deliberately aggregate time/day counters across profiles but never add amounts from different currencies into one number. Existing CSV/XLSX/PDF exports remain scoped to the selected profile to keep exported money semantics unambiguous.

## Phase 7 — Optional sync

Not started unless product need is proven.

- [ ] explicit sync architecture decision;
- [ ] no forced account for local-only users;
- [ ] conflict model;
- [ ] encryption/auth/privacy review;
- [ ] deletion/export requirements;
- [ ] Data Safety update.

## Verified build status

Phase 6 и P1 calendar search/filter реализованы до optional sync. Текущий release/runtime marker не привязан к вручную записанному commit SHA: ревизия считается runtime-проверенной только после полного зелёного обязательного Android CI на её фактическом head commit.

GitHub Actions выполняет через repository Gradle Wrapper:

- `testDebugUnitTest`;
- `lintDebug`;
- `assembleDebug`;
- `assembleDebugAndroidTest`;
- отдельный последовательный `bundleRelease` как production-like AAB smoke с R8/resource shrinking;
- `connectedDebugAndroidTest` на Android API 36 x86_64 emulator;
- отдельную Android 17 compatibility suite на API 37.1 `google_apis_playstore_ps16k` x86_64 с 16 KB page-size image.

Debug/unit/lint/androidTest assembly и release bundle выполняются двумя последовательными Gradle invocations. Это сохраняет полный release gate и исключает конкурентную запись одного Room schema export из `kspDebugKotlin` и `kspReleaseKotlin`, которая воспроизводимо давала обрезанный JSON при одновременном запуске variants.

API 36 instrumentation gate использует обычный `connectedDebugAndroidTest`: Package Manager должен быть готов, `AndroidTestRunner failed` считается ошибкой, а XML-результат должен содержать реально выполненные тесты. Android 17 API 37.1 проверяется отдельным direct-only path, потому что текущий AGP/UTP device-execution на доступном 16 KB Play Store image может завершаться `BUILD SUCCESSFUL` с zero-test XML и не даёт полезного runtime-доказательства. Android 17 job сначала собирает `assembleDebug` и `assembleDebugAndroidTest`, переводит Play Store image в offline/quiesced состояние, один раз устанавливает app/test APK, ждёт idle broadcast queues и запускает зарегистрированный `AndroidJUnitRunner` через `adb shell am instrument`. Play Store, Photos и Wellbeing отключаются только внутри этого CI emulator; GMS не отключается и лишь force-stop'ится для остановки first-boot background work. Gate проходит только при непустом успешном результате `OK (N tests)`, где `N > 0`. CI сохраняет instrumentation log, emulator diagnostics и при crash отдельный `android-instrumentation-logcat.log` для разбора ранних process/LMKD failures. Актуальный Android 17 direct-only run с calendar search/filter завершил 44 теста успешно.

Instrumentation-проверки используют реальные `MainActivity`, `AppContainer`, Room и DataStore. Покрыты сохранение/редактирование/удаление с Undo, relaunch, configuration recreation, day-rate override, day types, shift calculator, pattern preview/apply/undo, Room migrations, profile isolation/switching/per-profile payment, month/year statistics и combined profile reports, export surfaces, PDF generation/validity, multi-profile backup/restore against real Room + DataStore, destructive restore confirmation, widget receiver/provider metadata, dynamic quick-add shortcut, reminder permission/settings persistence и фактическое создание/отмена AlarmManager PendingIntent, manifest security для reminder, открытие stable Material 3 DatePicker из заголовка месяца и переход из него в реальный day editor, persisted shift timer start/relaunch/stop flow, calendar note search/day-type filtering с открытием существующей записи, accessibility основных поверхностей и доступность primary actions при font scale 200%, включая DatePicker dialog, timer controls, combined reports и calendar search sheet. Та же runtime suite используется для Android 17 compatibility. Glance JVM tests отдельно проверяют compact/expanded widget content и расчёт widget snapshot из существующей month-summary/money логики; reminder JVM tests проверяют расчёт следующего локального срабатывания; timer JVM tests проверяют расчёт целых минут и границы существующей модели длительности.

AndroidX Espresso явно закреплён на 3.7.0 для Android 17-compatible input injection. Gradle Wrapper 9.6.1 используется и локально, и в CI; SHA-256 binary distribution и wrapper JAR сверены с официальным Gradle checksum reference. GitHub Actions dependencies закреплены immutable commit SHA. `android-actions/setup-android` использует Node-24-compatible v4.0.1, compileSdk 37 устанавливается в CI явно как `platforms;android-37.0`, а Android 17 AVD создаётся из официального API 37.1 16 KB Play Store system image с явной ADB-auth/readiness проверкой до запуска tests. Успешный build проверяет release bundle path и публикует debug APK как CI artifact с ограниченным retention.

Официальный Compose Preview Screenshot Testing остаётся experimental; visual golden tests не включаются в стабильный gate, пока tooling не станет достаточно предсказуемым для проекта.

## Explicitly deferred

- strict cross-store crash-atomic restore journal (current Room + DataStore runtime failures use compensating rollback);
- reminder settings are device-specific and intentionally excluded from WTBK v3 so restore cannot silently re-enable notifications on another device;
- active shift timer state is device-specific and intentionally excluded from WTBK v3;
- employer/team SaaS;
- invoices;
- geofencing;
- location-based clock-in;
- Wear OS;
- advertising SDK;
- social features.

## UX regression checklist for every feature

Before merging a feature, answer:

1. Does it add anything to the main calendar screen?
2. Does that information need to be visible every time the app opens?
3. Can the same feature be contextual/secondary instead?
4. Does ordinary day entry still fit without unnecessary scrolling?
5. Did the day cell gain a second/third piece of data?
6. Does large font preserve the primary action?
7. Is there a standard Android component/API instead of a custom implementation?
8. Is new architecture/dependency solving a real problem?

If a feature makes the most common action slower, it needs a stronger justification than «больше функций».
