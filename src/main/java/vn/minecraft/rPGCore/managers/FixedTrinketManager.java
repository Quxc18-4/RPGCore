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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FixedTrinketManager {

    private static FixedTrinketManager instance;
    private final List<FixedRecipe> loadedRecipes = new ArrayList<>();

    public static FixedTrinketManager getInstance() {
        if (instance == null) instance = new FixedTrinketManager();
        return instance;
    }

    public void loadAll() {
        loadedRecipes.clear();
        File dir = new File(RPGCore.getInstance().getDataFolder(), "trinkets");
        if (!dir.exists()) dir.mkdirs(); // Tự động tạo thư mục nếu chưa có

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            // 1. Build ItemStack (Thành phẩm) từ YAML
            String type = config.getString("item.type", "necklace");
            Material mat = Material.matchMaterial(config.getString("item.material", "GOLDEN_AXE"));
            if (mat == null) mat = Material.GOLDEN_AXE;

            ItemStack result = new ItemStack(mat);
            ItemMeta meta = result.getItemMeta();
            meta.setDisplayName(config.getString("item.name", "&cUnknown").replace("&", "§"));
            meta.setCustomModelData(config.getInt("item.custom-model-data", 1001));

            List<String> lore = config.getStringList("item.lore");
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(line.replace("&", "§"));
            meta.setLore(coloredLore);

            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            NamespacedKey typeKey = new NamespacedKey(RPGCore.getInstance(), "trinket_type");
            NamespacedKey uuidKey = new NamespacedKey(RPGCore.getInstance(), "trinket_uuid");
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString());

            // Gắn cờ Không thể nâng cấp cho Lò Rèn
            if (config.getBoolean("item.unupgradable", false)) {
                NamespacedKey upgKey = new NamespacedKey(RPGCore.getInstance(), "unupgradable");
                meta.getPersistentDataContainer().set(upgKey, PersistentDataType.BYTE, (byte) 1);
            }
            result.setItemMeta(meta);

            // 2. Build Recipe (Nguyên liệu) bằng cách Pull từ ItemManager
            List<ItemStack> ingredients = new ArrayList<>();
            if (config.contains("recipe")) {
                for (String key : config.getConfigurationSection("recipe").getKeys(false)) {
                    String reqId = config.getString("recipe." + key + ".id");
                    int amount = config.getInt("recipe." + key + ".amount", 1);

                    ItemStack reqItem = getRealItem(reqId, amount);
                    ingredients.add(reqItem);
                }
            }

            loadedRecipes.add(new FixedRecipe(result, ingredients));
        }
        Bukkit.getLogger().info("[RPGCore] Đã load thành công " + loadedRecipes.size() + " Trang sức Cố định từ YAML.");
    }

    public List<FixedRecipe> getRecipes() {
        return loadedRecipes;
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