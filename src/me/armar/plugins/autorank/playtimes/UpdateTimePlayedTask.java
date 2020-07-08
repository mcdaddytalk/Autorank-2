package me.armar.plugins.autorank.playtimes;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.permissions.AutorankPermission;
import me.armar.plugins.autorank.storage.TimeType;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * This class is responsible for updating a player's play time every x minutes.
 */
public class UpdateTimePlayedTask implements Runnable {

    private final Autorank plugin;

    // UUID of player to keep track of.
    private final UUID uuid;

    public UpdateTimePlayedTask(Autorank instance, UUID uuid) {
        this.plugin = instance;
        this.uuid = uuid;
    }

    @Override
    public void run() {

        plugin.debugMessage("Run task to update play time of " + uuid);

        Player player = plugin.getServer().getPlayer(uuid);

        // Cancel task as player is not online anymore.
        if (player == null || !player.isOnline()) {
            plugin.debugMessage("Cancelling update play time of " + uuid + " as he's not online.");

            return;
        }

        // Set when we last updated this player's time.
        plugin.getTaskManager().setLastPlayTimeUpdate(uuid, System.currentTimeMillis());

        // Provide debug message so we know what's going on
        plugin.debugMessage("Updating play time of " + player.getName());

        // Do checks for offline exemptions
        plugin.getPlayerChecker().doOfflineExemptionChecks(player);

        // Don't add extra time to the player when he is excluded from time updates.
        if (player.hasPermission(AutorankPermission.EXCLUDE_FROM_TIME_UPDATES)) {
            plugin.debugMessage("Player " + player.getName() + " is excluded from time updates by given permissions.");
            return;
        }

        // Check to see if player is afk
        if (plugin.getDependencyManager().isAFK(player)) {
            plugin.debugMessage("Player " + player.getName() + " is AFK and so we don't add time.");
            return;
        }

        // Add time to a player's current time for all storage providers.
        for (final TimeType type : TimeType.values()) {
            plugin.getPlayTimeStorageManager().addPlayerTime(type, uuid, PlayTimeManager.INTERVAL_MINUTES);
        }

        // Auto assign path (if possible)
        plugin.getPathManager().autoAssignPaths(player.getUniqueId());

        // Only check a player if it is not disabled in the Settings.yml
        if (!plugin.getSettingsConfig().isAutomaticPathDisabled()) {
            // Check if player meets requirements
            plugin.getPlayerChecker().checkPlayer(player.getUniqueId());
        }
    }
}
