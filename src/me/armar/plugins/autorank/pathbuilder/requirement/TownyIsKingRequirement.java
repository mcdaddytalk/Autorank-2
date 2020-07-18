package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.TownyAdvancedHook;
import org.bukkit.entity.Player;

public class TownyIsKingRequirement extends AbstractRequirement {

    boolean shouldBeKing = true;
    private TownyAdvancedHook hook;

    @Override
    public String getDescription() {

        String lang = Lang.TOWNY_IS_KING_REQUIREMENT.getConfigValue();

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            lang = lang.concat(" (in world '" + this.getWorld() + "')");
        }

        return lang;
    }

    @Override
    public String getProgressString(final Player player) {
        return hook.isKing(player.getName()) + "/" + shouldBeKing;
    }

    @Override
    public boolean meetsRequirement(final Player player) {

        if (!hook.isHooked())
            return false;

        return hook.isKing(player.getName()) == shouldBeKing;
    }

    @Override
    public boolean initRequirement(final String[] options) {

        // Add dependency
        addDependency(Library.TOWNY_ADVANCED);

        this.hook =
                (TownyAdvancedHook) this.getAutorank().getDependencyManager().getLibraryHook(Library.TOWNY_ADVANCED).orElse(null);

        try {
            shouldBeKing = Boolean.parseBoolean(options[0]);
        } catch (NumberFormatException e) {
            this.registerWarningMessage("An invalid boolean was provided.");
            return false;
        }

        if (hook == null || !hook.isHooked()) {
            this.registerWarningMessage("Towny is not available");
            return false;
        }

        return true;
    }

    @Override
    public boolean needsOnlinePlayer() {
        return true;
    }
}
