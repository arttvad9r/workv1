# WorkTime — Technical specification

**Версия:** 0.1  
**Дата:** 2026-09-08

## 1. Engineering goals

Проект должен быть:

- native Android;
- offline-first;
- Compose-first;
- state-driven;
- простым по структуре, пока продукт мал;
- тестируемым на уровне расчётов, state и persistence;
- корректным при process/activity recreation;
- adaptive и edge-to-edge;
- готовым к `targetSdk 36` и проверке на API 37;
- без ненужных permissions/network SDK в MVP.

## 2. Toolchain snapshot

Целевой baseline на 2026-09-08:

| Компонент | Выбор |
|---|---|
| Language | Kotlin |
| JDK | 17 |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.1 |
| Kotlin plugin/toolchain | 2.3.21 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 26 (product decision, пересмотреть при необходимости) |
| Compose | BOM `2026.08.00` / stable Compose 1.12 generation |
| Activity | 1.13.0 |
| Lifecycle | 2.10.0 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| Navigation 3 | 1.1.1, когда появится полноценная navigation graph |
| KSP | 2.3.11 |

### Почему minSdk 26

Это не требование «современности», а стартовый product trade-off:

- покрывает Android 8+;
- снижает количество legacy compatibility paths;
- `java.time` доступен платформенно;
- приложение персонального учёта не имеет требования поддерживать крайне старые устройства.

Если появятся данные аудитории, решение пересматривается. `minSdk` не повышается просто ради новой цифры.

## 3. AGP 9 и Kotlin

AGP 9+ использует built-in Kotlin. Поэтому проект не должен механически подключать legacy `org.jetbrains.kotlin.android` как в старых шаблонах.

Compose compiler plugin и KSP настраиваются согласно актуальной документации/совместимости toolchain.

Dependencies централизуются в Version Catalog (`gradle/libs.versions.toml`).

## 4. Modules

На MVP:

```text
:app
```

Внутри — package boundaries, а не искусственные Gradle modules.

Модули выделяются только если появляется реальная причина:

- reusable core;
- независимые крупные features;
- build-time benefit;
- ownership boundary;
- отдельная delivery unit.

Количество модулей не является quality metric.

## 5. Package structure

Начальная структура:

```text
com.arttvad.worktime
├── app/
│   └── WorkTimeApplication / app wiring
├── data/
│   ├── local/
│   │   ├── WorkTimeDatabase
│   │   ├── WorkDayDao
│   │   └── WorkDayEntity
│   ├── preferences/
│   └── repository/
├── domain/
│   ├── model/
│   └── calculation/
├── ui/
│   ├── calendar/
│   ├── settings/
│   ├── components/
│   └── theme/
└── MainActivity
```

`domain` содержит только реальную предметную логику — например расчёт заработка/summary. Trivial getters не оборачиваются в use case classes.

## 6. Architecture

```text
Compose UI
    ↓ events
ViewModel / screen state holder
    ↓
Repository interface/boundary
    ↓
Room / DataStore
```

Принципы:

- Unidirectional Data Flow;
- immutable UI state;
- `StateFlow`/`Flow`;
- `collectAsStateWithLifecycle()`;
- UI не вызывает DAO напрямую;
- reusable composables не получают ViewModel/NavController без необходимости;
- расчёты не живут в composable;
- важный state не хранится только в ephemeral UI object.

## 7. Dependency injection

MVP использует **manual constructor injection**.

Причины:

- небольшой object graph;
- один module;
- нет remote stack;
- меньше build/plugin complexity.

Ожидаемая схема:

```text
Application/AppContainer
├── Room database
├── WorkDayRepository
└── PreferencesRepository

ViewModel factory
└── получает repository/calculator dependencies
```

Hilt добавляется только когда object graph реально становится достаточно большим, чтобы ручная wiring начала мешать.

## 8. Persistence

### 8.1. Room

Room — source of truth для истории рабочих дней.

Начальная сущность:

```text
WorkDayEntity
- date: String / LocalDate converter, primary key
- workedMinutes: Int
- overtimeMinutes: Int
- hourlyRateMinorOverride: Long?   // можно подключить после первого slice
- note: String
- updatedAtEpochMillis: Long
```

Ограничения проверяются до записи:

```text
workedMinutes >= 0
overtimeMinutes >= 0
overtimeMinutes <= workedMinutes
```

Query месяца использует диапазон ISO date (`YYYY-MM-DD`), что сохраняет лексикографический порядок календарных дат.

### 8.2. DataStore

Для настроек:

- default hourly rate;
- currency code;
- theme preference;
- другие небольшие preferences.

