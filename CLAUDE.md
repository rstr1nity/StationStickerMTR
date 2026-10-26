# StationStickerMTR

Аддон "Station Sticker" к моду MTR (Minecraft Transit Railway). Добавляет блоки: станционный стикер (`station_sticker`) и схему линии (`line_scheme`), которые рендерятся через BlockEntityRenderer с данными станций MTR.

## Стек

- Minecraft 1.20.1, Forge 47.3.0 (ForgeGradle 5.1, маппинги official)
- Java 17
- MTR 4.0.3 для 1.20.1 (локальный jar в `libs/`, подключён через `fg.deobf`)
- modId: `stationsticker`, автор: Tr1nity

## Команды

- Сборка: `./gradlew build` (Windows PowerShell: `.\gradlew build`)
- Запуск клиента для проверки: `./gradlew runClient`
- Сборка копирует jar в папку mods через задачу `copyToMods` (путь в `build.gradle` сейчас прибит к машине автора).

## Структура

- `src/main/java/com/yourname/stationsticker/` - пакет `com.yourname` временный, будет переименован
  - `block/` - блоки (`StationStickerBlock`, `LineSchemeBlock`), `block/entity/` - блок-сущности
  - `client/` - рендереры и регистрация клиента (`SPBRouteMapGenerator`, `SPBDynamicTextureCache` и т.д.)
  - `registration/` - `ModBlocks`, `ModBlockEntities`, `ModTabs`
- `src/main/resources/` - `META-INF/mods.toml`, `pack.mcmeta`, `assets/stationsticker/`, `data/stationsticker/`

## Правила работы

- Минимальные точечные правки, не переписывать файлы целиком без запроса.
- Один логический пункт = один коммит. Перед правкой убедиться, что рабочее дерево чистое.
- После правок кода запускать `./gradlew build` и показывать результат компилятора, не гадать.
- Не трогать `libs/` и не коммитить jar-файлы.
- MTR - обязательная зависимость (код напрямую использует `org.mtr.*`). Внутренние классы MTR (`org.mtr.mod.*`) могут меняться между версиями, поэтому версию MTR не повышать без проверки.
- Русские комментарии допустимы. Общение по проекту на русском, кратко и технически.
- Технические утверждения про Forge/MTR/Minecraft проверять по документации или исходникам, а не по памяти.

## План работ

Список найденных проблем и порядок исправления лежит в `REVIEW.md`. Идти по нему сверху вниз, начиная с раздела A.

## Что не проверено

Файлы `client/SPBRouteMapGenerator.java`, `block/entity/LineSchemeEntity.java` и `block/entity/StationStickerBlockEntity.java` в ревью не читались.
