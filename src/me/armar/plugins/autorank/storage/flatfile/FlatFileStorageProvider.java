package me.armar.plugins.autorank.storage.flatfile;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.backup.BackupManager;
import me.armar.plugins.autorank.config.SimpleYamlConfiguration;
import me.armar.plugins.autorank.storage.PlayTimeStorageProvider;
import me.armar.plugins.autorank.storage.TimeType;
import me.armar.plugins.autorank.util.AutorankTools;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlatFileStorageProvider extends PlayTimeStorageProvider {

    private final String pathTotalTimeFile = "/data/Total_time.yml";
    private final String pathDailyTimeFile = "/data/Daily_time.yml";
    private final String pathWeeklyTimeFile = "/data/Weekly_time.yml";
    private final String pathMonthlyTimeFile = "/data/Monthly_time.yml";
    // Store where the file for each time type is saved.
    private final Map<TimeType, String> dataTypePaths = new HashMap<>();
    // Store for each time type a file to store data about players.
    private final Map<TimeType, SimpleYamlConfiguration> dataFiles = new HashMap<>();

    private boolean isLoaded = false;

    public FlatFileStorageProvider(Autorank instance) {
        super(instance);
    }

    @Override
    public void setPlayerTime(TimeType timeType, UUID uuid, int time) {
        // Set time of a player of a specific type

        plugin.debugMessage("Setting time of " + uuid.toString() + " to " + time + " (" + timeType.name() + ").");

        // Setting time of a player
        plugin.getLoggerManager().logMessage("Setting (Flatfile) " + timeType.name() + " of " + uuid.toString() + " " +
                "to: " + time);

        final SimpleYamlConfiguration data = this.getDataFile(timeType);

        data.set(uuid.toString(), time);
    }

    @Override
    public CompletableFuture<Integer> getPlayerTime(TimeType timeType, UUID uuid) {
        // Get time of a player with specific type
        final SimpleYamlConfiguration data = this.getDataFile(timeType);

        return CompletableFuture.completedFuture(data.getInt(uuid.toString(), 0));
    }

    @Override
    public void resetData(TimeType timeType) {
        final SimpleYamlConfiguration data = this.getDataFile(timeType);

        plugin.debugMessage("Resetting storage file '" + timeType + "'!");

        // Delete file
        final boolean deleted = data.getInternalFile().delete();

        // Don't create a new file if it wasn't deleted in the first place.
        if (!deleted) {
            plugin.debugMessage("Tried deleting storage file, but could not delete!");
            return;
        }

        // Create a new file so it's empty
        if (timeType == TimeType.DAILY_TIME) {
            plugin.getLoggerManager().logMessage("Resetting daily time file");

            try {
                dataFiles.put(TimeType.DAILY_TIME,
                        new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.DAILY_TIME), "Daily storage"));
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else if (timeType == TimeType.WEEKLY_TIME) {
            plugin.getLoggerManager().logMessage("Resetting weekly time file");

            try {
                dataFiles.put(TimeType.WEEKLY_TIME,
                        new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.WEEKLY_TIME), "Weekly storage"));
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else if (timeType == TimeType.MONTHLY_TIME) {
            plugin.getLoggerManager().logMessage("Resetting monthly time file");

            try {
                dataFiles.put(TimeType.MONTHLY_TIME,
                        new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.MONTHLY_TIME), "Monthly " +
                                "storage"));
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else if (timeType == TimeType.TOTAL_TIME) {
            plugin.getLoggerManager().logMessage("Resetting total time file");

            try {
                dataFiles.put(TimeType.TOTAL_TIME,
                        new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.TOTAL_TIME), "Total storage"));
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addPlayerTime(TimeType timeType, UUID uuid, int timeToAdd) {
        int time = 0;

        plugin.debugMessage("Adding " + timeToAdd + " to " + uuid.toString() + " (" + timeType.name() + ")");

        try {
            time = this.getPlayerTime(timeType, uuid).get();
            plugin.debugMessage("Player " + uuid.toString() + " already has " + time + " for (" + timeType.name() +
                    ")");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (time < 0) {
            time = 0;
        }

        plugin.debugMessage("New time of " + uuid.toString() + " will be " + (time + timeToAdd) + " (" + timeType.name() + ")");

        this.setPlayerTime(timeType, uuid, time + timeToAdd);
    }

    @Override
    public String getName() {
        return "FlatFileStorageProvider";
    }

    @Override
    public CompletableFuture<Boolean> initialiseProvider() {
        return CompletableFuture.supplyAsync(() -> {
            // Load data files
            FlatFileStorageProvider.this.loadDataFiles();

            // Register task for saving data files.
            FlatFileStorageProvider.this.registerTasks();

            isLoaded = true;

            return true;
        });
    }

    @Override
    public int purgeOldEntries(int threshold) {
        int entriesRemoved = 0;

        final SimpleYamlConfiguration data = this.getDataFile(TimeType.TOTAL_TIME);

        long currentTime = System.currentTimeMillis();

        for (final UUID uuid : getStoredPlayers(TimeType.TOTAL_TIME)) {
            // Get the player object that represents this UUID
            OfflinePlayer offPlayer = plugin.getServer().getOfflinePlayer(uuid);

            // Check if this player has ever logged in on the server
            if (offPlayer.getName() == null) {
                // Remove record
                data.set(uuid.toString(), null);
                entriesRemoved++;
                continue;
            }

            // Check when the player has last logged in.
            long lastPlayed = offPlayer.getLastPlayed();

            // Check if 'last played time' is over threshold time.
            if (lastPlayed <= 0 || (currentTime - lastPlayed) / 86400000 >= threshold) {
                // Remove record
                data.set(uuid.toString(), null);
                entriesRemoved++;
            }
        }

        return entriesRemoved;
    }

    @Override
    public CompletableFuture<Integer> getNumberOfStoredPlayers(TimeType timeType) {
        return CompletableFuture.completedFuture(getStoredPlayers(timeType).size());
    }

    @Override
    public List<UUID> getStoredPlayers(TimeType timeType) {
        final List<UUID> uuids = new ArrayList<UUID>();

        final SimpleYamlConfiguration data = this.getDataFile(timeType);

        // Loop over all entries in the file and retrieve their uuids.
        for (final String uuidString : data.getKeys(false)) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (final IllegalArgumentException e) {
                continue;
            }

            uuids.add(uuid);
        }

        return uuids;
    }

    @Override
    public void saveData() {
        for (final Map.Entry<TimeType, SimpleYamlConfiguration> entry : dataFiles.entrySet()) {
            entry.getValue().saveFile();
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FLAT_FILE;
    }

    @Override
    public boolean canImportData() {
        return true;
    }


    @Override
    public void importData() {
        final SimpleYamlConfiguration data = this.getDataFile(TimeType.TOTAL_TIME);
        data.reloadFile();
    }

    @Override
    public boolean canBackupData() {
        return true;
    }

    @Override
    public boolean backupData() {

        // Back up all files
        for (Map.Entry<TimeType, String> entry : dataTypePaths.entrySet()) {
            plugin.debugMessage("Making a backup of " + entry.getValue());
            plugin.getBackupManager().backupFile(entry.getValue(), plugin.getDataFolder().getAbsolutePath()
                    + File.separator + "backups" + File.separator + entry.getValue().replace("/data/", ""));
        }

        return true;
    }

    @Override
    public int clearBackupsBeforeDate(LocalDate date) {

        String backupsFolder = plugin.getDataFolder().getAbsolutePath()
                + File.separator + "backups";

        AtomicInteger deletedFiles = new AtomicInteger();

        try (Stream<Path> walk = Files.walk(Paths.get(backupsFolder))) {

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(Path::toString).collect(Collectors.toList());

            result.forEach(fileName -> {
                // Check what the date of the file is.
                String fileDateString = fileName.replaceAll("[^\\d]", "");

                Date fileDate = null;

                try {
                    fileDate = BackupManager.dateFormat.parse(fileDateString);
                } catch (ParseException e) {
                    // Ignore error.
                }

                if (fileDate == null) {
                    return;
                }

                if (!fileDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(date)) {
                    // This file is not from before the date, so skip it.
                    return;
                }

                // Now remove the file since it is applicable for removal.
                try {
                    Files.deleteIfExists(Paths.get(fileName));
                    deletedFiles.getAndIncrement();
                } catch (IOException ignored) {
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }


        return deletedFiles.get();
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Get a storage file for a specific time type.
     *
     * @param type Type of time
     * @return a storage file where the given time type is stored.
     */
    private SimpleYamlConfiguration getDataFile(final TimeType type) {
        return dataFiles.get(type);
    }

    /**
     * Load all the storage files (daily time, weekly time, etc.).
     */
    private void loadDataFiles() {

        // Register storage path for each time type
        dataTypePaths.put(TimeType.TOTAL_TIME, pathTotalTimeFile);
        dataTypePaths.put(TimeType.DAILY_TIME, pathDailyTimeFile);
        dataTypePaths.put(TimeType.WEEKLY_TIME, pathWeeklyTimeFile);
        dataTypePaths.put(TimeType.MONTHLY_TIME, pathMonthlyTimeFile);

        // Create new files for the time type.
        try {
            dataFiles.put(TimeType.TOTAL_TIME,
                    new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.TOTAL_TIME), "Total storage"));
            dataFiles.put(TimeType.DAILY_TIME,
                    new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.DAILY_TIME), "Daily storage"));
            dataFiles.put(TimeType.WEEKLY_TIME,
                    new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.WEEKLY_TIME), "Weekly storage"));
            dataFiles.put(TimeType.MONTHLY_TIME,
                    new SimpleYamlConfiguration(plugin, dataTypePaths.get(TimeType.MONTHLY_TIME), "Monthly storage"));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

    }


    /**
     * Register tasks for saving and updating time of players.
     */
    private void registerTasks() {
        // Run save task every 30 seconds
        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.debugMessage("Periodically saving all flatfile storage files.");
                saveData();
            }
        }, AutorankTools.TICKS_PER_SECOND, AutorankTools.TICKS_PER_MINUTE);
    }

    /**
     * Archive old records. Records below the minimum value will be removed
     * because they are 'inactive'.
     *
     * @param minimum Lowest threshold to check for
     * @return Number of records that were removed
     */
    private int archive(final int minimum) {
        // Keep a counter of archived items
        int counter = 0;

        final SimpleYamlConfiguration data = this.getDataFile(TimeType.TOTAL_TIME);

        for (final UUID uuid : getStoredPlayers(TimeType.TOTAL_TIME)) {

            int time = 0;
            try {
                time = this.getPlayerTime(TimeType.TOTAL_TIME, uuid).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            // Found a record to be archived
            if (time < minimum) {
                counter++;
                // Remove record
                data.set(uuid.toString(), null);
            }
        }

        saveData();
        return counter;
    }

}
