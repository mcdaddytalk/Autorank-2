package me.armar.plugins.autorank.leaderboard;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.storage.PlayTimeStorageProvider;
import me.armar.plugins.autorank.storage.TimeType;
import me.armar.plugins.autorank.util.AutorankTools;
import me.armar.plugins.autorank.util.uuid.UUIDManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This class is used to handle all leaderboard things. <br>
 * When a player calls /ar leaderboard, it will show the currently cached
 * leaderboard. <br>
 * <i>/ar leaderboard force</i> can be used to forcefully update the current
 * leaderboard. <br>
 * <i>/ar leaderboard broadcast</i> can be used to broadcast the leaderboard
 * over the entire server.
 * <p>
 * Date created: 21:03:23 15 mrt. 2014
 *
 * @author Staartvin
 */
public class LeaderboardHandler {

    private static final double LEADERBOARD_TIME_VALID = 30; // Leaderboard is valid for 30 minutes
    private final Autorank plugin;
    private String layout = "&6&r | &b&p - &7&d %day%, &h %hour% and &m %minute%.";
    private int leaderboardLength = 10;

    public LeaderboardHandler(final Autorank plugin) {
        this.plugin = plugin;

        leaderboardLength = plugin.getSettingsConfig().getLeaderboardLength();
        layout = plugin.getSettingsConfig().getLeaderboardLayout();
    }

