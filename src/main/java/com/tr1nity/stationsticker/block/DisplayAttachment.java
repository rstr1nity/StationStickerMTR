package com.tr1nity.stationsticker.block;

import net.minecraft.util.StringRepresentable;

// Как закреплён экран: на стене или на подвесе к потолку
public enum DisplayAttachment implements StringRepresentable {
    WALL("wall"),
    CEILING("ceiling");

    private final String name;

    DisplayAttachment(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
