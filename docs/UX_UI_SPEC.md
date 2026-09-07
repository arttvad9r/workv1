# WorkTime — UX/UI specification

**Версия:** 0.1  
**Дата:** 2026-09-08

## 1. Design direction

WorkTime должен выглядеть как современный Android utility, а не как уменьшенная бухгалтерская таблица.

Ключевые качества:

- спокойный;
- минималистичный;
- высокочитаемый;
- быстрый;
- тактильно понятный;
- без декоративного шума;
- с выразительностью только там, где она улучшает иерархию.

Material 3 используется как interaction/accessibility foundation. Material 3 Expressive можно применять точечно — shapes, transitions, emphasized containers — но не превращать экран в набор крупных декоративных карточек.

## 2. Информационная архитектура

### MVP

```text
WorkTime
├── Calendar / Month (home)
│   └── Day editor (modal bottom sheet)
└── Settings
```

Не нужен постоянный bottom navigation: в MVP нет трёх-пяти равноправных top-level destinations.

Когда появятся полноценные Reports/Profiles, навигация пересматривается по фактической структуре.

## 3. Home / Month screen

### Compact phone

```text
┌─────────────────────────────┐
│ Рабочее время          ⚙    │
│                             │
│        Сентябрь 2026        │
│     ‹                  ›    │
│                             │
│  176 ч     2 480 €    +8 ч  │
│  часы      заработок overtime│
│                             │
│ Пн Вт Ср Чт Пт Сб Вс        │
│ 31  1  2  3  4  5  6       │
│    8ч 8ч 8ч 8ч              │
│  7  8  9 10 11 12 13       │
│ 8ч 8ч                       │
│ ...                         │
│                             │
└─────────────────────────────┘
```

Это schematic layout, не pixel-perfect mockup.

### Порядок визуального веса

1. текущий/выбранный месяц;
2. календарная сетка;
3. месячная сводка;
4. settings/secondary actions.

Название приложения не должно занимать треть экрана крупным hero-заголовком.

## 4. Top app bar

Содержит:

- короткий title `Рабочее время`;
- settings action;
- в будущем overflow только если появляются действительно редкие глобальные действия.

Не содержит:

- export;
- auto-fill;
- несколько режимов расчёта;
- фильтры, если они не нужны постоянно.

Touch targets — минимум 48×48 dp.

## 5. Month navigation

### Primary

Горизонтальный swipe календаря.

### Secondary

Previous/next actions рядом с заголовком месяца или в доступной области header.

### Поведение

- переход ощущается пространственным: соседний месяц входит с соответствующей стороны;
- сводка меняется вместе с выбранным месяцем;
- today/selected-day state не «прыгает» случайно;
- rapid swipes не запускают тяжёлые блокирующие расчёты.

## 6. Month summary

Один компактный блок из трёх показателей:

```text
Отработано | Заработано | Переработка
```

### Визуальные правила

- главное число — `titleMedium`/`titleLarge` по доступной ширине;
- label — `labelMedium`/`bodySmall` с достаточным contrast;
- не использовать отдельный яркий цвет на каждый KPI;
- overtime может получать semantic accent только если значение ненулевое;
- сумма форматируется locale-aware;
- отсутствие ставки показывает `—`, а не фиктивные `0 ₽` как будто расчёт выполнен.

## 7. Calendar grid

### Структура

- 7 колонок;
- заголовки дней недели;
- 5–6 недель по необходимости;
- neighboring-month dates либо скрываются/приглушаются последовательно, либо не показываются — решение должно быть единым;
- вся day cell является touch target.

### Content budget одной day cell

Разрешено:

1. число даты;
2. одна короткая строка длительности;
3. один небольшой semantic indicator при необходимости.

Запрещено одновременно показывать:

- часы;
- overtime;
- заработок;
- ставку;
- несколько цветных маркеров;
- заметку.

Эти данные доступны после выбора дня.

### Day states

Минимальный набор:

- default;
- today;
- selected/pressed;
- has-entry;
- outside-current-month (если отображается);
- disabled — только если появится реальная причина.

Состояния не должны различаться только цветом. Используются сочетания container/outline/typography/semantics.

## 8. Day editor bottom sheet

### Phone

`ModalBottomSheet` с drag handle и понятной датой.

Первый экран без необходимости скролла при обычном font scale:

```text
Вторник, 8 сентября

Отработано
[ 8 ] ч  [ 00 ] мин

Переработка
[ 0 ] ч  [ 00 ] мин

Заметка                   >

[        Сохранить        ]
```

После сохранения sheet закрывается и календарь/summary обновляются из persistence state, а не optimistic fake data.

### Expanded width

Вместо модального sheet допустим supporting pane справа от календаря. Выбор дня обновляет pane, сохраняя контекст месяца.

## 9. Duration input

Для первой версии предпочтительнее два контролируемых numeric поля (`hours`, `minutes`), чем сложный custom wheel/time picker.

Причины:

- пользователь вводит **длительность**, а не время суток;
- Android time picker семантически решает другую задачу;
- numeric input проще для 8/10/12-часовых смен;
- легче обеспечить accessibility и тестирование.

Правила:

- `hours >= 0`;
- `0 <= minutes <= 59`;
- overtime не больше worked duration;
- пустое значение трактуется только по документированному правилу, без скрытых преобразований;
- ошибки inline, без Toast для валидации.

Позже можно добавить presets `8 ч`, `10 ч`, `12 ч`, если это ускорит реальное использование.

## 10. Notes

Заметка — secondary data.

Варианты:

- collapsed row `Заметка` → раскрывает text field;
- если заметка уже есть, показывается короткий preview.

