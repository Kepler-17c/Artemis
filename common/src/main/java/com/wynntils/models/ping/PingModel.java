/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.ping;

import com.wynntils.core.components.Model;
import com.wynntils.mc.event.PacketEvent;
import com.wynntils.models.worlds.WorldStateModel;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PingModel extends Model {
    private static final int MS_PER_PING = 1000;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private long lastPingSent = 0;
    private int lastPing = 0;

    public PingModel(WorldStateModel worldState) {
        super(List.of(worldState));
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.WORLD) {
            executor.scheduleAtFixedRate(this::sendPingPacket, 0, MS_PER_PING, TimeUnit.MILLISECONDS);
        } else {
            executor.shutdownNow();
            executor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    // We are specifically looking for the packet itself, not it's processing. This is why we are using the PacketEvent.
    @SubscribeEvent
    public void onCommandSuggestions(PacketEvent.PacketReceivedEvent<ClientboundCommandSuggestionsPacket> event) {
        if (event.getPacket().getId() != -1) return;

        lastPing = (int) (System.currentTimeMillis() - lastPingSent);

        // We also need to cancel the packet, as it would be unexpected to Minecraft.
        event.setCanceled(true);
    }

    private void sendPingPacket() {
        // We use -1 as the id, as it is not used by Minecraft.
        McUtils.mc().getConnection().send(new ServerboundCommandSuggestionPacket(-1, ""));
        lastPingSent = System.currentTimeMillis();
    }

    public int getPing() {
        return lastPing;
    }
}
