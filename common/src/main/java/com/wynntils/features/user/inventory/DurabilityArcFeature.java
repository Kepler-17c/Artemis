/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.google.common.collect.ImmutableList;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.core.managers.Model;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.custom.item.ItemStackTransformModel;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.DurabilityProperty;
import com.wynntils.wc.custom.item.properties.ItemProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE, category = "Inventory")
public class DurabilityArcFeature extends UserFeature {
    @Config
    public static boolean renderDurabilityArcInventories = true;

    @Config
    public static boolean renderDurabilityArcHotbar = true;

    @Override
    protected void onInit(
            ImmutableList.Builder<Condition> conditions, ImmutableList.Builder<Class<? extends Model>> dependencies) {
        dependencies.add(ItemStackTransformModel.class);
    }

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Pre e) {
        if (!renderDurabilityArcHotbar) return;
        drawDurabilityArc(e.getStack(), e.getX(), e.getY(), true);
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        if (!renderDurabilityArcInventories) return;
        drawDurabilityArc(e.getSlot().getItem(), e.getSlot().x, e.getSlot().y, false);
    }

    private void drawDurabilityArc(ItemStack item, int slotX, int slotY, boolean hotbar) {
        if (!(item instanceof WynnItemStack wynnItem)) return;

        if (!wynnItem.hasProperty(ItemProperty.DURABILITY)) return; // no durability info
        DurabilityProperty durability = wynnItem.getProperty(ItemProperty.DURABILITY);

        // calculate color of arc
        float durabilityPercent = durability.getDurabilityPercent();
        int colorInt = Mth.hsvToRgb(Math.max(0f, durabilityPercent) / 3f, 1f, 1f);
        CustomColor color = CustomColor.fromInt(colorInt).setAlpha(160);

        // draw
        RenderUtils.drawArc(color, slotX, slotY, hotbar ? 0 : 200, durabilityPercent, 6, 8);
    }
}