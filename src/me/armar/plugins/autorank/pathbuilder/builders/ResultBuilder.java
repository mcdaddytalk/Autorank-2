package me.armar.plugins.autorank.pathbuilder.builders;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.pathbuilder.result.AbstractResult;
import me.armar.plugins.autorank.util.AutorankTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.*;

/**
 * This class is used to create a new AbstractResult. It uses the Factory Method Design Pattern.
 */
public class ResultBuilder {

    private static final Map<String, Class<? extends AbstractResult>> results = new HashMap<String, Class<? extends
            AbstractResult>>();

    // Keep track of the associated abstractResult.
    private AbstractResult abstractResult = null;

    // Whether the associated abstractResult is valid.
    private boolean isValid = false;

    // Extra metadata for the associated abstractResult.
    private String pathName, resultName, originalResultString;

    /**
     * Add a new type of AbstractResult that can be used in the Paths.yml file.
     *
     * @param type   String literal that must be used in the file to identify the abstractResult.
     * @param result Class of the AbstractResult that must be instantiated.
     */
    public static void registerResult(final String type, final Class<? extends AbstractResult> result) {
        results.put(type, result);

        // Add type to the list of AutorankTools so it can use the correct name.
        AutorankTools.registerResult(type);
    }

    /**
     * Remove a registered result with the given type.
     *
     * @param type Identifier of the result
     * @return true if it was removed, false if there was no element with the given identifier.
     */
    public static boolean unRegisterResult(String type) {
        if (!results.containsKey(type)) {
            return false;
        }

        results.remove(type);

        return true;
    }

    /**
     * Get all results that are registered.
     *
     * @return A collection of result classes that are registered.
     */
    public static List<Class<? extends AbstractResult>> getRegisteredResults() {
        return new ArrayList<>(results.values());
    }

    /**
     * Get the result that is registered with the given type.
     *
     * @param type Identifier of the result.
     * @return result if found, or null.
     */
    public static Optional<Class<? extends AbstractResult>> getRegisteredResult(String type) {
        return Optional.ofNullable(results.getOrDefault(type, null));
    }

    /**
     * Create a AbstractResult using the ResultBuilder factory.
     *
     * @param pathName    Name of the path the abstractResult is in.
     * @param resultType  Type of the abstractResult, which does not have to be the exact string value.
     * @param stringValue Value of the abstractResult string.
     * @return a newly created AbstractResult with the given storage, or null if invalid storage was given.
     */
    public static AbstractResult createResult(String pathName, String resultType, String stringValue) {
        ResultBuilder builder = new ResultBuilder().createEmpty(pathName, resultType).populateResult(stringValue);

        // Check if abstractResult is valid before building it.
        if (!builder.isValid()) {
            return null;
        }

        // Get abstractResult of ResultBuilder.

        return builder.finish();
    }

    public final Autorank getAutorank() {
        return Autorank.getInstance();
    }

    /**
     * Create an empty AbstractResult.
     *
     * @param pathName   Name of the path that this abstractResult is in.
     * @param resultType Type of the abstractResult.
     * @return this builder.
     */
    public ResultBuilder createEmpty(String pathName, String resultType) {

        this.pathName = pathName;

        this.originalResultString = resultType;

        resultType = AutorankTools.findMatchingResultName(resultType);

        this.resultName = resultType;

        if (resultType == null) {
            Autorank.getInstance().getWarningManager().registerWarning(
                    String.format("You are using a '%s' result in path '%s', but that result doesn't " +
                                    "exist!", originalResultString,
                            pathName),
                    10);
            return this;
        }

        final Class<? extends AbstractResult> c = results.get(resultType);
        if (c != null) {
            try {
                abstractResult = c.newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getServer().getConsoleSender()
                    .sendMessage("[Autorank] " + ChatColor.RED + "Result '" + originalResultString + "' is not a " +
                            "valid " +
                            "result type!");
        }
        return this;
    }

    /**
     * Populate the created AbstractResult with storage.
     *
     * @return this builder.
     */
    public ResultBuilder populateResult(String stringValue) {

        if (abstractResult == null) {
            return this;
        }

        if (stringValue == null) {
            return this;
        }

        // Initialize the abstractResult with options.
        abstractResult.setOptions(stringValue.split(";"));

        // Check if there is a custom description set for this result, if so, set it.
        if (getAutorank().getPathsConfig().hasCustomResultDescription(this.pathName, this.originalResultString)) {
            abstractResult.setCustomDescription(getAutorank().getPathsConfig().getCustomResultDescription(this
                    .pathName, this.originalResultString));
        }

        abstractResult.setGlobal(getAutorank().getPathsConfig().isResultGlobal(this.pathName,
                this.originalResultString));

        // AbstractResult is non-null and populated with storage, so valid.
        isValid = true;

        return this;
    }

    /**
     * Finish the creation of the AbstractResult, will return the abstractResult object that was created.
     *
     * @return created AbstractResult object.
     * @throws IllegalStateException if the abstractResult was not valid and could not be finished.
     */
    public AbstractResult finish() throws IllegalStateException {
        if (!isValid || abstractResult == null) {
            throw new IllegalStateException("Result '" + originalResultString + "' of '" + pathName + "' was not " +
                    "valid and could not be finished.");
        }

        return abstractResult;
    }

    /**
     * Check whether the associated abstractResult is valid.
     *
     * @return true if it is, false otherwise.
     */
    public boolean isValid() {
        return isValid;
    }

}
