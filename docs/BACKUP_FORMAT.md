# WorkTime backup format

**Текущая версия:** `WTBK v1`

Резервная копия предназначена для переноса локальных данных WorkTime между установками приложения без зависимости от внутренней версии Room database.

## Что входит в копию

- все `WorkDay` записи;
- тип дня (`WORK`, `DAY_OFF`, `VACATION`, `SICK`);
- отработанные минуты и переработка;
- заметка;
- optional hourly-rate override дня;
- timestamp последнего изменения записи;
- базовая почасовая ставка;
- ISO 4217 currency code.

UI-only state, выбранный месяц, открытые sheets и другие временные состояния в backup не входят.

## Контейнер v1

Числа записываются `DataOutputStream` в big-endian порядке.

1. `int32` magic `0x5754424B` (`WTBK`);
2. `int32` version (`1`);
3. currency string;
4. nullable base hourly rate;
5. `int32` number of work-day records;
6. записи дней, отсортированные по дате.

### String

- `int32` UTF-8 byte length;
- UTF-8 bytes.

Максимальная длина одной строки — 1 MiB UTF-8.

### Nullable Long

- `boolean` presence flag;
- если `true`, далее `int64` value.

### Work-day record

1. date string (`YYYY-MM-DD`);
2. day type string;
3. `int32` worked minutes;
4. `int32` overtime minutes;
5. nullable `int64` hourly-rate override in minor currency units;
6. `int64` updated-at epoch milliseconds;
7. note string.

## Validation before restore

Файл полностью декодируется и проверяется **до первого изменения пользовательских данных**. Restore отклоняется, если:

- magic или version неизвестны;
- файл оборван или имеет trailing data;
- количество записей выходит за допустимый предел;
- строка превышает допустимый размер;
- currency code не является поддерживаемым ISO 4217 code;
- базовая или дневная ставка отрицательна;
- даты дублируются;
- тип дня неизвестен;
- duration/overtime нарушают `WorkDayValidator`;
- нерабочий день содержит hourly-rate override;
- timestamp отрицателен.

## Restore semantics

Restore является **replace**, а не merge:

- текущий набор `WorkDay` заменяется набором из backup;
- базовая ставка и валюта заменяются значениями из backup;
- пользователь должен подтвердить destructive operation до открытия document picker.

Room replacement выполняется одной `@Transaction` операцией. Room и DataStore являются разными storage-механизмами и не могут участвовать в общей ACID-транзакции, поэтому runtime failure второго шага обрабатывается compensating rollback: приложение сохраняет предыдущие Room records и payment settings и независимо пытается восстановить оба snapshot-а.

Это защищает от обычных runtime/IO failures внутри процесса, но не превращает Room + DataStore в единую crash-atomic транзакцию при принудительном завершении процесса между двумя storage writes. Если в будущем понадобится строгая crash-atomic restore semantics, payment settings следует перенести в тот же transactional store либо добавить journaled restore protocol.

## Compatibility policy

- Encoder всегда пишет текущую version.
- Decoder v1 принимает только version `1` и не угадывает неизвестные форматы.
- При появлении v2 старый decoder должен безопасно отклонять файл до мутации данных.
- Новые версии должны иметь явную migration/compatibility policy и отдельные fixtures/tests.
