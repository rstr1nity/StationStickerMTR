# StationStickerMTR

Аддон "Station Sticker" к моду MTR (Minecraft Transit Railway). Добавляет блоки: станционный стикер (`station_sticker`) и схему линии (`line_scheme`), которые рендерятся через BlockEntityRenderer с данными станций MTR.

## Стек

- Forge, ForgeGradle 5.1, маппинги official, Java 17
- Версии MC/Forge/MTR/мода — только в `gradle.properties`; `mods.toml` и `pack.mcmeta` получают их через `processResources expand`
- MTR 4.0.3 с Modrinth Maven (`maven.modrinth:minecraft-transit-railway:FORGE-<ver>+<mc>`)
- modId: `stationsticker`, автор: Tr1nity

## Ветки (одна ветка = одна версия MC)

- `mc/1.20.1` — основная (MC 1.20.1, Forge 47.3.0), новая работа в `feature/*` от неё
- `mc/1.19.2` — поддержка (MC 1.19.2, Forge 43.5.0), изменения переносить `git cherry-pick`
- Ветки разных версий MC НЕ мержить друг в друга: отличаются `gradle.properties` и местами API
- Тег релиза: `v<mod_version>+<mc>`, изменения записывать в `CHANGELOG.md`

## Команды

- Сборка: `./gradlew build` (Windows PowerShell: `.\gradlew build`)
- Запуск клиента для проверки: `./gradlew runClient`
- `copyToMods` копирует jar в игру, только если в `~/.gradle/gradle.properties` задан `mods_dir_<версия MC>`
- CI: `.github/workflows/build.yml` (сборка на push в `mc/**`, `feature/**`; релиз по тегу `v*`)

## Структура

- `src/main/java/com/tr1nity/stationsticker/` - пакет `com.tr1nity.stationsticker`
  - `block/` - блоки (`StationStickerBlock`, `LineSchemeBlock`), `block/entity/` - блок-сущности
  - `client/` - рендереры и регистрация клиента (`SPBRouteMapGenerator`, `SPBDynamicTextureCache` и т.д.)
  - `registration/` - `ModBlocks`, `ModBlockEntities`, `ModTabs`
- `src/main/resources/` - `META-INF/mods.toml`, `pack.mcmeta`, `assets/stationsticker/`, `data/stationsticker/`

## Правила работы

- Минимальные точечные правки, не переписывать файлы целиком без запроса.
- В сообщения коммитов и описания PR не добавлять `Co-Authored-By: Claude` и прочую атрибуцию Claude.
- Один логический пункт = один коммит. Перед правкой убедиться, что рабочее дерево чистое.
- После правок кода запускать `./gradlew build` и показывать результат компилятора, не гадать.
- Не коммитить jar-файлы (`libs/` в `.gitignore`).
- Исправление нужно в обеих версиях MC: сначала коммит в `mc/1.20.1`, потом cherry-pick в `mc/1.19.2` и сборка там.
- MTR - обязательная зависимость (код напрямую использует `org.mtr.*`). Внутренние классы MTR (`org.mtr.mod.*`) могут меняться между версиями, поэтому версию MTR не повышать без проверки.
- Русские комментарии допустимы. Общение по проекту на русском, кратко и технически.
- Технические утверждения про Forge/MTR/Minecraft проверять по документации или исходникам, а не по памяти.

## План работ

Список найденных проблем и порядок исправления лежит в `REVIEW.md`. Идти по нему сверху вниз, начиная с раздела A.

## Что не проверено

Рендереры (`StationStickerRenderer`, `LineSchemeRenderer`) просмотрены только по структуре. Остальные файлы прочитаны, находки в `REVIEW.md`.