DataStore не используется вместо relational history.

### 8.3. Migrations

Любое изменение Room schema:

- увеличивает version;
- имеет явную migration либо осознанную destructive policy только для pre-release development;
- перед production обязательно покрывается migration test.

## 9. Domain model

UI не должен зависеть от Room entity напрямую.

Минимальные модели:

```text
WorkDay
- date: LocalDate
- workedMinutes: Int
- overtimeMinutes: Int
- hourlyRateMinorOverride: Long?
- note: String

MonthSummary
- workedMinutes: Int
- overtimeMinutes: Int
- earningsMinor: Long?
```

`earningsMinor = null`, если расчёт невозможен из-за отсутствующей ставки. UI показывает `—`, а не вводящий в заблуждение ноль.

## 10. Money calculation

Никаких `Float`/`Double` для денег.

Внутреннее значение ставки и результата — `Long` minor units.

Базовая формула:

```text
earningsMinor = round(workedMinutes × hourlyRateMinor / 60)
```

Реализация должна предотвращать ненужную потерю точности и быть покрыта unit tests:

- 0 минут;
- ровный час;
- 30 минут;
- нецелое количество minor units после деления;
- большие допустимые значения;
- day override;
- отсутствующая ставка.

Overtime multiplier не применяется без явной настройки пользователя.

## 11. Time/date semantics

Для дневного табеля используем `LocalDate`, потому что запись относится к календарному дню пользователя.

`Instant`/UTC используется только для metadata вроде `updatedAt`.

Нельзя сохранять «8 сентября» как midnight UTC и затем восстанавливать дату через текущую timezone — это создаёт off-by-one-day ошибки.

## 12. ViewModel state

Пример `CalendarUiState`:

```text
CalendarUiState
- visibleMonth: YearMonth
- entries: Map<LocalDate, WorkDay>
- summary: MonthSummary
- selectedDate: LocalDate?
- isInitializing: Boolean
- message/error: UiMessage?
```

Events:

```text
PreviousMonth
NextMonth
MonthPageChanged
DaySelected(date)
SaveDay(input)
DeleteDay(date)
DismissEditor
Retry
```

ViewModel не должен становиться god-object: settings/reporting позже получают собственные state holders.

## 13. Calendar implementation

Предпочтение — Compose Foundation `HorizontalPager`, а не собственный gesture detector.

Причины:

- готовая semantics/gesture foundation;
- predictable page state;
- меньше ручной physics/velocity логики;
- проще testing.

Месяц вычисляется относительно anchor page либо хранится как explicit `YearMonth` state.

Day grid строится детерминированной pure-функцией, которую можно unit-test без Compose.

## 14. Navigation

MVP с home + modal editor не требует немедленно добавлять сложный navigation framework.

Когда появляется Settings как самостоятельный destination, использовать официальный современный Compose navigation stack; Navigation 3 — целевой кандидат.

Требования независимо от библиотеки:

- Back возвращает по понятной history;
- predictive back поддержан;
- state restoration предсказуем;
- нельзя использовать Back как reset-to-current-month.

## 15. Edge-to-edge

`targetSdk 36` предполагает корректный edge-to-edge behavior.

Правила:

- decor/background под system bars допустим;
- interactive content получает нужные insets локально;
- IME inset применяется к editor;
- cutout/rounded corners учитываются;
- не использовать blanket `systemBarsPadding()` на root как универсальный фикс.

## 16. Adaptive UI

Breakpoints определяются по window size.

- compact: calendar + modal bottom sheet;
- medium: увеличенная композиция без бессмысленного stretching;
- expanded: list/detail-like split — calendar слева, selected day/details справа.

Тестируем resize, rotation, split screen и tablet/desktop-windowing emulator.

## 17. Localization

- все user-facing strings в resources;
- первая локаль — Russian;
- code/identifiers/docs architecture — English-friendly;
- plural resources для `час/часа/часов`, `минута/...`;
- currency formatting через locale-aware APIs;
- UI не фиксируется под длину русской строки.

## 18. Accessibility

MUST для core:

- Material/Foundation controls по умолчанию;
- touch target >= 48 dp;
- TalkBack descriptions day cells;
- selected/today semantics;
- label/error semantics полей;
- font scaling;
- достаточный contrast;
- keyboard focus path на expanded/desktop layouts;
- meaning не передаётся только цветом.

## 19. Security / privacy

MVP не нуждается в dangerous permissions.

Baseline:

- `android:exported` явно проверяется;
- no cleartext network (network вообще отсутствует в core);
- нет secrets в APK;
- нет PII/work records в logs;
- backup/export должен быть user-initiated;
- внешние intents/document URIs валидируются;
- зависимости регулярно обновляются.

