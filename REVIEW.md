# Ревью аддона StationStickerMTR

Составлено по файлам проекта, статус на 2026-10-26 (✅ = сделано в обеих ветках `mc/1.20.1` и `mc/1.19.2`). НЕ прочитаны: `client/SPBRouteMapGenerator.java` (46 КБ), `block/entity/LineSchemeEntity.java`, `block/entity/StationStickerBlockEntity.java`. Рендереры просмотрены только по структуре.

Правила работы: один пункт = один коммит, минимальные точечные правки, после каждого пункта `gradlew build`.

## A. Может сломаться (делать первыми)

1. ✅ **MTR должен быть обязательной зависимостью.** В `src/main/resources/META-INF/mods.toml` для `modId="mtr"` стоит `mandatory=false`, но код жёстко использует MTR (`LineSchemeBlock` вызывает `org.mtr.mod.Items.BRUSH`, рендереры импортируют `org.mtr.*`). Без MTR будет `NoClassDefFoundError`. Поставить `mandatory=true`.
2. ✅ **Версии в `mods.toml` от 1.19.2.** `loaderVersion="[40,)"` и forge `versionRange="[43,)"` заменить на `[47,)` (Forge для 1.20.1 = 47.x). Диапазон MTR `[4.0.2,)` сузить, например до `[4.0.3,4.1)`: используются внутренние классы MTR (`org.mtr.mod.*`, `DynamicTextureCache`), между версиями они ломаются. Добавить зависимость на `minecraft` `[1.20.1,1.21)`.
3. ✅ **`pack.mcmeta`.** (по шаблону Forge MDK: в 1.20.1 только `pack_format: 15`, в 1.19.2 файл был верным) `pack_format: 9` -> `15` (1.20.1), `forge:resource_pack_format` -> `15`, `forge:data_pack_format` -> `15`. Описание `${mod_id} resources` не подставляется (нет `processResources` с expand): заменить на обычный текст или добавить expand.
4. ✅ **В моделях блоков нет текстуры `particle`.** Файлы `models/block/station_sticker.json` и `models/block/line_scheme.json` содержат только `"all"`. Добавить `"particle": "stationsticker:block/station_sticker"`, иначе частицы фиолетово-чёрные.
5. ✅ **Блокстейт `station_sticker.json`.** (ставить на пол/потолок можно было и раньше; не хватало вариантов модели, отсюда ошибки в логе и частицы) Нет вариантов `facing=up` и `facing=down`, хотя блок их поддерживает (`StationStickerBlock.FACING = BlockStateProperties.FACING`). Все существующие варианты ведут на одну модель, поэтому достаточно одного варианта `""`.
6. ✅ **`gradle.properties` от шаблона 1.19.2** (`minecraft_version=1.19.2`, `forge_version=43.5.0`), а `build.gradle` собирает под 1.20.1 / Forge 47.3.0. Привести к одной версии и использовать свойства в `build.gradle`.
7. ✅ **Хитбокс `LineSchemeBlock` выходит за блок.** Сделано: столкновения убраны, схема стала составным блоком (невидимые `LineSchemePartBlock` в области 5x3). `Block.box(-24, -8, ..., 40, 24, ...)` выходит за 0..16. Проверить выделение и culling в игре, при проблемах разбить на несколько блоков.

## B. Мусор и плейсхолдеры

8. Пакет `com.yourname.stationsticker` и `group = 'com.yourname...'` переименовать до публичного релиза (меняется везде: java, `mods.toml`, `mixins.json`, импорты).
9. `src/main/resources/stationsticker.mixins.json` пустой, не подключён, ссылается на несуществующие `refmap` и пакет `mixin`. Удалить.
10. ✅ `README.txt`, `CREDITS.txt`, `LICENSE.txt`, `changelog.txt` (47 КБ) взяты из Forge MDK. `LICENSE.txt` - лицензия Forge, а в `mods.toml` указано `license="All rights reserved"` и отдельно `license="MIT"`. Определиться с лицензией, положить свой LICENSE и написать README про аддон.
11. `client/ClientModEvents.java`: в начале файла закомментированный старый вариант класса, удалить. Убрать оставшиеся комментарии вида `// ← ДОБАВИТЬ ЭТОТ ИМПОРТ`, `// ИЗМЕНЕНО С true НА false!`.
12. ✅ `build.gradle`: задача `copyToMods` прибита к `C:/Users/aleks/AppData/Roaming/.minecraft/versions/проверка мода 1.20.1/mods` и вызывается через `build.finalizedBy`. На другой машине и в CI сборка упадёт. Вынести путь в локальное свойство (`gradle.properties` в `~/.gradle`) и сделать задачу необязательной.
13. ✅ (без чистки истории) `libs/MTR-forge-4.0.3+1.20.1.jar` (85 МБ) не исключён в `.gitignore`. Проверить `git ls-files libs`; если в индексе, убрать (`git rm --cached`) и добавить `libs/` в `.gitignore`. Если jar уже в истории, GitHub отклонит push из-за лимита 100 МБ на файл. Описать в README, куда класть jar. Проверить, что лицензия MTR допускает распространение.
14. ✅ Версия в `mods.toml` захардкожена (`1.1.0`), в `build.gradle` стоит `1.1.0+1.20.1`. Использовать `${file.jarVersion}` или expand.
15. Ресурсы:
    - рецепт `line_scheme.json`: пустые ряды `"   "` заменить на один ряд `"DSD"`;
    - текстуры названы непоследовательно: `line-scheme-item.png` и `station-sticker-item.png` (дефисы) против snake_case в остальном;
    - `textures/block/sticker.png` (115 КБ) выглядит неиспользуемым, проверить и удалить;
    - `logo.png` 1,7 МБ - уменьшить;
    - `ru_ru.json`: название вкладки «Станционные стикеры» вместо единственного числа;
    - ✅ блокстейт `line_scheme.json` ссылается на модель `station_sticker`, сделать свою;
    - текстура `station-sticker-item.png` 669x373: для предмета нужен квадрат со стороной степени двойки (16, 32, 64), иначе отключаются mipmap.

## D. Найдено при проверке в игре

16. ✅ Стикеры разных направлений (например, в углу) сливались в одну линию: сосед в линии теперь должен совпадать по `FACING` и `HORIZONTAL_FACING`.
17. ✅ Название станции рисовалось от края линии: теперь по центру каждого отрезка ~8 блоков.
18. ✅ Стикер вне станции MTR был невидимым: теперь белая полоска без названия.
19. ✅ `runClient` падал на mixin MTR (`f_8927_`): включён перевод SRG-имён в refmap (`mixin.env.remapRefMap`).
20. ✅ Обе ветки делили папку `run/`: теперь `run-<версия MC>`.
21. ✅ Нет таблиц лута (`data/stationsticker/loot_tables/blocks/*.json`): в выживании стикер и схема ничего не выпадают при ломании.
22. Схемы, поставленные до перехода на составной блок, не имеют частей: хитбокс у них только в центральной клетке, их нужно переставить.

## C. Что нормально (не трогать)

- Структура пакетов `block`, `block/entity`, `client`, `registration`.
- Регистрация через `DeferredRegister`, клиентский код отделён через `Dist.CLIENT`.
- Локализация en/ru, рецепты, креативная вкладка.
- Рендереры отдельно от блоков.
