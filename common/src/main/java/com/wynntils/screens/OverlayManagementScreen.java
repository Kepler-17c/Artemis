/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.ConfigManager;
import com.wynntils.core.features.overlays.Corner;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayManager;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.SectionCoordinates;
import com.wynntils.core.features.overlays.sizes.OverlaySize;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.utils.objects.CustomColor;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.Vec2;

public class OverlayManagementScreen extends Screen {
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MAX_CLICK_DISTANCE = 20;

    private static SelectionMode selectionMode = SelectionMode.None;
    private static Overlay selectedOverlay = null;
    private static Corner selectedCorner = null;

    public OverlayManagementScreen() {
        super(new TranslatableComponent("screens.wynntils.overlayManagement.name"));
    }

    @Override
    protected void init() {
        setupButtons();
    }

    private void setupButtons() {

        this.addRenderableWidget(new Button(
                this.width / 2 - BUTTON_WIDTH * 2,
                this.height - 150,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                new TranslatableComponent("screens.wynntils.overlayManagement.closeSettingsScreen"),
                button -> {
                    onClose();
                }));
        this.addRenderableWidget(new Button(
                this.width / 2 - BUTTON_WIDTH / 2,
                this.height - 150,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                new TranslatableComponent("screens.wynntils.overlayManagement.testSettings"),
                button -> {}));
        this.addRenderableWidget(new Button(
                this.width / 2 + BUTTON_WIDTH,
                this.height - 150,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                new TranslatableComponent("screens.wynntils.overlayManagement.applySettings"),
                button -> {
                    ConfigManager.saveConfig();
                    onClose();
                }));
    }

    @Override
    public void onClose() {
        ConfigManager.loadConfigFile();
        ConfigManager.loadConfigOptions(ConfigManager.getConfigHolders(), true);
        super.onClose();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderSections(poseStack);

        Set<Overlay> overlays = OverlayManager.getOverlays();

        for (Overlay overlay : overlays) {
            CustomColor color = OverlayManager.isEnabled(overlay) ? CommonColors.GREEN : CommonColors.RED;
            RenderUtils.drawRectBorders(
                    poseStack,
                    color,
                    overlay.getRenderX(),
                    overlay.getRenderY(),
                    overlay.getRenderX() + overlay.getWidth(),
                    overlay.getRenderY() + overlay.getHeight(),
                    1,
                    1.8f);
            RenderUtils.drawRect(
                    poseStack,
                    color.withAlpha(30),
                    overlay.getRenderX(),
                    overlay.getRenderY(),
                    0,
                    overlay.getWidth(),
                    overlay.getHeight());

            Vec2 middleOfOverlay = new Vec2(
                    overlay.getRenderX() + overlay.getWidth() / 2, overlay.getRenderY() + overlay.getHeight() / 2);

            RenderUtils.drawRect(CommonColors.WHITE, middleOfOverlay.x - 5, middleOfOverlay.y - 5, 10, 10, 10);
        }

        super.render(poseStack, mouseX, mouseY, partialTick); // This renders widgets
    }

    private void renderSections(PoseStack poseStack) {
        for (SectionCoordinates section : OverlayManager.getSections()) {
            RenderUtils.drawRectBorders(
                    poseStack,
                    CustomColor.fromInt(section.hashCode()).withAlpha(255),
                    section.x1(),
                    section.y1(),
                    section.x2(),
                    section.y2(),
                    0,
                    2);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Order:
        //  - Corners
        //  - Edges
        //  - OverlayArea

        // reset
        selectionMode = SelectionMode.None;
        selectedOverlay = null;
        selectedCorner = null;

        Vec2 mousePos = new Vec2((float) mouseX, (float) mouseY);

        for (Overlay overlay : OverlayManager.getOverlays()) {
            for (Map.Entry<Corner, Vec2> corner : overlay.getCornersMap().entrySet()) {
                float distance = corner.getValue().distanceToSqr(mousePos);
                if (distance < MAX_CLICK_DISTANCE) {
                    selectedOverlay = overlay;
                    selectedCorner = corner.getKey();
                    selectionMode = SelectionMode.Corner;

                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        for (Overlay overlay : OverlayManager.getOverlays()) {
            if ((overlay.getRenderX() <= mouseX && overlay.getRenderX() + overlay.getWidth() >= mouseX)
                    && (overlay.getRenderY() <= mouseY && overlay.getRenderY() + overlay.getHeight() >= mouseY)) {
                selectedOverlay = overlay;
                selectionMode = SelectionMode.Area;

                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        selectedOverlay = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Order:
        //  - Corners
        //  - Edges
        //  - OverlayArea

        handleOverlayCornerDrag(button, dragX, dragY);

        handleOverlayBodyDrag(button, dragX, dragY);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void handleOverlayBodyDrag(int button, double dragX, double dragY) {
        if (selectionMode != SelectionMode.Area || selectedOverlay == null) {
            return;
        }

        Overlay overlay = selectedOverlay;

        overlay.setPosition(OverlayPosition.getBestPositionFor(
                overlay, overlay.getRenderX(), overlay.getRenderY(), (float) dragX, (float) dragY));
    }

    private void handleOverlayCornerDrag(int button, double dragX, double dragY) {
        if (selectionMode != SelectionMode.Corner || selectedCorner == null || selectedOverlay == null) {
            return;
        }

        Overlay overlay = selectedOverlay;
        Corner corner = selectedCorner;

        OverlaySize overlaySize = overlay.getSize();

        final float renderX = overlay.getRenderX();
        final float renderY = overlay.getRenderY();

        switch (corner) {
            case TOP_LEFT -> {
                overlaySize.setWidth((float) (overlaySize.getWidth() - dragX));
                overlaySize.setHeight((float) (overlaySize.getHeight() - dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) dragX, (float) dragY));
            }
            case TOP_RIGHT -> {
                overlaySize.setWidth((float) (overlaySize.getWidth() + dragX));
                overlaySize.setHeight((float) (overlaySize.getHeight() - dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) dragY));
            }
            case BOTTOM_LEFT -> {
                overlaySize.setWidth((float) (overlaySize.getWidth() - dragX));
                overlaySize.setHeight((float) (overlaySize.getHeight() + dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) dragX, (float) 0));
            }
            case BOTTOM_RIGHT -> {
                overlaySize.setWidth((float) (overlaySize.getWidth() + dragX));
                overlaySize.setHeight((float) (overlaySize.getHeight() + dragY));
                overlay.setPosition(
                        OverlayPosition.getBestPositionFor(overlay, renderX, renderY, (float) 0, (float) 0));
            }
        }
    }

    private enum SelectionMode {
        None,
        Corner,
        Edge,
        Area
    }
}