Если появится network layer, по умолчанию HTTPS + Network Security Configuration.

## 20. Background work

MVP не требует WorkManager или foreground service.

WorkManager добавляется только для persistent deferrable jobs, например:

- scheduled backup;
- deferred sync;
- retryable export/upload.

Обычное локальное сохранение экрана — coroutine, не WorkManager.

## 21. Performance

Измерять в release/release-like build.

Проверяем:

- cold start;
- first composition;
- month swipe jank;
- opening/closing editor;
- Room query latency;
- allocations при листании месяцев;
- process recreation.

R8 включён для release.

Macrobenchmark/Baseline Profile добавляются после появления измеряемого critical journey, а не как пустая инфраструктура в первый commit.

## 22. Testing strategy

### Unit

Обязательно:

- calendar month grid generation;
- duration validation;
- money calculation;
- month summary;
- formatters/parsers, где есть business semantics.

### Repository / persistence

- DAO insert/update/delete;
- month range query;
- Room migration tests до production schema changes.

### ViewModel

- initial month;
- selecting date;
- saving valid day;
- validation rejection;
- month navigation;
- repository error;
- process/state restoration where relevant.

### Compose UI

Critical flow:

```text
launch
→ tap day
→ enter duration
→ save
→ calendar shows entry
→ reopen day
→ values restored
```

### Screenshot tests

После стабилизации design system — matrix из `UX_UI_SPEC.md`.

## 23. CI quality gate

GitHub Actions для каждого PR/push:

```text
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

После расширения suite:

- static checks;
- connected/device tests в отдельном job;
- release assemble/bundle smoke;
- dependency/security update workflow.

Build должен воспроизводиться через Gradle Wrapper и JDK 17.

## 24. Release build

Production:

- AAB;
- `targetSdk >= 36`;
- R8/resource shrinking после проверки;
- release signing вне repository;
- versionCode increment;
- no debug logs/secrets;
- Android 17 compatibility smoke;
- Play Vitals monitoring после публикации.

## 25. Quality gate

Перед production должны быть истинны минимум:

### Platform

- [ ] targetSdk 36+
- [ ] API 37 compatibility checked
- [ ] predictive Back works
- [ ] edge-to-edge works
- [ ] resize/adaptive flows work

### Data correctness

- [ ] calculations covered by tests
- [ ] no floating-point money
- [ ] persistence survives process recreation
- [ ] migrations tested

### UI/accessibility

- [ ] large font critical flow
- [ ] TalkBack critical flow
- [ ] 48dp targets
- [ ] contrast
- [ ] light/dark if both claimed

### Security/privacy

- [ ] minimal permissions
- [ ] exported surface reviewed
- [ ] no secret/PII logs
- [ ] Data Safety matches actual behavior

### Performance/reliability

- [ ] release-like build tested
- [ ] no known crash/ANR in core flows
- [ ] month navigation is smooth on target device matrix

## 26. Official sources

Primary/current sources used for technical decisions:

- Compose-first: <https://developer.android.com/develop/ui/compose/first>
- Core app quality: <https://developer.android.com/docs/quality-guidelines/core-app-quality>
- Adaptive app quality: <https://developer.android.com/develop/adaptive-apps/quality-guidelines/adaptive-app-quality>
- Android 16 behavior changes: <https://developer.android.com/about/versions/16/behavior-changes-16>
- Android 17 migration: <https://developer.android.com/about/versions/17/migration>
- Target API requirements: <https://support.google.com/googleplay/android-developer/answer/11926878>
- Material 3 Compose: <https://developer.android.com/develop/ui/compose/designsystems/material3>
- Navigation 3 releases: <https://developer.android.com/jetpack/androidx/releases/navigation3>
- Room releases: <https://developer.android.com/jetpack/androidx/releases/room>
- DataStore releases: <https://developer.android.com/jetpack/androidx/releases/datastore>
- Lifecycle releases: <https://developer.android.com/jetpack/androidx/releases/lifecycle>
- Activity releases: <https://developer.android.com/jetpack/androidx/releases/activity>
- Compose BOM: <https://developer.android.com/develop/ui/compose/bom>
- AGP 9 built-in Kotlin: <https://developer.android.com/build/migrate-to-built-in-kotlin>
- AGP 9.4 release notes: <https://developer.android.com/build/releases/agp-9-4-0-release-notes>
- Screenshot testing: <https://developer.android.com/training/testing/ui-tests/screenshot>

Версии библиотек — snapshot, а не вечный норматив. Перед обновлением toolchain они перепроверяются по official release notes.
