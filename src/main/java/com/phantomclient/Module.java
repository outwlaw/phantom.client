package com.phantomclient;

import net.minecraft.item.Item;

public class Module {
    public final String name;
    public final String category;
    public final Item icon;
    public boolean enabled;
    public boolean favorite;
    public Runnable onDisable;

    public Module(String name, String category, Item icon) {
        this.name = name;
        this.category = category;
        this.icon = icon;
    }

    public void toggle() {
        enabled = !enabled;
        if (!enabled && onDisable != null) onDisable.run();
    }
}
