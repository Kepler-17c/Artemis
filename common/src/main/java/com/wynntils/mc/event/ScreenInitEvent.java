/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a screen is re-inited. Use this to add widgets.
 */
public class ScreenInitEvent extends Event {
    private final Screen screen;
    private final boolean firstInit;

    public ScreenInitEvent(Screen screen, boolean firstInit) {
        this.screen = screen;
        this.firstInit = firstInit;
    }

    public Screen getScreen() {
        return screen;
    }

    public boolean isFirstInit() {
        return firstInit;
    }
}
