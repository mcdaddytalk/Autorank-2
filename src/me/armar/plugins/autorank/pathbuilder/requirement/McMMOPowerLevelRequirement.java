package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.McMMOHook;
import org.bukkit.entity.Player;

public class McMMOPowerLevelRequirement extends AbstractRequirement {

    private McMMOHook handler = null;
    int powerLevel = -1;

    @Override
    public String getDescription() {
        return Lang.MCMMO_POWER_LEVEL_REQUIREMENT.getConfigValue(powerLevel + "");
    }

    @Override
    public String getProgressString(final Player player) {
        final int level = handler.getPowerLevel(player);

        return level + "/" + powerLevel;
    }

    @Override
    public boolean meetsRequirement(final Player player) {

        if (!handler.isHooked())
            return false;

        final int level = handler.getPowerLevel(player);

        return level >= powerLevel;
    }

    @Override
    public boolean initRequirement(final String[] options) {

        // Add dependency
        addDependency(Library.MCMMO);

        handler = (McMMOHook) this.getDependencyManager().getLibraryHook(Library.MCMMO).orElse(null);

        if (options.length > 0) {
            powerLevel = Integer.parseInt(options[0]);
        }

        if (powerLevel < 0) {
            this.registerWarningMessage("No number is provided or smaller than 0.");
            return false;
        }

        if (handler == null || !handler.isHooked()) {
            this.registerWarningMessage("mcMMO is not available");
            return false;
        }

        return handler != null;
    }

    @Override
    public boolean needsOnlinePlayer() {
        return true;
    }
}
