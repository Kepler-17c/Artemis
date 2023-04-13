/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wynntils.models.lootruns.LootrunInstance;
import com.wynntils.models.lootruns.type.ColoredPath;
import com.wynntils.models.lootruns.type.ColoredPosition;
import com.wynntils.models.map.MapTexture;
import com.wynntils.models.map.pois.Poi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.PointerType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public final class MapRenderer {
    public static Poi hovered = null;

    // ####### Start of Filtered Lootrun Points Cache #######
    private static LootrunInstance lastLootrun = null;
    private static final Map<ColoredPath, PathCacheEntry> filteredPathMap = new HashMap<>();
    // #######  End of Filtered Lootrun Points Cache  #######

    public static void renderMapQuad(
            MapTexture map,
            PoseStack poseStack,
            float centerX,
            float centerZ,
            float textureX,
            float textureZ,
            float width,
            float height,
            float scale,
            boolean renderUsingLinear) {
        RenderSystem.disableBlend();

        // has to be before setting shader texture
        int option = renderUsingLinear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, option);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, option);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, map.resource());

        // clamp map rendering
        // has to be after setting shader texture
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);

        float uScale = 1f / map.getTextureWidth();
        float vScale = 1f / map.getTextureHeight();

        float halfRenderedWidth = width / 2f;
        float halfRenderedHeight = height / 2f;
        float halfTextureWidth = halfRenderedWidth * scale;
        float halfTextureHeight = halfRenderedHeight * scale;

        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder
                .vertex(matrix, (centerX - halfRenderedWidth), (centerZ + halfRenderedHeight), 0)
                .uv((textureX - halfTextureWidth) * uScale, (textureZ + halfTextureHeight) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, (centerX + halfRenderedWidth), (centerZ + halfRenderedHeight), 0)
                .uv((textureX + halfTextureWidth) * uScale, (textureZ + halfTextureHeight) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, (centerX + halfRenderedWidth), (centerZ - halfRenderedHeight), 0)
                .uv((textureX + halfTextureWidth) * uScale, (textureZ - halfTextureHeight) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, (centerX - halfRenderedWidth), (centerZ - halfRenderedHeight), 0)
                .uv((textureX - halfTextureWidth) * uScale, (textureZ - halfTextureHeight) * vScale)
                .endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void renderLootrunLine(
            LootrunInstance lootrun,
            float lootrunWidth,
            PoseStack poseStack,
            float centerX,
            float centerZ,
            float mapTextureX,
            float mapTextureZ,
            float currentZoom,
            float mapWidth,
            float mapHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        renderLootrun(
                lootrun,
                lootrunWidth + 2,
                true,
                poseStack,
                centerX,
                centerZ,
                mapTextureX,
                mapTextureZ,
                currentZoom,
                mapWidth,
                mapHeight);
        renderLootrun(
                lootrun,
                lootrunWidth,
                false,
                poseStack,
                centerX,
                centerZ,
                mapTextureX,
                mapTextureZ,
                currentZoom,
                mapWidth,
                mapHeight);

        RenderSystem.disableBlend();
    }

    public static void renderCursor(
            PoseStack poseStack,
            float renderX,
            float renderY,
            float pointerScale,
            CustomColor pointerColor,
            PointerType pointerType,
            boolean followPlayerRotation) {
        if (!followPlayerRotation) {
            poseStack.pushPose();
            RenderUtils.rotatePose(
                    poseStack, renderX, renderY, 180 + McUtils.player().getYRot());
        }

        float renderedWidth = pointerType.width * pointerScale;
        float renderedHeight = pointerType.height * pointerScale;

        RenderUtils.drawTexturedRectWithColor(
                poseStack,
                Texture.MAP_POINTERS.resource(),
                pointerColor,
                renderX - renderedWidth / 2f,
                renderY - renderedHeight / 2f,
                0,
                renderedWidth,
                renderedHeight,
                0,
                pointerType.textureY,
                pointerType.width,
                pointerType.height,
                Texture.MAP_POINTERS.width(),
                Texture.MAP_POINTERS.height());

        if (!followPlayerRotation) {
            poseStack.popPose();
        }
    }

    /**
     * {@param poi} POI that we get the render coordinate for
     * {@param mapCenterX} center coordinates of map (in-game coordinates)
     * {@param centerX} center coordinates of map (screen render coordinates)
     * {@param currentZoom} the bigger, the more detailed the map is
     */
    public static float getRenderX(Poi poi, float mapCenterX, float centerX, float currentZoom) {
        return getRenderX(poi.getLocation().getX(), mapCenterX, centerX, currentZoom);
    }

    public static float getRenderX(int worldX, float mapCenterX, float centerX, float currentZoom) {
        double distanceX = worldX - mapCenterX;
        return (float) (centerX + distanceX * currentZoom);
    }

    /**
     * {@param poi} POI that we get the render coordinate for
     * {@param mapCenterZ} center coordinates of map (in-game coordinates)
     * {@param centerZ} center coordinates of map (screen render coordinates)
     * {@param currentZoom} the bigger, the more detailed the map is
     */
    public static float getRenderZ(Poi poi, float mapCenterZ, float centerZ, float currentZoom) {
        return getRenderZ(poi.getLocation().getZ(), mapCenterZ, centerZ, currentZoom);
    }

    public static float getRenderZ(int worldZ, float mapCenterZ, float centerZ, float currentZoom) {
        double distanceZ = worldZ - mapCenterZ;
        return (float) (centerZ + distanceZ * currentZoom);
    }

    private static void renderLootrun(
            LootrunInstance lootrun,
            float lootrunWidth,
            boolean outline,
            PoseStack poseStack,
            float centerX,
            float centerZ,
            float mapTextureX,
            float mapTextureZ,
            float currentZoom,
            float mapWidth,
            float mapHeight) {
        if (lastLootrun != lootrun) {
            lastLootrun = lootrun;
            filteredPathMap.clear();
        }
        // limit lod rebuilding by frame time
        final long frameStartTime = System.currentTimeMillis();
        // at 60 fps, each frame takes ~16ms
        final long maxMsPerFrame = 8;
        final long noRebuildAfter = frameStartTime + maxMsPerFrame;
        for (List<ColoredPath> value : lootrun.points().values()) {
            for (ColoredPath path : value) {
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                // default to cached lod
                PathCacheEntry cacheEntry = filteredPathMap.get(path);
                List<ColoredPosition> points = cacheEntry == null ? Collections.emptyList() : cacheEntry.points;
                // check lod rebuilding
                if (System.currentTimeMillis() < noRebuildAfter) {
                    List<ColoredPosition> lod =
                            getPathLodChecked(path, mapTextureX, mapTextureZ, currentZoom, mapWidth, mapHeight);
                    if (lod != null) {
                        points = lod;
                    }
                }
                for (int i = 0; i < points.size() - 1; i++) {
                    ColoredPosition point1 = points.get(i);
                    Vec3 pos1 = point1.position();
                    float renderX1 = getRenderX((int) pos1.x(), mapTextureX, centerX, currentZoom);
                    float renderZ1 = getRenderZ((int) pos1.z(), mapTextureZ, centerZ, currentZoom);

                    ColoredPosition point2 = points.get(i + 1);
                    Vec3 pos2 = point2.position();
                    float renderX2 = getRenderX((int) pos2.x(), mapTextureX, centerX, currentZoom);
                    float renderZ2 = getRenderZ((int) pos2.z(), mapTextureZ, centerZ, currentZoom);

                    drawLine(
                            poseStack,
                            bufferBuilder,
                            outline ? CommonColors.BLACK.asInt() : point1.color(),
                            renderX1,
                            renderZ1,
                            renderX2,
                            renderZ2,
                            0,
                            lootrunWidth);
                }

                BufferUploader.drawWithShader(bufferBuilder.end());
            }
        }
    }

    // Taken from RenderUtils and modified for this use case
    private static void drawLine(
            PoseStack poseStack,
            BufferBuilder bufferBuilder,
            int color,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            float width) {
        Matrix4f matrix = poseStack.last().pose();

        float halfWidth = width / 2;

        if (x1 == x2) {
            if (y2 < y1) {
                float tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            bufferBuilder.vertex(matrix, x1 - halfWidth, y1, z).color(color).endVertex();
            bufferBuilder.vertex(matrix, x2 - halfWidth, y2, z).color(color).endVertex();
            bufferBuilder.vertex(matrix, x1 + halfWidth, y1, z).color(color).endVertex();
            bufferBuilder.vertex(matrix, x2 + halfWidth, y2, z).color(color).endVertex();
        } else if (y1 == y2) {
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            bufferBuilder.vertex(matrix, x1, y1 - halfWidth, z).color(color).endVertex();
            bufferBuilder.vertex(matrix, x1, y1 + halfWidth, z).color(color).endVertex();
            bufferBuilder.vertex(matrix, x2, y2 - halfWidth, z).color(color).endVertex();
            bufferBuilder.vertex(matrix, x2, y2 + halfWidth, z).color(color).endVertex();
        } else if ((x1 < x2 && y1 < y2) || (x2 < x1 && y2 < y1)) { // Top Left to Bottom Right line
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            bufferBuilder
                    .vertex(matrix, x1 + halfWidth, y1 - halfWidth, z)
                    .color(color)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1 + halfWidth, z)
                    .color(color)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2 - halfWidth, z)
                    .color(color)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2 + halfWidth, z)
                    .color(color)
                    .endVertex();
        } else { // Top Right to Bottom Left Line
            if (x1 < x2) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            bufferBuilder
                    .vertex(matrix, x1 + halfWidth, y1 + halfWidth, z)
                    .color(color)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1 - halfWidth, z)
                    .color(color)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2 + halfWidth, z)
                    .color(color)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2 - halfWidth, z)
                    .color(color)
                    .endVertex();
        }
    }

    private static List<ColoredPosition> getPathLodChecked(
            ColoredPath path,
            float mapTextureX,
            float mapTextureZ,
            float currentZoom,
            float mapWidth,
            float mapHeight) {
        PathCacheEntry cacheEntry = filteredPathMap.get(path);
        float granularity = 1f / currentZoom;
        // rebuild if not in cache or ...
        boolean needsRebuild = cacheEntry == null
                // zoom changed enough or ...
                || Math.abs(1f - cacheEntry.zoom / currentZoom) > .1f
                // map panning changed enough
                || Math.abs(cacheEntry.mapX - mapTextureX) > granularity
                || Math.abs(cacheEntry.mayZ - mapTextureZ) > granularity;
        if (needsRebuild) {
            List<ColoredPosition> lod = buildPathLod(path, currentZoom, mapWidth, mapHeight, mapTextureX, mapTextureZ);
            filteredPathMap.put(path, new PathCacheEntry(lod, currentZoom, mapTextureX, mapTextureZ));
            return lod;
        } else {
            return null;
        }
    }

    private static List<ColoredPosition> buildPathLod(
            ColoredPath path,
            float currentZoom,
            float mapWidth,
            float mapHeight,
            float mapTextureX,
            float mapTextureZ) {
        // first filter by visible/drawn area
        float granularity = 1f / currentZoom;
        float renderedWorldWidth = mapWidth * granularity;
        float renderedWorldHeight = mapHeight * granularity;
        float drawnAreaStartX = mapTextureX - renderedWorldWidth / 2;
        float drawnAreaStartZ = mapTextureZ - renderedWorldHeight / 2;
        float drawnAreaEndX = mapTextureX + renderedWorldWidth / 2;
        float drawnAreaEndZ = mapTextureZ + renderedWorldHeight / 2;
        Predicate<ColoredPosition> areaFilter = point -> {
            Vec3 pos = point.position();
            return pos.x > drawnAreaStartX && pos.x < drawnAreaEndX && pos.z > drawnAreaStartZ && pos.z < drawnAreaEndZ;
        };
        List<ColoredPosition> areaFilteredPoints =
                path.points().stream().filter(areaFilter).toList();
        // then filter unnoticeable intermediate path
        List<ColoredPosition> sparsePoints = new ArrayList<>();
        if (!areaFilteredPoints.isEmpty()) {
            sparsePoints.add(areaFilteredPoints.get(0));
            Vec3 lastPos = sparsePoints.get(0).position();
            for (ColoredPosition colPos : areaFilteredPoints) {
                if (!lastPos.closerThan(colPos.position(), granularity)) {
                    lastPos = colPos.position();
                    sparsePoints.add(colPos);
                }
            }
            ColoredPosition lastSparsePoint = sparsePoints.get(sparsePoints.size() - 1);
            ColoredPosition lastFilteredPoint = areaFilteredPoints.get(areaFilteredPoints.size() - 1);
            if (areaFilteredPoints.size() > 1 && lastSparsePoint != lastFilteredPoint) {
                sparsePoints.add(lastFilteredPoint);
            }
        }
        return sparsePoints;
    }

    private record PathCacheEntry(List<ColoredPosition> points, float zoom, float mapX, float mayZ) {}
}
