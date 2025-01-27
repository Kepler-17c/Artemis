/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.CodedString;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.container.ScriptedContainerQuery;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.MenuEvent.MenuClosedEvent;
import com.wynntils.mc.event.PlayerTeleportEvent;
import com.wynntils.models.character.event.CharacterDeathEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Position;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CharacterModel extends Model {
    private static final Pattern CLASS_MENU_CLASS_PATTERN = Pattern.compile("§e- §r§7Class: §r§f(.+)");
    private static final Pattern CLASS_MENU_LEVEL_PATTERN = Pattern.compile("§e- §r§7Level: §r§f(\\d+)");
    private static final Pattern INFO_MENU_CLASS_PATTERN = Pattern.compile("§7Class: §r§f(.+)");
    private static final Pattern INFO_MENU_LEVEL_PATTERN = Pattern.compile("§7Combat Lv: §r§f(\\d+)");

    private static final int CHARACTER_INFO_SLOT = 7;
    private static final int SOUL_POINT_SLOT = 8;
    private static final int PROFESSION_INFO_SLOT = 17;

    // we need a .* in front because the message may have a custom timestamp prefix (or some other mod could do
    // something weird)
    private static final Pattern WYNN_DEATH_MESSAGE = Pattern.compile(".*§r §4§lYou have died\\.\\.\\.");
    private Position lastPositionBeforeTeleport;
    private Location lastDeathLocation;

    private boolean inCharacterSelection;
    private boolean hasCharacter;

    private ClassType classType;
    private boolean reskinned;
    private int level;

    // This field is basically the slot id of the class,
    // meaning that if a class changes slots, the ID will not be persistent.
    // This was implemented the same way by legacy.
    private String id = "-";

    public CharacterModel() {
        super(List.of());
    }

    public ClassType getClassType() {
        if (!hasCharacter) return ClassType.NONE;

        return classType;
    }

    public boolean isReskinned() {
        if (!hasCharacter) return false;

        return reskinned;
    }

    /** Returns the current class name, wrt reskinned or not.
     */
    public String getActualName() {
        return getClassType().getActualName(isReskinned());
    }

    public String getId() {
        // We can't return an empty string, otherwise we risk making our config file messed up (empty string map key for
        // ItemLockFeature)
        if (!hasCharacter) return "-";

        return id;
    }

    @SubscribeEvent
    public void onMenuClosed(MenuClosedEvent e) {
        inCharacterSelection = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldStateChanged(WorldStateEvent e) {
        // Whenever we're leaving a world, clear the current character
        if (e.getOldState() == WorldState.WORLD) {
            hasCharacter = false;
            // This should not be needed, but have it as a safeguard
            inCharacterSelection = false;
        }

        if (e.getNewState() == WorldState.CHARACTER_SELECTION) {
            inCharacterSelection = true;
        }

        if (e.getNewState() == WorldState.WORLD) {
            WynntilsMod.info("Scheduling character info query");

            // We need to scan character info and profession info as well.
            scanCharacterInfoPage();

            // We need to parse the current character id from our inventory
            updateCharacterId();
        }
    }

    private void scanCharacterInfoPage() {
        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Character Info Query")
                .useItemInHotbar(InventoryUtils.COMPASS_SLOT_NUM)
                .matchTitle("Character Info")
                .processContainer(container -> {
                    ItemStack characterInfoItem = container.items().get(CHARACTER_INFO_SLOT);
                    ItemStack professionInfoItem = container.items().get(PROFESSION_INFO_SLOT);

                    Models.Profession.resetValueFromItem(professionInfoItem);

                    parseCharacterFromCharacterMenu(characterInfoItem);
                    hasCharacter = true;
                    WynntilsMod.postEvent(new CharacterUpdateEvent());
                    WynntilsMod.info("Deducing character " + getCharacterString());
                })
                .onError(msg -> WynntilsMod.warn("Error querying Character Info:" + msg))
                .build();
        query.executeQuery();
    }

    private void updateCharacterId() {
        ItemStack soulPointItem = McUtils.inventory().items.get(SOUL_POINT_SLOT);

        List<CodedString> soulLore = LoreUtils.getLore(soulPointItem);

        String id = "";
        for (CodedString line : soulLore) {
            if (line.startsWith(ChatFormatting.DARK_GRAY.toString())) {
                id = ComponentUtils.stripFormatting(line);
                break;
            }
        }

        WynntilsMod.info("Selected character: " + id);

        this.id = id;
    }

    private String getCharacterString() {
        return "CharacterInfo{" + "classType="
                + classType + ", reskinned="
                + reskinned + ", level="
                + level + ", id="
                + id + '}';
    }

    private void parseCharacterFromCharacterMenu(ItemStack characterInfoItem) {
        List<CodedString> lore = LoreUtils.getLore(characterInfoItem);

        int level = 0;
        String className = "";

        for (CodedString line : lore) {
            Matcher levelMatcher = line.getMatcher(INFO_MENU_LEVEL_PATTERN);
            if (levelMatcher.matches()) {
                level = Integer.parseInt(levelMatcher.group(1));
                continue;
            }

            Matcher classMatcher = line.getMatcher(INFO_MENU_CLASS_PATTERN);

            if (classMatcher.matches()) {
                className = classMatcher.group(1);
            }
        }
        ClassType classType = ClassType.fromName(className);

        updateCharacterInfo(classType, classType != null && ClassType.isReskinned(className), level);
    }

    @SubscribeEvent
    public void onContainerClick(ContainerClickEvent e) {
        if (inCharacterSelection) {
            if (e.getItemStack().getItem() == Items.AIR) return;
            parseCharacter(e.getItemStack(), e.getSlotNum());
            hasCharacter = true;
            WynntilsMod.postEvent(new CharacterUpdateEvent());
            WynntilsMod.info("Selected character " + getCharacterString());
        }
    }

    private void parseCharacter(ItemStack itemStack, int id) {
        List<CodedString> lore = LoreUtils.getLore(itemStack);

        int level = 0;
        String className = "";

        for (CodedString line : lore) {
            Matcher levelMatcher = line.getMatcher(CLASS_MENU_LEVEL_PATTERN);
            if (levelMatcher.matches()) {
                level = Integer.parseInt(levelMatcher.group(1));
                continue;
            }

            Matcher classMatcher = line.getMatcher(CLASS_MENU_CLASS_PATTERN);

            if (classMatcher.matches()) {
                className = classMatcher.group(1);
            }
        }
        ClassType classType = ClassType.fromName(className);

        updateCharacterInfo(classType, classType != null && ClassType.isReskinned(className), level);
    }

    private void updateCharacterInfo(ClassType classType, boolean reskinned, int level) {
        this.classType = classType;
        this.reskinned = reskinned;
        this.level = level;
    }

    @SubscribeEvent
    public void onChatReceived(ChatMessageReceivedEvent e) {
        if (!e.getCodedMessage().getMatcher(WYNN_DEATH_MESSAGE).matches()) return;
        lastDeathLocation = Location.containing(lastPositionBeforeTeleport);
        WynntilsMod.postEvent(new CharacterDeathEvent(lastDeathLocation));
    }

    @SubscribeEvent
    public void beforePlayerTeleport(PlayerTeleportEvent e) {
        if (McUtils.player() == null) return;
        lastPositionBeforeTeleport = McUtils.player().position();
    }
}
