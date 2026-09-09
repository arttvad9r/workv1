# WorkTime backup format

**Текущая версия:** `WTBK v3`

Резервная копия предназначена для переноса локальных данных WorkTime между установками приложения без зависимости от внутренней версии Room database. Encoder всегда пишет v3; decoder сохраняет явную совместимость с ранее выпущенными v1 и v2.

## Что входит в копию

- все рабочие профили;
- выбранный активный профиль;
- имя и timestamp создания каждого профиля;
- индивидуальная базовая почасовая ставка и ISO 4217 currency code профиля, если они заданы;
- legacy/global payment fallback из DataStore для совместимости старых установок и профилей без собственной оплаты;
- все `WorkDay` записи каждого профиля;
- тип дня (`WORK`, `DAY_OFF`, `VACATION`, `SICK`);
- отработанные минуты и переработка;
- заметка;
- optional hourly-rate override дня;
- timestamp последнего изменения записи.

UI-only state, выбранный месяц, открытые sheets, reminder settings и активная timer-сессия в backup не входят.

## Контейнер v3

Числа записываются `DataOutputStream` в big-endian порядке.

1. `int32` magic `0x5754424B` (`WTBK`);
2. `int32` version (`3`);
3. global/legacy payment fallback;
4. `int64` active profile id;
5. `int32` number of work profiles;
6. профили, отсортированные по id.

### Payment

1. currency string;
2. nullable hourly rate в minor currency units.

Currency при записи нормализуется в uppercase.

### Profile record v3

1. `int64` profile id;
2. profile name string;
3. `int64` created-at epoch milliseconds;
4. `boolean` — есть ли собственные payment settings;
5. если `true` — payment record;
6. `int32` number of work-day records профиля;
7. записи дней, отсортированные по дате.

Отсутствующий profile payment означает использование global/legacy fallback после восстановления.

### Work-day record

1. date string (`YYYY-MM-DD`);
2. day type string;
3. `int32` worked minutes;
4. `int32` overtime minutes;
5. nullable `int64` hourly-rate override in minor currency units;
6. `int64` updated-at epoch milliseconds;
7. note string.

### String

- `int32` UTF-8 byte length;
- UTF-8 bytes.

Максимальная длина одной строки — 1 MiB UTF-8. Имя профиля дополнительно ограничено 40 символами.

### Nullable Long

- `boolean` presence flag;
- если `true`, далее `int64` value.

## Совместимость со старыми версиями

### v1

Legacy single-profile формат:

1. magic;
2. version `1`;
3. payment;
4. `int32` work-day count;
5. work-day records.

При чтении v1 decoder создаёт профиль `id=1`, `Основная работа`, `createdAt=0`, делает его активным и оставляет profile-specific payment пустым, поэтому старые ставка/валюта продолжают работать как fallback.

### v2

Первый multi-profile формат:

1. magic;
2. version `2`;
3. global/legacy payment;
4. active profile id;
5. profile count;
6. profile records без profile-specific payment: id, name, created-at, day count, days.

При чтении v2 индивидуальная оплата профилей остаётся пустой и используется сохранённый global fallback.

### v3

Добавляет nullable payment settings внутрь каждого profile record. Остальная модель v2 сохраняется.

## Validation before restore

Файл полностью декодируется и проверяется **до первого изменения пользовательских данных**. Restore отклоняется, если:

- magic или version неизвестны;
- файл оборван или содержит trailing data;
- число профилей не входит в `1..1000`;
- id профиля неположительный или дублируется;
- active profile отсутствует среди профилей;
- имя профиля пустое, имеет внешние пробелы или длиннее 40 символов;
- profile timestamp отрицателен;
- суммарно больше 100 000 дневных записей;
- строка превышает 1 MiB UTF-8;
- currency code не является поддерживаемым ISO 4217 code;
- global, profile или дневная ставка отрицательна;
- даты дублируются внутри одного профиля;
- тип дня неизвестен;
- duration/overtime нарушают `WorkDayValidator`;
- нерабочий день содержит hourly-rate override;
- timestamp дневной записи отрицателен.

Одинаковая календарная дата допустима в разных профилях: записи изолированы ключом `(profileId, date)`.

## Restore semantics

Restore является **replace**, а не merge:

- текущий набор профилей и их `WorkDay` записей заменяется набором из backup;
- profile-specific payment восстанавливается вместе с профилями;
- global/legacy payment fallback и active profile id заменяются значениями из backup;
- пользователь должен подтвердить destructive operation до открытия document picker.

Замена профилей и дней выполняется одной Room transaction. DataStore с global fallback/active profile id является отдельным storage-механизмом и не может участвовать в общей ACID-транзакции с Room. Поэтому runtime failure второго шага обрабатывается compensating rollback: coordinator заранее снимает snapshots Room profiles и DataStore settings, а при ошибке независимо пытается восстановить оба состояния.

Это защищает от обычных runtime/IO failures внутри процесса, но не превращает Room + DataStore в единую crash-atomic транзакцию при принудительном завершении процесса между двумя storage writes. Если в будущем понадобится строгая crash-atomic restore semantics, потребуется transactional journal либо перенос соответствующего состояния в общий transactional store.

## Compatibility policy

- Encoder всегда пишет текущую version `3`.
- Decoder явно поддерживает versions `1`, `2` и `3`.
- Неизвестная version отклоняется до мутации данных; формат не угадывается эвристически.
- v1 мигрируется в один default profile; v2 — в multi-profile модель без индивидуальной оплаты; v3 сохраняет profile payment.
- При появлении v4 старые варианты чтения должны оставаться детерминированными либо получить явно документированную migration policy и fixtures/tests.