Не держать большое многострочное поле открытым по умолчанию.

## 11. Settings

Первый набор:

```text
Оплата
  Почасовая ставка
  Валюта

Внешний вид
  Тема: Системная / Светлая / Тёмная

Данные (P1)
  Экспорт
  Резервная копия
```

Settings используют стандартные Material list patterns. Не превращать экран в сетку карточек.

## 12. Design system

### 12.1. Color

Направление: нейтральные поверхности + один холодный акцент.

Начальный seed для prototype: **indigo / blue-violet**, приблизительно `#5967D8`. Финальные light/dark tonal roles должны генерироваться/проверяться как полноценная Material color scheme, а не собираться случайными hex-значениями по компонентам.

Используем только semantic roles:

- `primary` / `onPrimary`;
- `primaryContainer` / `onPrimaryContainer`;
- `surface` variants;
- `outline` / `outlineVariant`;
- `error` roles.

Dynamic Color — возможная настройка позже, но не обязательна для MVP: постоянная собственная схема даёт предсказуемую идентичность и screenshot tests.

### 12.2. Typography

Material semantic typography; без набора локальных размеров «13/15/17/19» на разных экранах.

Ориентир:

- app/month title — `titleLarge`;
- KPI values — `titleMedium`/`titleLarge`;
- day number — `bodyMedium`;
- duration in cell — `labelMedium`;
- ordinary text — `bodyMedium`/`bodyLarge`;
- secondary labels — `labelMedium`, если contrast и font scaling остаются достаточными.

Все пользовательские размеры — scalable.

### 12.3. Shape

Сдержанная система:

- small controls: Material small/medium shape;
- summary container: medium/large;
- bottom sheet: platform Material shape;
- selected date: compact rounded container/circle-like shape только если не уменьшает hit target.

Не давать каждой строке отдельную огромную capsule/card форму.

### 12.4. Spacing

Ограниченный набор tokens, примерно:

```text
4 / 8 / 12 / 16 / 24 / 32 dp
```

Это ритм, а не догма. Insets и adaptive composition важнее механического 8-dp grid.

### 12.5. Iconography

Material Symbols/официальные Material icons там, где значение стандартно. Иконка всегда сопровождается accessibility semantics, если действие не очевидно из соседнего текста.

## 13. Edge-to-edge

- фон может продолжаться под system bars;
- интерактивный контент получает только необходимые insets;
- IME insets учитываются в editor;
- не применять один глобальный `systemBarsPadding()` ко всему приложению;
- navigation gesture area не перекрывает critical controls.

## 14. Adaptive composition

Проектируем по window size, не по модели телефона.

### Compact

- один столбец;
- calendar full width;
- day edit modal sheet.

### Medium

- больше whitespace;
- summary может перераспределить ширину;
- editor остаётся sheet либо side sheet по usability test.

### Expanded

```text
┌───────────────────────┬──────────────────┐
│ Month + summary       │ Selected day     │
│ Calendar              │ editor/details   │
│                       │                  │
└───────────────────────┴──────────────────┘
```

Не растягивать 7 календарных колонок до огромных пустых плиток без ограничения readable width.

## 15. Motion

Motion объясняет state/spatial relationship.

Используем:

- pager transition между месяцами;
- expand/collapse advanced day fields;
- bottom-sheet enter/exit;
- короткое `AnimatedContent`/content-size изменение для summary, если не вызывает layout noise;
- shared bounds только если появится реальный переход между объектом календаря и detail surface.

Не используем:

- bounce всего интерфейса;
- stagger всех 42 day cells;
- бесконечные pulses;
- декоративные loading-анимации там, где данные локальные и появляются мгновенно;
- случайные easing/duration на каждом компоненте.

Уважаем system animator duration/reduced motion behavior, где это применимо.

## 16. Accessibility

Critical flow должен проходить с TalkBack и large font.

Обязательно:

- day cell имеет content description вида `8 сентября, 8 часов`;
- пустая дата не произносится как набор декоративных элементов;
- previous/next month имеют текстовые semantics;
- numeric fields имеют labels и error semantics;
- selected/today state озвучивается;
- touch targets >= 48 dp;
- contrast проверяется;
- status не кодируется только цветом;
- keyboard focus order логичен на large/desktop windows.

## 17. UI states

### Calendar

Local Room flow обычно не требует длинного loading state, но должны существовать:

- initial loading/initialization;
- content;
- empty month (это обычный content, не отдельная трагичная empty page);
- persistence error с recovery.

### Save

- idle;
- validating;
- saving, если операция не мгновенная;
- error;
- success через фактическое обновление state/Snackbar при необходимости.

## 18. Visual anti-patterns

Запрещены без отдельного product decision:

- calendar cells с 3–5 строками цифр;
- серый текст с низким contrast на серой поверхности;
- маленькие hit targets ради «компактности»;
- card-in-card-in-card;
- floating action button только потому, что он Material-компонент;
- toolbar из 5–7 иконок;
- giant hero headers;
- gradients/glows как основной способ создать «современность»;
- анимация каждого изменения числа;
- bottom navigation с двумя пунктами;
- custom controls вместо готового accessible компонента без причины.

## 19. Screenshot quality matrix

Для ключевых экранов/компонентов фиксируем минимум:

- light / dark;
- compact / expanded;
- normal / large font;
- empty month / populated month;
- selected day / today;
- bottom sheet empty / filled / validation error;
- длинная локализованная строка.

Итоговый UI считается готовым только после проверки этой матрицы, а не по одному screenshot телефона разработчика.
