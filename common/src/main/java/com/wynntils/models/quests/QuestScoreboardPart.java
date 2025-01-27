/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.quests;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.CodedString;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.type.SegmentMatcher;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.wynn.WynnUtils;
import java.util.List;
import net.minecraft.ChatFormatting;

public class QuestScoreboardPart extends ScoreboardPart {
    private static final SegmentMatcher QUEST_MATCHER = SegmentMatcher.fromPattern("Tracked Quest:");

    @Override
    public SegmentMatcher getSegmentMatcher() {
        return QUEST_MATCHER;
    }

    @Override
    public void onSegmentChange(ScoreboardSegment newValue) {
        List<CodedString> content = newValue.getContent();

        if (content.isEmpty()) {
            WynntilsMod.error("QuestHandler: content was empty.");
        }

        StringBuilder questName = new StringBuilder();
        StringBuilder nextTask = new StringBuilder();

        for (CodedString line : content) {
            if (line.startsWith("§e")) {
                questName.append(ComponentUtils.stripFormatting(line)).append(" ");
            } else {
                nextTask.append(line.getInternalCodedStringRepresentation()
                                .replaceAll(ChatFormatting.WHITE.toString(), ChatFormatting.AQUA.toString())
                                .replaceAll(ChatFormatting.GRAY.toString(), ChatFormatting.RESET.toString()))
                        .append(" ");
            }
        }

        String fixedName = WynnUtils.normalizeBadString(questName.toString().trim());
        CodedString fixedNextTask =
                CodedString.fromString(nextTask.toString().trim()).getNormalized();
        Models.Quest.updateTrackedQuestFromScoreboard(fixedName, fixedNextTask);
    }

    @Override
    public void onSegmentRemove(ScoreboardSegment segment) {
        Models.Quest.clearTrackedQuestFromScoreBoard();
    }

    @Override
    public void reset() {
        Models.Quest.clearTrackedQuestFromScoreBoard();
    }

    @Override
    public String toString() {
        return "QuestScoreboardPart{}";
    }
}
