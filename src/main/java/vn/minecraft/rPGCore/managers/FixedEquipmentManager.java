package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import vn.minecraft.rPGCore.RPGCore;

import java.io.File;
import java.util.*;

public class FixedEquipmentManager {

    private static FixedEquipmentManager instance;

    // Lưu trữ danh sách công thức, phân loại theo Tên Thư mục (Category)
    private final Map<String, List<FixedRecipe>> recipesByCategory = new HashMap<>();

    public static FixedEquipmentManager getInstance() {
        if (instance == null) instance = new FixedEquipmentManager();
        return instance;
    }

    public void loadAll() {
        recipesByCategory.clear();
        File baseDir = new File(RPGCore.getInstance().getDataFolder(), "Equipments");

        // Tạo sẵn các thư mục nếu chưa tồn tại
        String[] categories = {"Helmet", "Chestplate", "Leggings", "Boots", "Sword", "Bow", "Staff", "Dagger", "Whip", "Shield", "Scythe"};
        for (String cat : categories) {
            File catDir = new File(baseDir, cat);
            if (!catDir.exists()) catDir.mkdirs();
            recipesByCategory.put(cat.toLowerCase(), new ArrayList<>());
        }

        // Quét từng thư mục con
        int totalLoaded = 0;
        File[] subDirs = baseDir.listFiles(File::isDirectory);
        if (subDirs == null) return;

        for (File dir : subDirs) {
            String category = dir.getName().toLowerCase();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) continue;

            for (File file : files) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                // 1. Build Thành Phẩm
                String type = config.getString("item.type", "sword");
                Material mat = Material.matchMaterial(config.getString("item.material", "IRON_SWORD"));
                if (mat == null) mat = Material.IRON_SWORD;

                ItemStack result = new ItemStack(mat);
                ItemMeta meta = result.getItemMeta();
                meta.setDisplayName(config.getString("item.name", "&cUnknown").replace("&", "§"));
                meta.setCustomModelData(config.getInt("item.custom-model-data", 0));

                List<String> lore = config.getStringList("item.lore");
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) coloredLore.add(line.replace("&", "§"));
                meta.setLore(coloredLore);

                meta.setUnbreakable(true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

                // Gắn thẻ nhận diện Equipment cho Lò Rèn
                NamespacedKey eqKey = new NamespacedKey(RPGCore.getInstance(), "equipment_type");
                meta.getPersistentDataContainer().set(eqKey, PersistentDataType.STRING, type);

                if (config.getBoolean("item.unupgradable", false)) {
                    NamespacedKey upgKey = new NamespacedKey(RPGCore.getInstance(), "unupgradable");
                    meta.getPersistentDataContainer().set(upgKey, PersistentDataType.BYTE, (byte) 1);
                }
                result.setItemMeta(meta);

                // 2. Build Nguyên Liệu (Từ ItemManager)
                List<ItemStack> ingredients = new ArrayList<>();
                if (config.contains("recipe")) {
                    for (String key : config.getConfigurationSection("recipe").getKeys(false)) {
                        String reqId = config.getString("recipe." + key + ".id");
                        int amount = config.getInt("recipe." + key + ".amount", 1);
                        ingredients.add(getRealItem(reqId, amount));
                    }
                }

                recipesByCategory.get(category).add(new FixedRecipe(result, ingredients));
                totalLoaded++;
            }
        }
        Bukkit.getLogger().info("[RPGCore] Đã load thành công " + totalLoaded + " Trang bị Cố định từ YAML.");
    }

    public List<FixedRecipe> getRecipes(String category) {
        return recipesByCategory.getOrDefault(category.toLowerCase(), new ArrayList<>());
    }

    private ItemStack getRealItem(String itemId, int amount) {
        // Thử lấy Custom Item từ ItemManager
        ItemStack realItem = RPGCore.getInstance().getItemManager().buildItem(itemId, amount);
        if (realItem != null) {
            realItem.setAmount(amount); // Đảm bảo số lượng
            return realItem;
        }

        // [FIX]: Nếu không phải Custom Item, quét trong danh sách Vanilla Material
        Material vanillaMat = Material.matchMaterial(itemId.toUpperCase());
        if (vanillaMat != null) {
            return new ItemStack(vanillaMat, amount);
        }

        // Trả về barrier đỏ nếu cấu hình sai hoàn toàn
        ItemStack err = new ItemStack(Material.BARRIER, amount);
        ItemMeta m = err.getItemMeta();
        m.setDisplayName("§cLỗi ID: " + itemId);
        err.setItemMeta(m);
        return err;
    }

    public static class FixedRecipe {
        public final ItemStack result;
        public final List<ItemStack> ingredients;
        public FixedRecipe(ItemStack result, List<ItemStack> ingredients) {
            this.result = result;
            this.ingredients = ingredients;
        }
    }
}