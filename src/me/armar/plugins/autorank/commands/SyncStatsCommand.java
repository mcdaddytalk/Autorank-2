package me.armar.plugins.autorank.commands;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.commands.manager.AutorankCommand;
import me.armar.plugins.autorank.permissions.AutorankPermission;
import me.armar.plugins.autorank.storage.TimeType;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.UUID;

/**
 * The command delegator for the '/ar syncstats' command.
 */
public class SyncStatsCommand extends AutorankCommand {

    private final Autorank plugin;

    public SyncStatsCommand(final Autorank instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {

        if (!this.hasPermission(AutorankPermission.SYNC_STATS_DATA, sender))
            return true;

        int count = 0;

        // Sync playtime of every player
        for (final UUID uuid : plugin.getPlayTimeStorageManager().getPrimaryStorageProvider().getStoredPlayers(TimeType
                .TOTAL_TIME)) {

            final OfflinePlayer p = plugin.getServer().getOfflinePlayer(uuid);

            // Time is stored in minutes
            final int statsPlayTime = plugin.getStatisticsManager().getTimePlayed(uuid, null);

            if (statsPlayTime <= 0) {
                continue;
            }

            // Update time
            plugin.getPlayTimeStorageManager().setPlayerTime(TimeType.TOTAL_TIME, uuid, statsPlayTime);

            // Increment count
            count++;
        }

        if (count == 0) {
            sender.sendMessage(ChatColor.GREEN + "Could not sync stats. Run command again!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Time has succesfully been updated for all entries.");
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Sync Autorank's time to Stats' time.";
    }

    @Override
    public String getPermission() {
        return AutorankPermission.SYNC_STATS_DATA;
    }

    @Override
    public String getUsage() {
        return "/ar syncstats";
    }
}
