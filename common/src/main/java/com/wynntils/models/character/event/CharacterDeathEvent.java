/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character.event;

import com.wynntils.utils.mc.type.Location;
import net.minecraftforge.eventbus.api.Event;

public class CharacterDeathEvent extends Event {

    private final Location location;

    public CharacterDeathEvent(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
