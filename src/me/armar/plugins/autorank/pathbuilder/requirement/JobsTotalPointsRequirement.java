package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.staartvin.utils.pluginlibrary.Library;
import me.staartvin.utils.pluginlibrary.hooks.JobsHook;

import java.util.UUID;

public class JobsTotalPointsRequirement extends AbstractRequirement {

    private JobsHook jobsHandler;
    int totalPoints = -1;

    @Override
    public String getDescription() {

        String lang = Lang.JOBS_TOTAL_POINTS_REQUIREMENT.getConfigValue(totalPoints);

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            lang = lang.concat(" (in world '" + this.getWorld() + "')");
        }

        return lang;
    }

    @Override
    public String getProgressString(UUID uuid) {

        double points = 0;

        if (jobsHandler != null && !jobsHandler.isHooked()) {
            points = jobsHandler.getTotalPoints(uuid);
        }

        return points + "/" + totalPoints;
    }

    @Override
    protected boolean meetsRequirement(UUID uuid) {

        // Add dependency
        addDependency(Library.JOBS);

        double points = -1;

        if (jobsHandler == null || !jobsHandler.isHooked()) {
            points = -1;
        } else {
            points = jobsHandler.getTotalPoints(uuid);
        }

        return points >= totalPoints;
    }

    @Override
    public boolean initRequirement(final String[] options) {

        jobsHandler = (JobsHook) this.getAutorank().getDependencyManager().getLibraryHook(Library.JOBS).orElse(null);

        try {
            totalPoints = Integer.parseInt(options[0]);
        } catch (NumberFormatException e) {
            this.registerWarningMessage("An invalid number is provided");
            return false;
        }

        if (totalPoints < 0) {
            this.registerWarningMessage("No level is provided or smaller than 0.");
            return false;
        }

        if (jobsHandler == null || !jobsHandler.isHooked()) {
            this.registerWarningMessage("Jobs is not available");
            return false;
        }

        return true;
    }

    @Override
    public double getProgressPercentage(UUID uuid) {
        double points = 0;

        if (jobsHandler != null && !jobsHandler.isHooked()) {
            points = jobsHandler.getTotalPoints(uuid);
        }

        return points / this.totalPoints;
    }
}
