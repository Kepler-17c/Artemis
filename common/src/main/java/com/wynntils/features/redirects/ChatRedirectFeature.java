/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.redirects;

import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.features.Feature;
import com.wynntils.core.text.CodedString;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.utils.wynn.WynnPlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.REDIRECTS)
public class ChatRedirectFeature extends Feature {
    @RegisterConfig
    public final Config<RedirectAction> craftedDurability = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> friendJoin = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> heal = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> horse = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> housingTeleport = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> ingredientPouch = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> loginAnnouncements = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> mageTeleport = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> notEnoughMana = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> potion = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> scrollTeleport = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> shaman = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> soulPoint = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> speed = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> toolDurability = new Config<>(RedirectAction.REDIRECT);

    @RegisterConfig
    public final Config<RedirectAction> unusedPoints = new Config<>(RedirectAction.REDIRECT);

    private final List<Redirector> redirectors = new ArrayList<>();

    public ChatRedirectFeature() {
        register(new CraftedDurabilityRedirector());
        register(new FriendJoinRedirector());
        register(new FriendLeaveRedirector());
        register(new HealRedirector());
        register(new HealedByOtherRedirector());
        register(new HorseDespawnedRedirector());
        register(new HorseScaredRedirector());
        register(new HorseSpawnFailRedirector());
        register(new HousingTeleportArrivalRedirector());
        register(new HousingTeleportArrivalCooldownRedirector());
        register(new HousingTeleportDepartureRedirector());
        register(new HousingTeleportDepartureCooldownRedirector());
        register(new IngredientPouchSellRedirector());
        register(new LoginRedirector());
        register(new MageTeleportationFailRedirector());
        register(new ManaDeficitRedirector());
        register(new NoTotemRedirector());
        register(new PotionAlreadyActiveRedirector());
        register(new PotionsMaxRedirector());
        register(new PotionsMovedRedirector());
        register(new PotionsReplacedRedirector());
        register(new ScrollTeleportationHousingFailRedirector());
        register(new ScrollTeleportationMobFailRedirector());
        register(new SoulPointGainDiscarder());
        register(new SoulPointGainRedirector());
        register(new SoulPointLossRedirector());
        register(new SpeedBoostRedirector());
        register(new ToolDurabilityRedirector());
        register(new UnusedAbilityPointsRedirector());
        register(new UnusedSkillAndAbilityPointsRedirector());
        register(new UnusedSkillPointsRedirector());
    }

    private void register(Redirector redirector) {
        redirectors.add(redirector);
    }

    @SubscribeEvent
    public void onChatMessage(ChatMessageReceivedEvent e) {
        if (e.getRecipientType() != RecipientType.INFO) return;

        CodedString message = e.getOriginalCodedString();
        MessageType messageType = e.getMessageType();

        for (Redirector redirector : redirectors) {
            RedirectAction action = redirector.getAction();
            if (action == RedirectAction.KEEP) continue;

            Pattern pattern = redirector.getPattern(messageType);
            if (pattern == null) continue;

            Matcher matcher = message.getMatcher(pattern);

            if (matcher.find()) {
                e.setCanceled(true);
                if (redirector.getAction() == RedirectAction.HIDE) continue;

                for (CodedString notification : redirector.getNotifications(matcher)) {
                    Managers.Notification.queueMessage(notification);
                }
            }
        }
    }

    public enum RedirectAction {
        KEEP,
        HIDE,
        REDIRECT
    }

    protected interface Redirector {
        Pattern getPattern(MessageType messageType);

        ChatRedirectFeature.RedirectAction getAction();

        List<CodedString> getNotifications(Matcher matcher);
    }

    public abstract static class SimpleRedirector implements Redirector {
        @Override
        public Pattern getPattern(MessageType messageType) {
            return switch (messageType) {
                case FOREGROUND -> getForegroundPattern();
                case BACKGROUND -> getBackgroundPattern();
            };
        }

        protected Pattern getForegroundPattern() {
            return null;
        }

        protected Pattern getBackgroundPattern() {
            return null;
        }

        @Override
        public List<CodedString> getNotifications(Matcher matcher) {
            return List.of(getNotification(matcher));
        }

        protected abstract CodedString getNotification(Matcher matcher);
    }

