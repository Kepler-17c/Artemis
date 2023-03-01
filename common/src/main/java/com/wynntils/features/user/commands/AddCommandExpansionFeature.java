/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.commands;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.event.CommandsPacketEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Set up Brigadier command structure of known Wynncraft commands.
 *
 * The commands in this file were extracted from https://wynncraft.fandom.com/wiki/Commands,
 * https://wynncraft.com/help?guide=commands, from running the commands in-game, and from a
 * list of server commands provided by HeyZeer0.
 */
@FeatureInfo(category = FeatureCategory.COMMANDS)
public class AddCommandExpansionFeature extends UserFeature {
    private static final SuggestionProvider<CommandSourceStack> PLAYER_NAME_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(Models.Player.getAllPlayerNames(), builder);

    private static final SuggestionProvider<CommandSourceStack> FRIEND_NAME_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(Models.Friends.getFriends(), builder);

    private static final SuggestionProvider<CommandSourceStack> PARTY_NAME_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Models.Party.getPartyMembers().stream()
                            .filter(p -> !p.equals(McUtils.player().getName().getString())),
                    builder);

    @Config
    public boolean includeDeprecatedCommands = false;

    @Config
    public AliasCommandLevel includeAliases = AliasCommandLevel.SHORT_FORMS;

    @SubscribeEvent
    public void onCommandPacket(CommandsPacketEvent event) {
        RootCommandNode root = event.getRoot();

        addArgumentlessCommandNodes(root);
        addChangetagCommandNode(root);
        addFriendCommandNode(root);
        addGuildCommandNode(root);
        addIgnoreCommandNode(root);
        addHousingCommandNode(root);
        addMessagingCommandNodes(root);
        addMiscCommandNodes(root);
        addParticlesCommandNode(root);
        addPartyCommandNode(root);
        addPlayerCommandNodes(root);
        addToggleCommandNode(root);

        if (includeDeprecatedCommands) {
            addDeprecatedCommandNodes(root);
        }
    }

    private void addAlias(
            RootCommandNode root,
            CommandNode<CommandSourceStack> originalNode,
            String aliasName,
            AliasCommandLevel level) {
        if (includeAliases.ordinal() >= level.ordinal()) {
            root.addChild(literal(aliasName).redirect(originalNode).build());
        }
    }

    private void addArgumentlessCommandNodes(RootCommandNode root) {
        root.addChild(literal("claimingredientbomb").build());
        root.addChild(literal("claimitembomb").build());
        root.addChild(literal("daily").build());
        root.addChild(literal("fixquests").build());
        root.addChild(literal("fixstart").build());
        root.addChild(literal("forum").build());
        root.addChild(literal("help").build());
        root.addChild(literal("rules").build());
        root.addChild(literal("scrap").build());
        root.addChild(literal("sign").build());
        root.addChild(literal("skiptutorial").build());
        root.addChild(literal("tracking").build());
        root.addChild(literal("use").build());

        // There is also a command "server" but it is reserved for those with admin permissions
        // only, so don't include it here.
        // The command "checknickname" is also available but is probably a defunct legacy command.

        // "hub" aliases
        CommandNode<CommandSourceStack> hubNode = literal("hub").build();
        root.addChild(hubNode);

        addAlias(root, hubNode, "change", AliasCommandLevel.ALL);
        addAlias(root, hubNode, "lobby", AliasCommandLevel.ALL);
        addAlias(root, hubNode, "leave", AliasCommandLevel.ALL);
        addAlias(root, hubNode, "port", AliasCommandLevel.ALL);

        // There is also an alias "servers" for "hub", but it conflicts with our command
        // so don't include it here

        // "class" aliases
        CommandNode<CommandSourceStack> classNode = literal("class").build();
        root.addChild(classNode);

        addAlias(root, classNode, "classes", AliasCommandLevel.ALL);

        // "crate" aliases
        CommandNode<CommandSourceStack> crateNode = literal("crate").build();
        root.addChild(crateNode);

        addAlias(root, crateNode, "crates", AliasCommandLevel.ALL);

        // "kill" aliases
        CommandNode<CommandSourceStack> killNode = literal("kill").build();
        root.addChild(killNode);

        addAlias(root, killNode, "die", AliasCommandLevel.ALL);
        addAlias(root, killNode, "suicide", AliasCommandLevel.ALL);

        // "itemlock" aliases
        CommandNode<CommandSourceStack> itemlockNode = literal("itemlock").build();
        root.addChild(itemlockNode);

        addAlias(root, itemlockNode, "ilock", AliasCommandLevel.ALL);
        addAlias(root, itemlockNode, "lock", AliasCommandLevel.ALL);
        addAlias(root, itemlockNode, "locki", AliasCommandLevel.ALL);
        addAlias(root, itemlockNode, "lockitem", AliasCommandLevel.ALL);

        // "pet" aliases
        CommandNode<CommandSourceStack> petNode = literal("pet").build();
        root.addChild(petNode);

        addAlias(root, petNode, "pets", AliasCommandLevel.ALL);

        // "partyfinder" aliases
        CommandNode<CommandSourceStack> partyfinderNode = literal("partyfinder").build();
        root.addChild(partyfinderNode);

        addAlias(root, partyfinderNode, "pfinder", AliasCommandLevel.SHORT_FORMS);

        // "silverbull" aliases
        CommandNode<CommandSourceStack> silverbullNode = literal("silverbull").build();
        root.addChild(silverbullNode);

        addAlias(root, silverbullNode, "shop", AliasCommandLevel.ALL);
        addAlias(root, silverbullNode, "store", AliasCommandLevel.ALL);
        addAlias(root, silverbullNode, "share", AliasCommandLevel.ALL);

        // "stream" aliases
        CommandNode<CommandSourceStack> streamNode = literal("stream").build();
        root.addChild(streamNode);

        addAlias(root, streamNode, "streamer", AliasCommandLevel.ALL);

        // "totem" aliases
        CommandNode<CommandSourceStack> totemNode = literal("totem").build();
        root.addChild(totemNode);

        addAlias(root, totemNode, "totems", AliasCommandLevel.ALL);

        // "hunted" aliases
        CommandNode<CommandSourceStack> huntedNode = literal("hunted").build();
        root.addChild(huntedNode);

        addAlias(root, huntedNode, "pvp", AliasCommandLevel.SHORT_FORMS);

        // "recruit" aliases
        CommandNode<CommandSourceStack> recruitNode = literal("recruit").build();
        root.addChild(recruitNode);

        addAlias(root, recruitNode, "rf", AliasCommandLevel.ALL);

        // "discord" aliases
        CommandNode<CommandSourceStack> discordNode = literal("discord").build();
        root.addChild(discordNode);

        addAlias(root, discordNode, "link", AliasCommandLevel.ALL);
    }

    private void addChangetagCommandNode(RootCommandNode root) {
        root.addChild(literal("changetag")
                .then(literal("VIP"))
                .then(literal("VIP+"))
                .then(literal("HERO"))
                .then(literal("CHAMPION"))
                .then(literal("RESET"))
                .build());
    }

    private void addFriendCommandNode(RootCommandNode root) {
        CommandNode<CommandSourceStack> node = literal("friend")
                .then(literal("list"))
                .then(literal("online"))
                .then(literal("add")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("remove")
                        .then(argument("player", StringArgumentType.word()).suggests(FRIEND_NAME_SUGGESTION_PROVIDER)))
                .build();
        root.addChild(node);

        addAlias(root, node, "f", AliasCommandLevel.SHORT_FORMS);
        addAlias(root, node, "friends", AliasCommandLevel.ALL);
        addAlias(root, node, "buddy", AliasCommandLevel.ALL);
        addAlias(root, node, "buddies", AliasCommandLevel.ALL);
    }

    private void addGuildCommandNode(RootCommandNode root) {
        CommandNode<CommandSourceStack> node = literal("guild")
                .then(literal("attack"))
                .then(literal("contribute"))
                .then(literal("defend"))
                .then(literal("invite")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("join").then(argument("tag", StringArgumentType.greedyString())))
                .then(literal("kick")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("leaderboard"))
                .then(literal("leave"))
                .then(literal("list"))
                .then(literal("log"))
                .then(literal("manage"))
                .then(literal("rank")
                        .then(argument("player", EntityArgument.players())
                                .suggests(PLAYER_NAME_SUGGESTION_PROVIDER)
                                .then(argument("rank", StringArgumentType.string()))))
                .then(literal("rewards"))
                .then(literal("stats"))
                .then(literal("territory"))
                .then(literal("xp").then(argument("amount", IntegerArgumentType.integer())))
                .build();
        root.addChild(node);

        addAlias(root, node, "gu", AliasCommandLevel.SHORT_FORMS);
        addAlias(root, node, "guilds", AliasCommandLevel.ALL);
    }

    private void addIgnoreCommandNode(RootCommandNode root) {
        root.addChild(literal("ignore")
                .then(literal("add")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("remove")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .build());
    }

    private void addHousingCommandNode(RootCommandNode root) {
        CommandNode<CommandSourceStack> node = literal("housing")
                .then(literal("allowedit")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("ban")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("disallowedit")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("edit"))
                .then(literal("invite")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("kick")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("kickall"))
                .then(literal("leave"))
                .then(literal("public"))
                .then(literal("unban")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("visit"))
                .build();
        root.addChild(node);

        addAlias(root, node, "is", AliasCommandLevel.SHORT_FORMS);
        addAlias(root, node, "hs", AliasCommandLevel.SHORT_FORMS);
        addAlias(root, node, "home", AliasCommandLevel.ALL);
        addAlias(root, node, "house", AliasCommandLevel.ALL);
        addAlias(root, node, "island", AliasCommandLevel.ALL);
        addAlias(root, node, "plot", AliasCommandLevel.ALL);
    }

    private void addMessagingCommandNodes(RootCommandNode root) {
        root.addChild(literal("g")
                .then(argument("msg", StringArgumentType.greedyString()))
                .build());

        root.addChild(literal("p")
                .then(argument("msg", StringArgumentType.greedyString()))
                .build());

        root.addChild(literal("r")
                .then(argument("msg", StringArgumentType.greedyString()))
                .build());

        CommandNode<CommandSourceStack> node = literal("msg")
                .then(argument("player", EntityArgument.players())
                        .suggests(PLAYER_NAME_SUGGESTION_PROVIDER)
                        .then(argument("msg", StringArgumentType.greedyString())))
                .build();
        root.addChild(node);
    }

    private void addMiscCommandNodes(RootCommandNode root) {
        root.addChild(literal("report")
                .then(argument("player", EntityArgument.players())
                        .suggests(PLAYER_NAME_SUGGESTION_PROVIDER)
                        .then(argument("reason", StringArgumentType.greedyString())))
                .build());

        root.addChild(literal("switch")
                .then(argument("world", IntegerArgumentType.integer()))
                .build());

        root.addChild(literal("relore")
                .then(argument("lore", StringArgumentType.greedyString()))
                .build());

        // first option is 0; not really supposed to be run by users
        root.addChild(literal("dialogue")
                .then(argument("option", IntegerArgumentType.integer()))
                .build());

        // not really supposed to be run by users
        root.addChild(literal("thankyou")
                .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER))
                .build());

        // "renameitem" aliases
        CommandNode<CommandSourceStack> renameitemNode = literal("renameitem")
                .then(argument("name", StringArgumentType.greedyString()))
                .build();
        root.addChild(renameitemNode);

        addAlias(root, renameitemNode, "renameitems", AliasCommandLevel.ALL);

        // "renameitem" aliases
        CommandNode<CommandSourceStack> renamepetNode = literal("renamepet")
                .then(argument("name", StringArgumentType.greedyString()))
                .build();
        root.addChild(renamepetNode);

        addAlias(root, renamepetNode, "renamepets", AliasCommandLevel.ALL);
    }

    private void addParticlesCommandNode(RootCommandNode root) {
        CommandNode<CommandSourceStack> node = literal("particles")
                .then(literal("off"))
                .then(literal("low"))
                .then(literal("medium"))
                .then(literal("high"))
                .then(literal("veryhigh"))
                .then(literal("highest"))
                .then(argument("particles_per_tick", IntegerArgumentType.integer()))
                .build();
        root.addChild(node);
        addAlias(root, node, "pq", AliasCommandLevel.SHORT_FORMS);
        addAlias(root, node, "particlequality", AliasCommandLevel.ALL);
        addAlias(root, node, "particlesquality", AliasCommandLevel.ALL);
    }

    private void addPartyCommandNode(RootCommandNode root) {
        CommandNode<CommandSourceStack> node = literal("party")
                .then(literal("ban")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("create"))
                .then(literal("disband"))
                .then(literal("finder"))
                .then(literal("invite")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("join")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .then(literal("kick")
                        .then(argument("player", EntityArgument.players()).suggests(PARTY_NAME_SUGGESTION_PROVIDER)))
                .then(literal("leave"))
                .then(literal("list"))
                .then(literal("promote")
                        .then(argument("player", EntityArgument.players()).suggests(PARTY_NAME_SUGGESTION_PROVIDER)))
                .then(literal("unban")
                        .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER)))
                .build();
        root.addChild(node);

        addAlias(root, node, "pa", AliasCommandLevel.SHORT_FORMS);
        addAlias(root, node, "group", AliasCommandLevel.ALL);
    }

    private void addPlayerCommandNodes(RootCommandNode root) {
        CommandNode<CommandSourceStack> duelNode = literal("duel")
                .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER))
                .build();
        root.addChild(duelNode);
        addAlias(root, duelNode, "d", AliasCommandLevel.SHORT_FORMS);

        CommandNode<CommandSourceStack> tradeNode = literal("trade")
                .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER))
                .build();
        root.addChild(tradeNode);
        addAlias(root, tradeNode, "tr", AliasCommandLevel.SHORT_FORMS);

        root.addChild(literal("find")
                .then(argument("player", EntityArgument.players()).suggests(PLAYER_NAME_SUGGESTION_PROVIDER))
                .build());
    }

    private void addToggleCommandNode(RootCommandNode root) {
        root.addChild(literal("toggle")
                .then(literal("100"))
                .then(literal("attacksound"))
                .then(literal("autojoin"))
                .then(literal("autotracking"))
                .then(literal("beacon"))
                .then(literal("blood"))
                .then(literal("bombbell"))
                .then(literal("combatbar"))
                .then(literal("friendpopups"))
                .then(literal("ghosts")
                        .then(literal("none"))
                        .then(literal("low"))
                        .then(literal("medium"))
                        .then(literal("high"))
                        .then(literal("all")))
                .then(literal("guildjoin"))
                .then(literal("guildpopups"))
                .then(literal("insults"))
                .then(literal("music"))
                .then(literal("outlines"))
                .then(literal("popups"))
                .then(literal("pouchmsg"))
                .then(literal("pouchpickup"))
                .then(literal("queststartbeacon"))
                .then(literal("rpwarning"))
                .then(literal("sb"))
                .then(literal("swears"))
                .then(literal("vet"))
                .then(literal("war"))
                .build());
    }

    private void addDeprecatedCommandNodes(RootCommandNode root) {
        // "legacystore" aliases
        CommandNode<CommandSourceStack> legacystoreNode = literal("legacystore").build();
        root.addChild(legacystoreNode);

        addAlias(root, legacystoreNode, "buy", AliasCommandLevel.ALL);
        addAlias(root, legacystoreNode, "cash", AliasCommandLevel.ALL);
        addAlias(root, legacystoreNode, "cashshop", AliasCommandLevel.ALL);
        addAlias(root, legacystoreNode, "gc", AliasCommandLevel.ALL);
        addAlias(root, legacystoreNode, "gold", AliasCommandLevel.ALL);
        addAlias(root, legacystoreNode, "goldcoins", AliasCommandLevel.ALL);
        addAlias(root, legacystoreNode, "goldshop", AliasCommandLevel.ALL);

        // "rename" aliases
        CommandNode<CommandSourceStack> renameNode = literal("rename").build();
        root.addChild(renameNode);

        addAlias(root, renameNode, "name", AliasCommandLevel.ALL);
    }

    public enum AliasCommandLevel {
        NONE,
        SHORT_FORMS,
        ALL
    }
}