    /**
     * Sort a map by its values.
     *
     * @param map Map to sort.
     * @param <K> KeyType
     * @param <V> ValueType
     * @return a sorted map.
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Broadcast a leaderboard to all online players.
     *
     * @param type Type of leaderboard
     */
    public void broadcastLeaderboard(final TimeType type) {
        if (shouldUpdateLeaderboard(type)) {
            // Update leaderboard because it is not valid anymore.
            // Run async because it uses UUID lookup
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.debugMessage("Updating leaderboard because it's outdated");
                    updateLeaderboard(type);

                    // Send them afterwards, not at the same time.
                    for (final String msg : plugin.getInternalPropertiesConfig().getCachedLeaderboard(type)) {
                        plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                }
            });
        } else {
            // send them instantly
            for (final String msg : plugin.getInternalPropertiesConfig().getCachedLeaderboard(type)) {
                plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }
    }

    /**
     * Get a hashmap, the key is the UUID of a player and the value is time that
     * player has played. <br>
     * This map is sorted on player time.
     *
     * @param type TimeType to get the sort for.
     * @return a sorted map.
     */
    private Map<UUID, Integer> getSortedTimesByUUID(final TimeType type) {

        PlayTimeStorageProvider primaryStorageProvider = plugin.getPlayTimeStorageManager().getPrimaryStorageProvider();

        final List<UUID> uuids = primaryStorageProvider.getStoredPlayers(type);

        final HashMap<UUID, Integer> times = new HashMap<UUID, Integer>();

        int size = uuids.size();

        int lastSentPercentage = 0;

        // Fill unsorted lists
        for (int i = 0; i < uuids.size(); i++) {

            UUID uuid = uuids.get(i);

            // If UUID is null, we can't do anything with it.
            if (uuid == null) {
                continue;
            }

            // If player is exempted
            if (plugin.getPlayerChecker().isExemptedFromLeaderboard(uuid)) {
                continue;
            }

            DecimalFormat df = new DecimalFormat("#.#");
            double percentage = ((i * 1.0) / size) * 100;
            int floored = (int) Math.floor(percentage);

            if (lastSentPercentage != floored && floored % 10 == 0) {
                lastSentPercentage = floored;
                plugin.debugMessage("Autorank leaderboard update is at " + df.format(percentage) + "%.");
            }

            // Use cache on .getTimeOfPlayer() so that we don't refresh all
            // uuids in existence.
            if (type == TimeType.TOTAL_TIME) {

                if (plugin.getSettingsConfig().useGlobalTimeInLeaderboard() && plugin.getPlayTimeStorageManager()
                        .isStorageTypeActive(PlayTimeStorageProvider.StorageType.DATABASE)) {

                    try {
                        times.put(uuid, plugin.getPlayTimeManager().getGlobalPlayTime(type, uuid).get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {

                    try {
                        times.put(uuid, primaryStorageProvider.getPlayerTime(type, uuid).get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    times.put(uuid, primaryStorageProvider.getPlayerTime(type, uuid).get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sort all values

        return LeaderboardHandler.sortByValue(times);
    }

    private Map<String, Integer> getSortedTimesByNames(final TimeType type) {

        PlayTimeStorageProvider primaryStorageProvider = plugin.getPlayTimeStorageManager().getPrimaryStorageProvider();

        final List<String> playerNames = plugin.getUUIDStorage().getStoredPlayerNames();

        final Map<String, Integer> times = new HashMap<String, Integer>();

        int size = playerNames.size();

        int lastSentPercentage = 0;

        // Fill unsorted lists
        for (int i = 0; i < playerNames.size(); i++) {

            String playerName = playerNames.get(i);

            if (playerName == null) {
                continue;
            }

            UUID uuid = null;
            try {
                uuid = UUIDManager.getUUID(playerName).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            if (uuid == null) {
                continue;
            }

            // If player is exempted
            if (plugin.getPlayerChecker().isExemptedFromLeaderboard(uuid)) {
                continue;
            }

            DecimalFormat df = new DecimalFormat("#.#");
            double percentage = ((i * 1.0) / size) * 100;
            int floored = (int) Math.floor(percentage);

            if (lastSentPercentage != floored) {
                lastSentPercentage = floored;
                plugin.debugMessage("Autorank leaderboard update is at " + df.format(percentage) + "%.");
            }

            // Use cache on .getTimeOfPlayer() so that we don't refresh all
            // uuids in existence.
            if (type == TimeType.TOTAL_TIME) {

                if (plugin.getSettingsConfig().useGlobalTimeInLeaderboard() && plugin.getPlayTimeStorageManager()
                        .isStorageTypeActive(PlayTimeStorageProvider.StorageType.DATABASE)) {

                    try {
                        times.put(playerName, plugin.getPlayTimeManager().getGlobalPlayTime(type, uuid).get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        times.put(playerName, primaryStorageProvider.getPlayerTime(type, uuid).get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    times.put(playerName, primaryStorageProvider.getPlayerTime(type, uuid).get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sort all values
        // final Map<String, Integer> sortedMap = sortByComparatorString(times,
        // false);

        return LeaderboardHandler.sortByValue(times);
    }

    /**
     * Send the leaderboard to a {@linkplain CommandSender}.
     *
     * @param sender Sender to send it to.
     * @param type   Type of leaderboard to send.
     */
    public void sendLeaderboard(final CommandSender sender, final TimeType type) {
        if (shouldUpdateLeaderboard(type)) {
            // Update leaderboard because it is not valid anymore.
            // Run async because it uses UUID lookup
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.debugMessage("Updating leaderboard because it's outdated");
                    updateLeaderboard(type);

                    // Send them afterwards, not at the same time.
                    sendMessages(sender, type);
                }
            });
        } else {
            // send them instantly
            sendMessages(sender, type);
        }
    }

    /**
     * Send the given message to a {@linkplain CommandSender}.
     *
     * @param sender Sender to send message to.
     * @param type   Type of leaderboard to send
     */
    public void sendMessages(final CommandSender sender, final TimeType type) {
        for (final String msg : plugin.getInternalPropertiesConfig().getCachedLeaderboard(type)) {
            AutorankTools.sendColoredMessage(sender, msg);
        }
    }

    /**
     * Check whether we should update a leaderboard.
     *
     * @param type Type of leaderboard check
     * @return true if we should update the leaderboard
     */
    private boolean shouldUpdateLeaderboard(TimeType type) {
        if (System.currentTimeMillis() - plugin.getInternalPropertiesConfig().getLeaderboardLastUpdateTime(type) >
                (60000 * LEADERBOARD_TIME_VALID)) {
            return true;
        } else return plugin.getInternalPropertiesConfig().getCachedLeaderboard(type).size() <= 2;
    }

    /**
     * Update all leaderboards
     */
    public void updateAllLeaderboards() {

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.debugMessage("Updating all leaderboards forcefully");
                for (final TimeType type : TimeType.values()) {
                    if (!shouldUpdateLeaderboard(type))
                        continue;

                    updateLeaderboard(type);
                }
            }
        });
    }

    /**
     * Forcefully update a leaderboard (ignoring cached versions).
     *
     * @param type Type of leaderboard to update.
     */
    public void updateLeaderboard(final TimeType type) {
        plugin.debugMessage(ChatColor.BLUE + "Updating leaderboard '" + type.toString() + "'!");

        // Store messages to make leaderboard
        final List<String> stringList = new ArrayList<String>();

        if (type == TimeType.TOTAL_TIME) {
            stringList.add(Lang.LEADERBOARD_HEADER_ALL_TIME.getConfigValue());
        } else if (type == TimeType.DAILY_TIME) {
            stringList.add(Lang.LEADERBOARD_HEADER_DAILY.getConfigValue());
        } else if (type == TimeType.WEEKLY_TIME) {
            stringList.add(Lang.LEADERBOARD_HEADER_WEEKLY.getConfigValue());
        } else if (type == TimeType.MONTHLY_TIME) {
            stringList.add(Lang.LEADERBOARD_HEADER_MONTHLY.getConfigValue());
        }

        // Only store the users that should appear on the leaderboard, along with their time.
        AutorankLeaderboard finalLeaderboard = null;
        try {
            finalLeaderboard = getSortedLeaderboard(type).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // The leaderboard couldn't get created for some reason.
        if (finalLeaderboard == null) {
            return;
        }

        Iterator<Entry<String, Integer>> iterator = finalLeaderboard.getLeaderboard().entrySet().iterator();

        for (int i = 0; i < leaderboardLength && iterator.hasNext(); i++) {

            final Entry<String, Integer> entry = iterator.next();

            int time = entry.getValue();

            String message = layout.replace("&p", entry.getKey());

            // divided by 1440
            final int days = (time / 1440);

            // (time - days) / 60
            final int hours = (time - (days * 1440)) / 60;

            // (time - days - hours)
            final int minutes = time - (days * 1440) - (hours * 60);

            message = message.replace("&r", Integer.toString(i + 1));
            message = message.replace("&tm", Integer.toString(time));
            message = message.replace("&th", Integer.toString(time / 60));
            message = message.replace("&d", Integer.toString(days));
            time = time - ((time / 1440) * 1440);
            message = message.replace("&h", Integer.toString(hours));
            time = time - ((time / 60) * 60);

            message = message.replace("&m", Integer.toString(minutes));
            message = ChatColor.translateAlternateColorCodes('&', message);

            // Correctly show plural or singular format.
            if (days > 1 || days == 0) {
                message = message.replace("%day%", Lang.DAY_PLURAL.getConfigValue());
            } else {
                message = message.replace("%day%", Lang.DAY_SINGULAR.getConfigValue());
            }

            if (hours > 1 || hours == 0) {
                message = message.replace("%hour%", Lang.HOUR_PLURAL.getConfigValue());
            } else {
                message = message.replace("%hour%", Lang.HOUR_SINGULAR.getConfigValue());
            }

            if (minutes > 1 || minutes == 0) {
                message = message.replace("%minute%", Lang.MINUTE_PLURAL.getConfigValue());
            } else {
                message = message.replace("%minute%", Lang.MINUTE_SINGULAR.getConfigValue());
            }

            stringList.add(message);
        }

        stringList.add(Lang.LEADERBOARD_FOOTER.getConfigValue());

        // Cache this leaderboard
        plugin.getInternalPropertiesConfig().setCachedLeaderboard(type, stringList);

        // Update latest update-time
        plugin.getInternalPropertiesConfig().setLeaderboardLastUpdateTime(type, System.currentTimeMillis());
    }

    /**
     * Get the sorted leaderboard of play time for a specific time type. Note that the leaderboard is limited to
     * {@link LeaderboardHandler#leaderboardLength} entries.
     *
     * @param type Type of time to get the leaderboard for.
     * @return sorted leaderboard.
     */
    private CompletableFuture<AutorankLeaderboard> getSortedLeaderboard(TimeType type) {
        return CompletableFuture.supplyAsync(() -> {

            AutorankLeaderboard finalLeaderboard = new AutorankLeaderboard(type);

            // We can ask all UUIDs in the uuids file and sort the playtime
            // After we sorted the playtime, we collect the playernames of the top x (leaderboard length variable).
            final Map<UUID, Integer> sortedPlaytimes = getSortedTimesByUUID(type);

            Iterator<Entry<UUID, Integer>> itr = sortedPlaytimes.entrySet().iterator();

            plugin.debugMessage("Size leaderboard: " + sortedPlaytimes.size());

            for (int i = 0; i < leaderboardLength && itr.hasNext(); i++) {
                final Entry<UUID, Integer> entry = itr.next();

                final UUID uuid = entry.getKey();

                // Grab playername from here so it doesn't load all player names
                // ever.
                // Get the cached value of this uuid to improve performance
                String name = null;
                try {
                    name = UUIDManager.getPlayerName(uuid).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                // No cached name found, don't use this name.
                if (name == null)
                    continue;

                finalLeaderboard.add(name, entry.getValue());
            }


            // Sort the leaderboard before returning it.
            finalLeaderboard.sortLeaderboard();

            return finalLeaderboard;
        });
    }

}
