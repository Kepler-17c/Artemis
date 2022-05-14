/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.objects.item;

import com.wynntils.wc.objects.item.render.HighlightedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WynnCosmeticStack extends WynnItemStack implements HighlightedItem {

    private ChatFormatting color;

    public WynnCosmeticStack(ItemStack stack) {
        super(stack);

        color = ChatFormatting.getByCode(stack.getHoverName().getString().charAt(1));
        if (color == null) color = ChatFormatting.WHITE;
    }

    @Override
    public int getHighlightColor(Screen screen, Slot slot) {
        int highlightColor = color.getColor();
        highlightColor = 0xFF000000 | highlightColor;
        return highlightColor;
    }
}