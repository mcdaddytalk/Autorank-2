package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.JobsHook;
import org.bukkit.entity.Player;

public class JobsExperienceRequirement extends AbstractRequirement {

    int experience = -1;
    String jobName;
    private JobsHook jobsHandler;

    @Override
    public String getDescription() {

        String lang = Lang.JOBS_EXPERIENCE_REQUIREMENT.getConfigValue(experience, jobName);

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            lang = lang.concat(" (in world '" + this.getWorld() + "')");
        }

        return lang;
    }

    @Override
    public String getProgressString(final Player player) {

        double points = 0;

        if (jobsHandler != null && jobsHandler.isHooked()) {
            points = jobsHandler.getCurrentXP(player, jobName);
        }

        return points + "/" + experience;
    }

    @Override
    public boolean meetsRequirement(final Player player) {

        double points = -1;

        if (jobsHandler == null || !jobsHandler.isHooked()) {
            points = -1;
        } else {
            points = jobsHandler.getCurrentXP(player, jobName);
        }

        return points >= experience;
    }

    @Override
    public boolean initRequirement(final String[] options) {

        // Add dependency
        addDependency(Library.JOBS);

        jobsHandler = (JobsHook) this.getAutorank().getDependencyManager().getLibraryHook(Library.JOBS).orElse(null);

        try {
            experience = Integer.parseInt(options[0]);
        } catch (NumberFormatException e) {
            this.registerWarningMessage("An invalid number is provided");
            return false;
        }

        if (options.length > 1) {
            jobName = options[1];
        }

        if (experience < 0) {
            this.registerWarningMessage("No experience level is provided or smaller than 0.");
            return false;
        }

        if (jobsHandler == null || !jobsHandler.isHooked()) {
            this.registerWarningMessage("Jobs is not available");
            return false;
        }

        if (jobName == null) {
            this.registerWarningMessage("No job name is provided");
            return false;
        }

        return true;
    }

    @Override
    public boolean needsOnlinePlayer() {
        return true;
    }
}
