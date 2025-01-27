/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.CodedString;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.wynn.WynnUtils;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;

public final class ContainerModel extends Model {
    public static final Pattern ABILITY_TREE_PATTERN =
            Pattern.compile("(?:Warrior|Shaman|Mage|Assassin|Archer) Abilities");

    // Test suite: https://regexr.com/7b4lf
    private static final Pattern GUILD_BANK_PATTERN =
            Pattern.compile("[a-zA-Z ]+: Bank \\((?:Everyone|High Ranked)\\)");

    private static final Pattern LOOT_CHEST_PATTERN = Pattern.compile("Loot Chest (.+)");

    // Test suite: https://regexr.com/7c4qc
    private static final Pattern PERSONAL_STORAGE_PATTERN =
            Pattern.compile("^§0\\[Pg\\. (\\d+)\\] §8[a-zA-Z0-9_]+'s?§0 (.*)$");

    private static final String BANK_NAME = "Bank";
    private static final String BLOCK_BANK_NAME = "Block Bank";
    private static final String MISC_BUCKET_NAME = "Misc. Bucket";

    private static final Pair<Integer, Integer> ABILITY_TREE_PREVIOUS_NEXT_SLOTS = new Pair<>(57, 59);
    private static final Pair<Integer, Integer> BANK_PREVIOUS_NEXT_SLOTS = new Pair<>(17, 8);
    private static final Pair<Integer, Integer> GUILD_BANK_PREVIOUS_NEXT_SLOTS = new Pair<>(9, 27);
    private static final Pair<Integer, Integer> TRADE_MARKET_PREVIOUS_NEXT_SLOTS = new Pair<>(17, 26);
    private static final CodedString LAST_BANK_PAGE_STRING = CodedString.fromString(">§4>§c>§4>§c>");
    private static final CodedString FIRST_TRADE_MARKET_PAGE_STRING = CodedString.fromString("§bReveal Item Names");
    private static final CodedString TRADE_MARKET_TITLE = CodedString.fromString("Trade Market");
    private static final StyledText SEASKIPPER_TITLE = StyledText.fromString("V.S.S. Seaskipper");

    public ContainerModel() {
        super(List.of());
    }

    public boolean isAbilityTreeScreen(Screen screen) {
        return ABILITY_TREE_PATTERN.matcher(screen.getTitle().getString()).matches();
    }

    public boolean isBankScreen(Screen screen) {
        Matcher matcher = ComponentUtils.getCoded(screen.getTitle()).getMatcher(PERSONAL_STORAGE_PATTERN);
        if (!matcher.matches()) return false;

        String type = matcher.group(2);
        return type.equals(BANK_NAME);
    }

    /**
     * @return True if the page is the last page in a Bank, Block Bank, or Misc Bucket
     */
    public boolean isLastBankPage(Screen screen) {
        return (isBankScreen(screen) || isBlockBankScreen(screen) || isMiscBucketScreen(screen))
                && screen instanceof ContainerScreen cs
                && ComponentUtils.getCoded(cs.getMenu().getSlot(8).getItem().getHoverName())
                        .endsWith(LAST_BANK_PAGE_STRING);
    }

    public boolean isGuildBankScreen(Screen screen) {
        return ComponentUtils.getCoded(screen.getTitle())
                .getMatcher(GUILD_BANK_PATTERN)
                .matches();
    }

    public boolean isTradeMarketScreen(Screen screen) {
        if (!(screen instanceof ContainerScreen cs)) return false;
        // No regex required, title is very simple and can be checked with .equals()
        return cs.getMenu().getRowCount() == 6
                && ComponentUtils.getCoded(screen.getTitle()).equals(TRADE_MARKET_TITLE);
    }

    public boolean isFirstTradeMarketPage(Screen screen) {
        return isTradeMarketScreen(screen)
                && screen instanceof ContainerScreen cs
                && ComponentUtils.getCoded(cs.getMenu().getSlot(17).getItem().getHoverName())
                        .equals(FIRST_TRADE_MARKET_PAGE_STRING);
    }

    public boolean isBlockBankScreen(Screen screen) {
        Matcher matcher = ComponentUtils.getCoded(screen.getTitle()).getMatcher(PERSONAL_STORAGE_PATTERN);
        if (!matcher.matches()) return false;

        String type = matcher.group(2);
        return type.equals(BLOCK_BANK_NAME);
    }

    public boolean isMiscBucketScreen(Screen screen) {
        Matcher matcher = ComponentUtils.getCoded(screen.getTitle()).getMatcher(PERSONAL_STORAGE_PATTERN);
        if (!matcher.matches()) return false;

        String type = matcher.group(2);
        return type.equals(MISC_BUCKET_NAME);
    }

    public boolean isLootChest(Screen screen) {
        return screen instanceof ContainerScreen && lootChestMatcher(screen).matches();
    }

    public boolean isLootChest(String title) {
        return title.startsWith("Loot Chest");
    }

    public boolean isLootOrRewardChest(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;

        String title = screen.getTitle().getString();
        return isLootChest(title) || title.startsWith("Daily Rewards") || title.contains("Objective Rewards");
    }

    public boolean isSeaskipper(Component component) {
        return StyledText.fromComponent(component).equals(SEASKIPPER_TITLE);
    }

    public Matcher lootChestMatcher(Screen screen) {
        return LOOT_CHEST_PATTERN.matcher(
                WynnUtils.normalizeBadString(ComponentUtils.getUnformatted(screen.getTitle())));
    }

    public Optional<Integer> getScrollSlot(AbstractContainerScreen<?> gui, boolean scrollUp) {
        Pair<Integer, Integer> slots = getScrollSlots(gui, scrollUp);
        if (slots == null) return Optional.empty();

        return Optional.of(scrollUp ? slots.a() : slots.b());
    }

    private Pair<Integer, Integer> getScrollSlots(AbstractContainerScreen<?> gui, boolean scrollUp) {
        if (Models.Container.isAbilityTreeScreen(gui)) {
            return ABILITY_TREE_PREVIOUS_NEXT_SLOTS;
        }

        if (Models.Container.isBankScreen(gui)
                || Models.Container.isMiscBucketScreen(gui)
                || Models.Container.isBlockBankScreen(gui)) {
            if (!scrollUp && Models.Container.isLastBankPage(gui)) return null;

            return BANK_PREVIOUS_NEXT_SLOTS;
        }

        if (Models.Container.isGuildBankScreen(gui)) {
            return GUILD_BANK_PREVIOUS_NEXT_SLOTS;
        }

        if (Models.Container.isTradeMarketScreen(gui)) {
            if (scrollUp && Models.Container.isFirstTradeMarketPage(gui)) return null;

            return TRADE_MARKET_PREVIOUS_NEXT_SLOTS;
        }

        return null;
    }
}