    private class CraftedDurabilityRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§cYour items are damaged and have become less effective. Bring them to a Blacksmith to repair them.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return craftedDurability.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Your items are damaged.");
        }
    }

    private class FriendJoinRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "§a(§o)?(?<name>.+)§r§2 has logged into server §r§a(?<server>.+)§r§2 as §r§aan? (?<class>.+)");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile(
                "§r§7(§o)?(?<name>.+)§r§8(§o)? has logged into server §r§7(§o)?(?<server>.+)§r§8(§o)? as §r§7(§o)?an? (?<class>.+)");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return friendJoin.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String playerName = matcher.group("name");
            String server = matcher.group("server");
            String playerClass = matcher.group("class");

            return CodedString.fromString(ChatFormatting.GREEN + "→ " + ChatFormatting.DARK_GREEN
                    + playerName + " [" + ChatFormatting.GREEN
                    + server + "/" + playerClass + ChatFormatting.DARK_GREEN
                    + "]");
        }
    }

    private class FriendLeaveRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§a(?<name>.+) left the game.");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile("§r§7(?<name>.+) left the game.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return friendJoin.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String playerName = matcher.group("name");

            return CodedString.fromString(ChatFormatting.RED + "← " + ChatFormatting.DARK_GREEN + playerName);
        }
    }

    private class HealRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§c\\[\\+(\\d+) ❤\\]$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return heal.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String amount = matcher.group(1);

            return CodedString.fromString(ChatFormatting.DARK_RED + "[+" + amount + " ❤]");
        }
    }

    private class HealedByOtherRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^.+ gave you §r§c\\[\\+(\\d+) ❤\\]$");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile("^.+ gave you §r§7§o\\[\\+(\\d+) ❤\\]$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return heal.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String amount = matcher.group(1);

            return CodedString.fromString(ChatFormatting.DARK_RED + "[+" + amount + " ❤]");
        }
    }

    private class HorseDespawnedRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§dSince you interacted with your inventory, your horse has despawned.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return horse.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_PURPLE + "Your horse has despawned.");
        }
    }

    private class HorseScaredRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§dYour horse is scared to come out right now, too many mobs are nearby.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return horse.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Nearby mobs prevent horse spawning!");
        }
    }

    private class HorseSpawnFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§4There is no room for a horse.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return horse.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "No room for a horse!");
        }
    }

    private class HousingTeleportArrivalRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§aYou have flown to your housing island.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.GRAY + "→ Housing Island");
        }
    }

    private class HousingTeleportArrivalCooldownRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou need to wait a bit before joining another house.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Housing teleport is on cooldown!");
        }
    }

    private class HousingTeleportDepartureRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§aYou have flown to your original position.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.GRAY + "← Housing Island");
        }
    }

    private class HousingTeleportDepartureCooldownRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou need to wait a bit before leaving a house.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return housingTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Housing teleport is on cooldown!");
        }
    }

    private class IngredientPouchSellRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§dYou have sold §r§7(.+)§r§d ingredients for a total of §r§a(.+)§r§d\\.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return ingredientPouch.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            int ingredientCount = Integer.parseInt(matcher.group(1));
            CodedString ingredientString =
                    CodedString.fromString(ingredientCount + " §dingredient" + (ingredientCount == 1 ? "" : "s"));

            String emeraldString = matcher.group(2);

            return CodedString.fromString(String.format(
                    "§dSold §7%s §dfor §a%s§d.",
                    ingredientString.getInternalCodedStringRepresentation(), emeraldString));
        }
    }

    private class LoginRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§.\\[§r(§.)+\\|?(§.)*(?<rank>[a-zA-Z+]+)§r(§.)+\\|?(§.)*\\] §r§.(?<name>.*)§r§. has just logged in!$");
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile(
                "^(?:§r§8)?\\[§r(§.)+\\|?(§.)*(?<rank>[a-zA-Z+]+)§r(§.)+\\|?(§.)*\\] §r§7(?<name>.*)§r§8 has just logged in!$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return loginAnnouncements.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String rank = matcher.group("rank");
            String playerName = matcher.group("name");

            return CodedString.fromString(
                    ChatFormatting.GREEN + "→ " + WynnPlayerUtils.getFormattedRank(rank) + playerName);
        }
    }

    private class MageTeleportationFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cSorry, you can't teleport... Try moving away from blocks.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return mageTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Unable to teleport! Move away from blocks.");
        }
    }

    private class ManaDeficitRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§4You don't have enough mana to cast that spell!$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return notEnoughMana.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Not enough mana to do that spell!");
        }
    }

    private class NoTotemRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§4You have no active totems near you$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return shaman.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "No totems nearby!");
        }
    }

    private class PotionAlreadyActiveRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§cYou already have that potion active...$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "This potion is already active!");
        }
    }

    private class PotionsMaxRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§4You already are holding the maximum amount of potions allowed.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "At potion charge limit!");
        }
    }

    private class PotionsMovedRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§7You already are holding the maximum amount of potions allowed so your crafting result was moved to your bank.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.GRAY + "Moved excess healing items to bank.");
        }
    }

    private class PotionsReplacedRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("§7One less powerful potion was replaced to open space for the added one.");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return potion.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.GRAY + "Lesser potion replaced.");
        }
    }

    private class ScrollTeleportationHousingFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§cYou can not teleport while inside a house...$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return scrollTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Can't teleport on a housing island!");
        }
    }

    private class ScrollTeleportationMobFailRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("§cThere are aggressive mobs nearby...$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return scrollTeleport.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Nearby mobs prevent scroll teleportation!");
        }
    }

    private class SoulPointGainDiscarder implements Redirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§5As the sun rises, you feel a little bit safer...$");
        private static final Pattern BACKGROUND_PATTERN =
                Pattern.compile("^(§r§8)?As the sun rises, you feel a little bit safer...$");

        @Override
        public Pattern getPattern(MessageType messageType) {
            return switch (messageType) {
                case BACKGROUND -> BACKGROUND_PATTERN;
                case FOREGROUND -> FOREGROUND_PATTERN;
            };
        }

        @Override
        public RedirectAction getAction() {
            return soulPoint.get();
        }

        @Override
        public List<CodedString> getNotifications(Matcher matcher) {
            // Soul point messages comes in two lines. We just throw away the chatty one
            // if we have hide or redirect as action.
            return List.of();
        }
    }

    private class SoulPointGainRedirector extends SimpleRedirector {
        private static final Pattern BACKGROUND_PATTERN = Pattern.compile("^§r§7\\[(\\+\\d+ Soul Points?)\\]$");
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§d\\[(\\+\\d+ Soul Points?)\\]$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        protected Pattern getBackgroundPattern() {
            return BACKGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return soulPoint.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            // Send the matching part, which could be +1 Soul Point or +2 Soul Points, etc.
            return CodedString.fromString(ChatFormatting.LIGHT_PURPLE + matcher.group(1));
        }
    }

    private class SoulPointLossRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN =
                Pattern.compile("^§[47](\\d+) soul points? (has|have) been lost...$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return soulPoint.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String numberString = matcher.group(1);
            String pluralizer = "";

            int numberValue = Integer.parseInt(numberString);
            if (numberValue > 1) {
                pluralizer = "s";
            }

            return CodedString.fromString(String.format("§4-%s Soul Point%s", numberString, pluralizer));
        }
    }

    private class SpeedBoostRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile("^§b\\+([23]) minutes§r§7 speed boost.$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return speed.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String minutesString = String.format("+%s minutes", matcher.group(1));
            return CodedString.fromString(ChatFormatting.AQUA + minutesString + ChatFormatting.GRAY + " speed boost");
        }
    }

    private class ToolDurabilityRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4Your tool has 0 durability left! You will not receive any new resources until you repair it at a Blacksmith.$");

        @Override
        public Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return toolDurability.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "Your tool has 0 durability!");
        }
    }

    private class UnusedAbilityPointsRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4You have §r§b§l(\\d+) unused Ability Points?! §r§4Right-Click while holding your compass to use them$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return unusedPoints.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String unusedAbilityPoints = matcher.group(1);

            return getUnusedAbilityPointsMessage(unusedAbilityPoints);
        }

        protected static CodedString getUnusedAbilityPointsMessage(String unusedAbilityPoints) {
            return CodedString.fromString(ChatFormatting.DARK_AQUA + "You have " + ChatFormatting.BOLD
                    + unusedAbilityPoints + ChatFormatting.RESET + ChatFormatting.DARK_AQUA + " unused ability points");
        }
    }

    private class UnusedSkillAndAbilityPointsRedirector implements Redirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4You have §r§c§l(\\d+) unused Skill Points?§r§4 and §r§b§l(\\d+) unused Ability Points?! §r§4Right-Click while holding your compass to use them$");

        @Override
        public Pattern getPattern(MessageType messageType) {
            if (messageType == MessageType.FOREGROUND) {
                return FOREGROUND_PATTERN;
            } else {
                return null;
            }
        }

        @Override
        public RedirectAction getAction() {
            return unusedPoints.get();
        }

        @Override
        public List<CodedString> getNotifications(Matcher matcher) {
            String unusedSkillPoints = matcher.group(1);
            String unusedAbilityPoints = matcher.group(2);

            return List.of(
                    UnusedSkillPointsRedirector.getUnusedSkillPointsMessage(unusedSkillPoints),
                    UnusedAbilityPointsRedirector.getUnusedAbilityPointsMessage(unusedAbilityPoints));
        }
    }

    private class UnusedSkillPointsRedirector extends SimpleRedirector {
        private static final Pattern FOREGROUND_PATTERN = Pattern.compile(
                "^§4You have §r§c§l(\\d+) unused Skill Points?! §r§4Right-Click while holding your compass to use them$");

        @Override
        protected Pattern getForegroundPattern() {
            return FOREGROUND_PATTERN;
        }

        @Override
        public RedirectAction getAction() {
            return unusedPoints.get();
        }

        @Override
        protected CodedString getNotification(Matcher matcher) {
            String unusedSkillPoints = matcher.group(1);

            return getUnusedSkillPointsMessage(unusedSkillPoints);
        }

        protected static CodedString getUnusedSkillPointsMessage(String unusedSkillPoints) {
            return CodedString.fromString(ChatFormatting.DARK_RED + "You have " + ChatFormatting.BOLD
                    + unusedSkillPoints + ChatFormatting.RESET + ChatFormatting.DARK_RED + " unused skill points");
        }
    }
}
