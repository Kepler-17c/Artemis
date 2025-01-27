/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.map.pois;

import com.wynntils.models.map.PoiLocation;
import com.wynntils.models.map.type.DisplayPriority;
import com.wynntils.utils.render.Texture;
import java.util.function.Supplier;

public class PointerPoi extends DynamicIconPoi {
    public PointerPoi(Supplier<PoiLocation> locationSupplier) {
        super(locationSupplier);
    }

    @Override
    public Texture getIcon() {
        return Texture.POINTER;
    }

    @Override
    public float getMinZoomForRender() {
        return -1f;
    }

    @Override
    public String getName() {
        return "Waypoint";
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.NORMAL;
    }
}
