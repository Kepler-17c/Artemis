/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.map.pois;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.CodedString;
import com.wynntils.models.map.PoiLocation;
import com.wynntils.models.map.type.DisplayPriority;
import com.wynntils.models.seaskipper.type.SeaskipperDestination;
import com.wynntils.models.seaskipper.type.SeaskipperDestinationProfile;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.renderer.MultiBufferSource;

public class SeaskipperDestinationPoi implements Poi {
    private final SeaskipperDestination destination;
    private final SeaskipperDestinationProfile profile;

    private final int width;
    private final int height;
    private final PoiLocation destinationCenter;

    private float renderedX;
    private float renderedEndX;
    private float renderedY;
    private float renderedEndY;

    public SeaskipperDestinationPoi(SeaskipperDestination destination) {
        this.destination = destination;
        this.profile = destination.profile();

        int startX = profile.startX();
        int startZ = profile.startZ();
        int endX = profile.endX();
        int endZ = profile.endZ();

        this.width = endX - startX;
        this.height = endZ - startZ;
        this.destinationCenter = new PoiLocation(startX + width / 2, null, startZ + height / 2);
    }

    @Override
    public PoiLocation getLocation() {
        return destinationCenter;
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.HIGHEST;
    }

    @Override
    public boolean hasStaticLocation() {
        return true;
    }

    @Override
    public void renderAt(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float mapZoom) {
        renderPoi(poseStack, bufferSource, renderX, renderY, mapZoom, true);
    }

    public void renderAtWithoutBorders(
            PoseStack poseStack, MultiBufferSource bufferSource, float renderX, float renderY, float mapZoom) {
        renderPoi(poseStack, bufferSource, renderX, renderY, mapZoom, false);
    }

    private void renderPoi(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float renderX,
            float renderY,
            float mapZoom,
            boolean renderBorders) {
        poseStack.pushPose();
        poseStack.translate(0, 0, 100);

        final float renderWidth = width * mapZoom;
        final float renderHeight = height * mapZoom;
        final float actualRenderX = renderX - renderWidth / 2f;
        final float actualRenderZ = renderY - renderHeight / 2f;

        renderedX = actualRenderX;
        renderedY = actualRenderZ;
        renderedEndX = renderedX + renderWidth;
        renderedEndY = renderedY + renderHeight;

        CustomColor color;

        if (destination.isPlayerInside()) {
            color = CommonColors.ORANGE;
        } else {
            color = CommonColors.WHITE;
        }

        if (renderBorders) {
            BufferedRenderUtils.drawRect(
                    poseStack,
                    bufferSource,
                    color.withAlpha(65),
                    actualRenderX,
                    actualRenderZ,
                    0,
                    renderWidth,
                    renderHeight);

            BufferedRenderUtils.drawRectBorders(
                    poseStack,
                    bufferSource,
                    color,
                    actualRenderX,
                    actualRenderZ,
                    actualRenderX + renderWidth,
                    actualRenderZ + renderHeight,
                    0,
                    1.5f);
        }

        BufferedFontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        bufferSource,
                        CodedString.fromString(profile.destination()),
                        actualRenderX,
                        actualRenderX + renderWidth,
                        actualRenderZ,
                        actualRenderZ + renderHeight,
                        0,
                        color,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.OUTLINE);

        poseStack.popPose();
    }

    @Override
    public int getWidth(float mapZoom, float scale) {
        return (int) (width * mapZoom);
    }

    @Override
    public int getHeight(float mapZoom, float scale) {
        return (int) (height * mapZoom);
    }

    @Override
    public String getName() {
        return profile.destination();
    }

    public SeaskipperDestination getDestination() {
        return destination;
    }

    public int getLevel() {
        return profile.combatLevel();
    }

    public boolean isPlayerInside() {
        return destination.isPlayerInside();
    }

    public boolean isAvailable() {
        return destination.isAvailable();
    }

    public boolean isSelected(double mouseX, double mouseY) {
        return mouseX > renderedX && mouseX < renderedEndX && mouseY > renderedY && mouseY < renderedEndY;
    }
}
