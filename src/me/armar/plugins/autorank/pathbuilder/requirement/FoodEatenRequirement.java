package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.language.Lang;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FoodEatenRequirement extends AbstractRequirement {

    FoodWrapper foodEaten = null;

    @Override
    public String getDescription() {

        String desc = "";

        final int amount = foodEaten.getAmount();
        final String foodType = foodEaten.getFoodName();

        if (foodType == null || foodType.trim().equals("")) {
            desc = Lang.FOOD_EATEN_REQUIREMENT.getConfigValue(amount + " food");
        } else {
            desc = Lang.FOOD_EATEN_REQUIREMENT
                    .getConfigValue(amount + " " + foodType.toLowerCase().replace("_", " ") + "(s)");
        }

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            desc = desc.concat(" (in world '" + this.getWorld() + "')");
        }

        return desc;
    }

    @Override
    public String getProgressString(UUID uuid) {

        String progress = "";

        final int amount = foodEaten.getAmount();
        String foodType = foodEaten.getFoodName();

        int totalFoodEaten = 0;

        if (foodType != null) {
            totalFoodEaten = this.getStatisticsManager().getFoodEaten(uuid, this.getWorld(),
                    Material.getMaterial(foodType));
            foodType = foodType.toLowerCase();
        } else {
            totalFoodEaten = this.getStatisticsManager().getFoodEaten(uuid, this.getWorld(),
                    null);
            foodType = "food";
        }

        progress = progress.concat(totalFoodEaten + "/" + amount + " " + foodType.replace("_", " ") + "(s)");

        return progress;
    }

    @Override
    protected boolean meetsRequirement(UUID uuid) {
        final int amount = foodEaten.getAmount();
        final String foodType = foodEaten.getFoodName();

        int totalFoodEaten = 0;

        if (foodType == null) {
            totalFoodEaten = this.getStatisticsManager().getFoodEaten(uuid, this.getWorld(), null);
        } else {
            totalFoodEaten = this.getStatisticsManager().getFoodEaten(uuid, this.getWorld(),
                    Material.getMaterial(foodType));
        }

        return totalFoodEaten >= amount;
    }

    @Override
    public boolean initRequirement(final String[] options) {
        final int total = Integer.parseInt(options[0]);
        String foodType = "";

        if (options.length > 1) {
            foodType = options[1].trim();
        }

        Material foodMaterial = Material.matchMaterial(foodType);

        if (foodMaterial == null) {
            this.registerWarningMessage("Food '" + foodType + "' is not a valid type of food.");
            return false;
        }

        foodEaten = new FoodWrapper(foodMaterial, total);

        return true;
    }

    @Override
    public double getProgressPercentage(UUID uuid) {
        int totalFoodEaten = 0;

        String foodType = foodEaten.getFoodName();

        if (foodType == null) {
            totalFoodEaten = this.getStatisticsManager().getFoodEaten(uuid, this.getWorld(), null);
        } else {
            totalFoodEaten = this.getStatisticsManager().getFoodEaten(uuid, this.getWorld(), Material.getMaterial(foodType));
        }

        return totalFoodEaten * 1.0d / foodEaten.getAmount();
    }
}

class FoodWrapper {

    private int amount;
    private ItemStack foodItem;

    public FoodWrapper(final Material foodMaterial, final int amount) {
        this.setAmount(amount);
        this.setFoodItem(new ItemStack(foodMaterial, amount));
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public ItemStack getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(final ItemStack foodItem) {
        this.foodItem = foodItem;
    }

    public String getFoodName() {
        return foodItem.getType().name();
    }
}
