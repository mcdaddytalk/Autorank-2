package me.armar.plugins.autorank.pathbuilder.result;

import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.util.AutorankTools;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandResult extends AbstractResult {

    private List<String> commands = null;
    private Server server = null;

    @Override
    public boolean applyResult(final Player player) {
        if (server != null) {

            // Run all commands using the console
            for (final String command : commands) {
                final String cmd = command.replace("&p", player.getName());
                Bukkit.getScheduler().callSyncMethod(this.getAutorank(),
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            }

        }
        return server != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see me.armar.plugins.autorank.pathbuilder.result.AbstractResult#getDescription()
     */
    @Override
    public String getDescription() {
        // Check if we have a custom description. If so, return that instead.
        if (this.hasCustomDescription()) {
            return this.getCustomDescription();
        }

        return Lang.COMMAND_RESULT.getConfigValue(AutorankTools.createStringFromList(commands));
    }

    @Override
    public boolean setOptions(final String[] commands) {
        this.server = this.getAutorank().getServer();
        final List<String> replace = new ArrayList<String>();
        for (final String command : commands) {
            replace.add(command.trim());
        }
        this.commands = replace;
        return true;
    }
}
