package me.armar.plugins.autorank.permissions.handlers;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.permissions.PermissionsHandler;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.LibraryHook;
import me.staartvin.utils.pluginlibrary.hooks.VaultHook;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author Staartvin & DeathStampler (see replaceGroup())
 * <p>
 * VaultPermissionsHandler tackles all work that has to be done with
 * Vault. (Most of the permissions plugins are supported with Vault)
 */
public class VaultPermissionsHandler extends PermissionsHandler {

    public VaultPermissionsHandler(final Autorank plugin) {
        super(plugin);
    }

    /**
     * Add a player to group
     *
     * @param player Player to add
     * @param world  On a specific world
     * @param group  Group to add the player to
     * @return true if done, false if failed
     */
    public boolean addGroup(final Player player, final String world, final String group) {
        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return false;

        if (VaultHook.getPermissions() == null) {
            return false;
        }

        return VaultHook.getPermissions().playerAddGroup(world, player, group);
    }


    public boolean demotePlayer(final Player player, String world, final String groupFrom, final String groupTo) {

        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return false;

        if (VaultHook.getPermissions() == null) {
            return false;
        }

        // Temporary fix for bPermissions
        if (world == null && VaultHook.getPermissions().getName().toLowerCase().contains("bpermissions")) {
            world = player.getWorld().getName();
        }

        // Let get the player groups before we change them.
        final Collection<String> groupsBeforeAdd = getPlayerGroups(player);

        // Output array for debug
        for (final String group : groupsBeforeAdd) {
            getPlugin().debugMessage("Group of " + player.getName() + " before removing: " + group);
        }

        Collection<String> groupsAfterAdd;

        final boolean worked1 = removeGroup(player, world, groupFrom);

        boolean worked2 = false;

        if (worked1) {
            // There should be a difference between the two.
            groupsAfterAdd = getPlayerGroups(player);

            // Output array for debug
            for (final String group : groupsAfterAdd) {
                getPlugin().debugMessage("Group of " + player.getName() + " after removing: " + group);
            }

            worked2 = addGroup(player, world, groupTo);
        }

        return worked1 && worked2;
    }

    /**
     * Get all known groups
     *
     * @return an array of strings containing all setup groups of the
     * permissions plugin.
     */
    @Override
    public Collection<String> getGroups() {
        List<String> groups = new ArrayList<>();

        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return Collections.unmodifiableCollection(groups);

        if (VaultHook.getPermissions() == null) {
            return Collections.unmodifiableCollection(groups);
        }

        groups.addAll(Arrays.asList(VaultHook.getPermissions().getGroups()));

        return Collections.unmodifiableCollection(groups);
    }

    /*
     * (non-Javadoc)
     *
     * @see me.armar.plugins.autorank.permissions.PermissionsHandler#getName()
     */
    @Override
    public String getName() {
        return VaultHook.getPermissions().getName();
    }

    @Override
    public Collection<String> getPlayerGroups(final Player player) {
        List<String> groups = new ArrayList<>();

        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return Collections.unmodifiableCollection(groups);

        if (VaultHook.getPermissions() == null) {
            return Collections.unmodifiableCollection(groups);
        }

        // Let admin choose.
        if (getPlugin().getSettingsConfig().onlyUsePrimaryGroupVault()) {
            groups.add(VaultHook.getPermissions().getPrimaryGroup(player));
        } else {
            groups.addAll(Arrays.asList(VaultHook.getPermissions().getPlayerGroups(player)));
        }

        return Collections.unmodifiableCollection(groups);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Collection<String> getWorldGroups(final Player player, final String world) {
        List<String> groups = new ArrayList<>();

        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return Collections.unmodifiableCollection(groups);

        if (VaultHook.getPermissions() == null) {
            return Collections.unmodifiableCollection(groups);
        }

        groups.addAll(Arrays.asList(VaultHook.getPermissions().getPlayerGroups(world, player.getName())));

        return Collections.unmodifiableCollection(groups);
    }

    /**
     * Remove a player from a group
     *
     * @param player Player to remove
     * @param world  On a specific world
     * @param group  Group to remove the player from
     * @return true if done, false if failed
     */
    public boolean removeGroup(final Player player, final String world, final String group) {

        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return false;

        if (VaultHook.getPermissions() == null) {
            return false;
        }

        return VaultHook.getPermissions().playerRemoveGroup(world, player, group);
        // return permission.playerRemoveGroup(world, player.getName(), group);
    }

    @Override
    public boolean replaceGroup(final Player player, String world, final String oldGroup, final String newGroup) {

        LibraryHook hook = getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).orElse(null);

        if (hook == null || !hook.isHooked())
            return false;

        if (VaultHook.getPermissions() == null) {
            return false;
        }

        // Temporary fix for bPermissions
        if (world == null && VaultHook.getPermissions().getName().toLowerCase().contains("bpermissions")) {
            world = player.getWorld().getName();
        }

        // Let get the player groups before we change them.
        final Collection<String> groupsBeforeAdd = getPlayerGroups(player);

        // Output array for debug
        for (final String group : groupsBeforeAdd) {
            getPlugin().debugMessage("Group of " + player.getName() + " before adding: " + group);
        }

        Collection<String> groupsAfterAdd;

        final boolean worked1 = addGroup(player, world, newGroup);

        boolean worked2 = false;

        if (worked1) {
            // There should be a difference between the two.
            groupsAfterAdd = getPlayerGroups(player);

            // Output array for debug
            for (final String group : groupsAfterAdd) {
                getPlugin().debugMessage("Group of " + player.getName() + " after adding: " + group);
            }

            // When using PEX, if a player is in a default group this is not
            // really listed as the player being in the group.
            // It's just used as an alias. When we would change the rank, the
            // player would lose all other default groups.
            // We check if the player is in a default group and then re-add the
            // other groups after we added the new group the player was ranked
            // up to.
            // Thanks to @DeathStampler for this code and info.
            if (VaultHook.getPermissions().getName().toLowerCase().contains("permissionsex")) {
                // Normally the player should have one more group at this point.
                if (groupsAfterAdd.size() >= (groupsBeforeAdd.size() + 1)) {
                    // We have one more groups than before. Great. Let's remove
                    // oldGroup.
                    worked2 = removeGroup(player, world, oldGroup);

                    // Otherwise, let's see if we have just one group. This is
                    // an indication that the
                    // PermissionsEX player had more than one default group set.
                    // Those are now gone
                    // and we are left with just the newGroup.
                } else if (groupsAfterAdd.size() == 1) {
                    // We have just one group. Let's add any that are missing.
                    for (final String group : groupsBeforeAdd) {
                        // Let's not re-add the oldGroup
                        if (!group.equalsIgnoreCase(oldGroup)) {
                            // Should we check it if succeeds?
                            addGroup(player, world, group);
                        }
                    }
                    worked2 = true;
                } else {
                    // Not sure what situation would lead us here, so we'll just
                    // assume everything is good.
                    worked2 = true;
                }
            } else {
                worked2 = removeGroup(player, world, oldGroup);
            }
        }

        return worked1 && worked2;
    }

    @Override
    public boolean setupPermissionsHandler() {
        return getPlugin().getDependencyManager().getLibraryHook(Library.VAULT).map(LibraryHook::isHooked).orElse(false);
    }
}
