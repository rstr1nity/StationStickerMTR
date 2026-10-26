package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.NavSignOptions.Pictogram;
import com.tr1nity.stationsticker.block.NavSignOptions.Template;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

// Все настройки навигационной таблички: хранятся в блок-сущности, ходят в пакете и правятся в окне.
// Пересадка: станция MTR и цвет линии (0 - не выбрана).
// Выходы: названия выходов станции MTR слева и справа ("" - нет) и id значков MTR к их объектам ("" - без значка).
public record NavSignSettings(Template template, Arrow arrow, Pictogram pictogram,
                              long transferStationId, int transferColor,
                              String exitLeft, String exitRight, List<String> iconsLeft, List<String> iconsRight) {

    // Сколько объектов выхода (после улицы) показывается и может получить значок
    public static final int EXIT_ITEMS = 3;
    private static final int MAX_TEXT_LENGTH = 128;

    public static final NavSignSettings DEFAULT = new NavSignSettings(Template.TRANSFER, Arrow.LEFT, Pictogram.STAIRS,
            0, 0, "", "", emptyIcons(), emptyIcons());

    // Длина списков значков всегда EXIT_ITEMS, цвет без альфы
    public NavSignSettings {
        transferColor &= 0xFFFFFF;
        exitLeft = limit(exitLeft);
        exitRight = limit(exitRight);
        iconsLeft = normalize(iconsLeft);
        iconsRight = normalize(iconsRight);
    }

    public NavSignSettings withTemplate(Template value) {
        return new NavSignSettings(value, arrow, pictogram, transferStationId, transferColor, exitLeft, exitRight, iconsLeft, iconsRight);
    }

    public NavSignSettings withArrow(Arrow value) {
        return new NavSignSettings(template, value, pictogram, transferStationId, transferColor, exitLeft, exitRight, iconsLeft, iconsRight);
    }

    public NavSignSettings withPictogram(Pictogram value) {
        return new NavSignSettings(template, arrow, value, transferStationId, transferColor, exitLeft, exitRight, iconsLeft, iconsRight);
    }

    public NavSignSettings withTransfer(long stationId, int color) {
        return new NavSignSettings(template, arrow, pictogram, stationId, color, exitLeft, exitRight, iconsLeft, iconsRight);
    }

    public NavSignSettings withExit(boolean right, String exit) {
        return right
                ? new NavSignSettings(template, arrow, pictogram, transferStationId, transferColor, exitLeft, exit, iconsLeft, iconsRight)
                : new NavSignSettings(template, arrow, pictogram, transferStationId, transferColor, exit, exitRight, iconsLeft, iconsRight);
    }

    public NavSignSettings withIcon(boolean right, int index, String signId) {
        List<String> icons = new ArrayList<>(right ? iconsRight : iconsLeft);
        icons.set(index, signId);
        return right
                ? new NavSignSettings(template, arrow, pictogram, transferStationId, transferColor, exitLeft, exitRight, iconsLeft, icons)
                : new NavSignSettings(template, arrow, pictogram, transferStationId, transferColor, exitLeft, exitRight, icons, iconsRight);
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("template", template.name());
        tag.putString("arrow", arrow.name());
        tag.putString("pictogram", pictogram.name());
        tag.putLong("transferStationId", transferStationId);
        tag.putInt("transferColor", transferColor);
        tag.putString("exitLeft", exitLeft);
        tag.putString("exitRight", exitRight);
        tag.put("iconsLeft", toTag(iconsLeft));
        tag.put("iconsRight", toTag(iconsRight));
        return tag;
    }

    public static NavSignSettings load(CompoundTag tag) {
        return new NavSignSettings(
                NavSignOptions.byName(Template.class, tag.getString("template"), Template.TRANSFER),
                NavSignOptions.byName(Arrow.class, tag.getString("arrow"), Arrow.LEFT),
                NavSignOptions.byName(Pictogram.class, tag.getString("pictogram"), Pictogram.STAIRS),
                tag.getLong("transferStationId"), tag.getInt("transferColor"),
                tag.getString("exitLeft"), tag.getString("exitRight"),
                fromTag(tag.getList("iconsLeft", Tag.TAG_STRING)), fromTag(tag.getList("iconsRight", Tag.TAG_STRING)));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(template.ordinal());
        buf.writeVarInt(arrow.ordinal());
        buf.writeVarInt(pictogram.ordinal());
        buf.writeLong(transferStationId);
        buf.writeInt(transferColor);
        buf.writeUtf(exitLeft, MAX_TEXT_LENGTH);
        buf.writeUtf(exitRight, MAX_TEXT_LENGTH);
        for (String icon : iconsLeft) buf.writeUtf(icon, MAX_TEXT_LENGTH);
        for (String icon : iconsRight) buf.writeUtf(icon, MAX_TEXT_LENGTH);
    }

    public static NavSignSettings read(FriendlyByteBuf buf) {
        Template template = NavSignOptions.byOrdinal(Template.class, buf.readVarInt(), Template.TRANSFER);
        Arrow arrow = NavSignOptions.byOrdinal(Arrow.class, buf.readVarInt(), Arrow.LEFT);
        Pictogram pictogram = NavSignOptions.byOrdinal(Pictogram.class, buf.readVarInt(), Pictogram.STAIRS);
        long stationId = buf.readLong();
        int color = buf.readInt();
        String exitLeft = buf.readUtf(MAX_TEXT_LENGTH);
        String exitRight = buf.readUtf(MAX_TEXT_LENGTH);
        List<String> iconsLeft = new ArrayList<>();
        for (int i = 0; i < EXIT_ITEMS; i++) iconsLeft.add(buf.readUtf(MAX_TEXT_LENGTH));
        List<String> iconsRight = new ArrayList<>();
        for (int i = 0; i < EXIT_ITEMS; i++) iconsRight.add(buf.readUtf(MAX_TEXT_LENGTH));
        return new NavSignSettings(template, arrow, pictogram, stationId, color, exitLeft, exitRight, iconsLeft, iconsRight);
    }

    private static List<String> emptyIcons() {
        return normalize(List.of());
    }

    private static List<String> normalize(List<String> icons) {
        List<String> result = new ArrayList<>(EXIT_ITEMS);
        for (int i = 0; i < EXIT_ITEMS; i++) result.add(i < icons.size() ? limit(icons.get(i)) : "");
        return List.copyOf(result);
    }

    private static String limit(String text) {
        return text == null ? "" : text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }

    private static ListTag toTag(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        return list;
    }

    private static List<String> fromTag(ListTag list) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) values.add(list.getString(i));
        return values;
    }
}
