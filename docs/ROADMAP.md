# WorkTime — implementation roadmap

**Дата:** 2026-09-08

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
- [x] CI build/lint/unit tests.

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
- [x] persistence failure has user-safe error path.

## Phase 2 — Settings + polished core

- [ ] settings destination;
- [x] default hourly rate;
- [x] currency;
- [x] actual earnings in summary;
- [ ] optional day rate override;
- [ ] Navigation 3 integration when justified by destinations;
- [ ] predictive Back verification;
- [ ] large-font polish;
- [ ] TalkBack pass;
- [x] expanded two-pane layout;
- [ ] screenshot tests;
- [x] UI tests for save/edit/delete/relaunch/recreation.

## Phase 3 — Work patterns

- [ ] day types: work/off/vacation/sick;
- [ ] presets for 8/10/12h/custom shifts;
- [ ] optional start/end + break calculation;
- [ ] shift pattern generator (`2/2`, `3/3`, custom);
- [ ] preview before bulk apply;
- [ ] undo bulk changes;
- [ ] no auto-fill block on primary screen.

## Phase 4 — Reports and data portability

- [ ] detailed month/year statistics;
- [ ] CSV export;
- [ ] XLSX export;
- [ ] PDF report;
- [ ] Android share/document picker flows;
- [ ] backup/restore;
- [ ] export correctness tests;
- [ ] no broad storage permission.

## Phase 5 — Convenience surfaces

Only after core metrics/usability are stable:

- [ ] app widget;
- [ ] quick-add action;
- [ ] optional reminder;
- [ ] calendar/date shortcuts;
- [ ] optional timer/check-in/out.

## Phase 6 — Multiple work profiles

- [ ] schema migration to profile/workplace entity;
- [ ] profile switching without cluttering day entry;
- [ ] independent rates/currency where sensible;
- [ ] combined vs per-profile reports;
- [ ] migration tests from single-profile database.

## Phase 7 — Optional sync

Not started unless product need is proven.

- [ ] explicit sync architecture decision;
- [ ] no forced account for local-only users;
- [ ] conflict model;
- [ ] encryption/auth/privacy review;
- [ ] deletion/export requirements;
- [ ] Data Safety update.

## Verified build status

Последняя полностью проверенная runtime-ревизия: `e39eaba874688e58fb5ebeed2e0cbb563e6fdddd`.

GitHub Actions успешно выполняет:

- `testDebugUnitTest`;
- `lintDebug`;
- `assembleDebug`;
- `assembleDebugAndroidTest`;
- `connectedDebugAndroidTest` на Android API 36 x86_64 emulator.

Instrumentation-проверка использует реальную `MainActivity`, `AppContainer`, Room и DataStore. Проверены сохранение, редактирование, удаление с Undo, повторный запуск Activity и configuration recreation с сохранением релевантного UI-state.

Gradle Wrapper 9.6.1 сгенерирован самим Gradle; SHA-256 binary distribution и wrapper JAR проверены по официальному Gradle checksum reference. Следующий CI gate обязан работать через `./gradlew`, чтобы локальная и CI-сборки использовали одну версию Gradle.

## Explicitly deferred

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
