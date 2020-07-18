package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.QuestsHook;

import java.util.UUID;

public class QuestsQuestPointsRequirement extends AbstractRequirement {

    private QuestsHook handler = null;
    private int questPoints = -1;

    @Override
    public String getDescription() {
        return Lang.QUESTS_QUEST_POINTS_REQUIREMENT.getConfigValue(questPoints);
    }

    @Override
    public String getProgressString(UUID uuid) {
        return handler.getQuestsPoints(uuid) + "/" + questPoints;
    }

    @Override
    protected boolean meetsRequirement(UUID uuid) {

        if (!handler.isHooked())
            return false;

        return handler.getQuestsPoints(uuid) >= questPoints;
    }

    @Override
    public boolean initRequirement(final String[] options) {

        // Add dependency
        addDependency(Library.QUESTS);

        handler = (QuestsHook) this.getDependencyManager().getLibraryHook(Library.QUESTS).orElse(null);

        if (options.length > 0) {
            try {
                questPoints = Integer.parseInt(options[0]);
            } catch (NumberFormatException e) {
                this.registerWarningMessage("An invalid number is provided");
                return false;
            }
        }

        if (questPoints < 0) {
            this.registerWarningMessage("No number is provided or smaller than 0.");
            return false;
        }

        return handler != null;
    }

    @Override
    public double getProgressPercentage(UUID uuid) {
        return handler.getQuestsPoints(uuid) * 1.0d / questPoints;
    }
}
