package vn.minecraft.rPGCore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArrowRecipeManager {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;

    // Lưu trữ tạm trên RAM để lấy cho nhanh
    // Key: ID công thức (vd: "recipe_1"), Value: Map chứa các Slot và ItemStack
    private final Map<String, RecipeData> recipes = new HashMap<>();

    public ArrowRecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupFile();
        loadRecipes();
    }

    private void setupFile() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();
        file = new File(plugin.getDataFolder(), "arrow_recipes.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadRecipes() {
        recipes.clear();
        if (!config.contains("Recipes")) return;

        for (String key : config.getConfigurationSection("Recipes").getKeys(false)) {
            ItemStack result = config.getItemStack("Recipes." + key + ".Result");

            Map<Integer, ItemStack> ingredients = new HashMap<>();
            if (config.contains("Recipes." + key + ".Ingredients")) {
                for (String slotStr : config.getConfigurationSection("Recipes." + key + ".Ingredients").getKeys(false)) {
                    ingredients.put(Integer.parseInt(slotStr), config.getItemStack("Recipes." + key + ".Ingredients." + slotStr));
                }
            }
            recipes.put(key, new RecipeData(result, ingredients));
        }
        plugin.getLogger().info("Da tai thanh cong " + recipes.size() + " cong thuc mui ten!");
    }

    // Hàm lưu 1 công thức mới vào File
    public void saveRecipe(String recipeId, ItemStack result, Map<Integer, ItemStack> ingredients) {
        config.set("Recipes." + recipeId + ".Result", result);

        // Xóa data cũ của recipe này (nếu có)
        config.set("Recipes." + recipeId + ".Ingredients", null);

        for (Map.Entry<Integer, ItemStack> entry : ingredients.entrySet()) {
            config.set("Recipes." + recipeId + ".Ingredients." + entry.getKey(), entry.getValue());
        }

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

        // Cập nhật lại trên RAM
        recipes.put(recipeId, new RecipeData(result, ingredients));
    }

    public Map<String, RecipeData> getRecipes() { return recipes; }

    // Class nội bộ chứa dữ liệu của 1 công thức
    public static class RecipeData {
        public ItemStack result;
        public Map<Integer, ItemStack> ingredients; // Integer là vị trí ô (0-8) trong lưới 3x3

        public RecipeData(ItemStack result, Map<Integer, ItemStack> ingredients) {
            this.result = result;
            this.ingredients = ingredients;
        }
    }
}