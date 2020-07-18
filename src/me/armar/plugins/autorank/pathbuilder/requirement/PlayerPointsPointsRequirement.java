package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.PlayerPointsHook;

import java.util.UUID;

public class PlayerPointsPointsRequirement extends AbstractRequirement {

    private PlayerPointsHook handler = null;
    private int requiredPoints = -1;

    @Override
    public String getDescription() {
        return Lang.PLAYERPOINTS_POINTS_REQUIREMENT.getConfigValue(requiredPoints);
    }

    @Override
    public String getProgressString(UUID uuid) {
        return handler.getPlayerPoints(uuid) + "/" + requiredPoints;
    }

    @Override
    protected boolean meetsRequirement(UUID uuid) {

        if (!handler.isHooked())
            return false;

        return handler.getPlayerPoints(uuid) >= requiredPoints;
    }

    @Override
    public boolean initRequirement(final String[] options) {

        // Add dependency
        addDependency(Library.PLAYERPOINTS);

        handler = (PlayerPointsHook) this.getDependencyManager().getLibraryHook(Library.PLAYERPOINTS).orElse(null);

        if (options.length > 0) {
            try {
                requiredPoints = Integer.parseInt(options[0]);
            } catch (NumberFormatException e) {
                this.registerWarningMessage("An invalid number is provided");
                return false;
            }
        }

        if (requiredPoints < 0) {
            this.registerWarningMessage("No number is provided or smaller than 0.");
            return false;
        }

        return handler != null;
    }

    @Override
    public double getProgressPercentage(UUID uuid) {
        return handler.getPlayerPoints(uuid) * 1.0d / requiredPoints;
    }
}
