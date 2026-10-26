package com.tr1nity.stationsticker.block;

// Варианты настройки навигационной таблички. Хранятся по имени в NBT и по номеру в пакете.
public final class NavSignOptions {

    private NavSignOptions() {
    }

    public enum Template {
        TRANSFER // «Переход на линию»: стрелка, пиктограмма, кружок линии, станция пересадки
    }

    public enum Arrow {
        LEFT, UP, RIGHT, DOWN
    }

    public enum Pictogram {
        NONE, PERSON, STAIRS
    }

    // Значение по имени из NBT; при неизвестном имени (старая версия, повреждённые данные) - значение по умолчанию
    public static <E extends Enum<E>> E byName(Class<E> type, String name, E fallback) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) return value;
        }
        return fallback;
    }

    // Значение по номеру из пакета, с защитой от выхода за границы
    public static <E extends Enum<E>> E byOrdinal(Class<E> type, int ordinal, E fallback) {
        E[] values = type.getEnumConstants();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }
}
