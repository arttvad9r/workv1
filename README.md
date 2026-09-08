# WorkTime

Современное Android-приложение для личного учёта рабочего времени, заработка и переработок.

Главный продуктовый референс — приложение **«Табель учета рабочего времени»** (RavenDEV), но проект не является его визуальной копией. Цель — сохранить сильную calendar-first модель учёта и убрать перегруженность, мелкие элементы, длинные формы и лишние действия на основном экране.

## Статус

Проект находится в активной разработке. Рабочий offline-first вертикальный срез уже включает:

- календарь месяца и быстрый ввод часов по дню;
- переработку и заметки;
- локальное хранение в Room без обязательного аккаунта;
- базовую почасовую ставку, валюту и необязательную ставку для отдельного дня;
- месячную сводку по часам, переработке и заработку;
- сохранение настроек через DataStore;
- adaptive Compose UI, edge-to-edge, light/dark theme;
- accessibility checks и проверки font scale 200%;
- unit/instrumentation CI на Android API 36;
- устанавливаемый debug APK как artifact каждого успешного CI build.

## Документация

- [`docs/PRODUCT_RESEARCH.md`](docs/PRODUCT_RESEARCH.md) — аудит референса и аналогов, продуктовые выводы.
- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) — продуктовая модель, сценарии и границы MVP.
- [`docs/UX_UI_SPEC.md`](docs/UX_UI_SPEC.md) — структура экранов, визуальная система, компоненты и motion.
- [`docs/TECHNICAL_SPEC.md`](docs/TECHNICAL_SPEC.md) — стек, архитектура, данные, тестирование, безопасность и quality gates.
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — этапы реализации и критерии готовности.

## Базовые принципы

1. **Быстрый ввод важнее количества функций на экране.**
2. **Offline-first и без обязательного аккаунта.**
3. **Редкие функции скрыты до момента, когда они нужны.**
4. **Календарь показывает только данные, которые полезны для быстрого чтения.**
5. **Денежные и временные расчёты детерминированы и покрываются тестами.**
6. **Никакой обязательной аналитики, рекламы или сетевого слоя в MVP.**
7. **Адаптивность, edge-to-edge и accessibility закладываются сразу, а не после релиза.**

## Сборка и установка

Проект использует repository Gradle Wrapper 9.6.1:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Debug APK после локальной сборки:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Установка через ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

В GitHub Actions каждый успешный `Android CI` build также публикует artifact `worktime-debug-<commit-sha>` с готовым `app-debug.apk`.

## Android target

Проект ориентируется на актуальный Android-стек 2026 года: Kotlin, Jetpack Compose, Material 3/design system, UDF, Room и DataStore. Текущий baseline: `compileSdk 37`, `targetSdk 36`, `minSdk 26`, JDK 17, AGP 9.4 и Gradle 9.6.1. Runtime instrumentation выполняется на Android API 36 x86_64 emulator.
