package vn.minecraft.rPGCore.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class IngredientManager {

    private final JavaPlugin plugin;
    public final NamespacedKey INGREDIENT_KEY;

    public enum Category {
        MEAT("Thịt"), VEG("Rau Quả"), FISH("Hải sản"), FAT("Chất béo"),
        STARCH("Tinh bột"), MONSTER("Quái vật"), MAGIC("Ma thuật"),
        SPICE("Gia vị"), MISC("Khác");

        public final String displayName;
        Category(String displayName) { this.displayName = displayName; }
    }

    public static class IngredientData {
        public String id;
        public Category category;
        public double points;
        public Material material;
        public String name;
        public List<String> customLore; // Đổi tên để không nhầm lẫn

        public IngredientData(String id, Category category, double points, Material mat, String name, String... customLoreLines) {
            this.id = id;
            this.category = category;
            this.points = points;
            this.material = mat;
            this.name = name;
            this.customLore = Arrays.asList(customLoreLines);
        }
    }

    private final Map<String, IngredientData> registry = new HashMap<>();

    public IngredientManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.INGREDIENT_KEY = new NamespacedKey(plugin, "rpg_ingredient_id");
        registerAllIngredients();
    }

    private void registerAllIngredients() {
        // 1. NHÓM THỊT (MEAT)
        register(new IngredientData("meat_beef", Category.MEAT, 1.0, Material.BEEF, "§cThịt Bò Tươi"));
        register(new IngredientData("meat_pork", Category.MEAT, 1.0, Material.PORKCHOP, "§cThịt Heo Rừng"));
        register(new IngredientData("meat_mutton", Category.MEAT, 1.0, Material.MUTTON, "§cThịt Cừu Non"));
        register(new IngredientData("meat_chicken", Category.MEAT, 0.5, Material.CHICKEN, "§fThịt Gà Ta"));
        register(new IngredientData("meat_rabbit", Category.MEAT, 0.25, Material.RABBIT, "§fThịt Thỏ Đồng"));

        // 2. NHÓM HẢI SẢN (FISH) - 3 Kích thước
        register(new IngredientData("fish_cod_small", Category.FISH, 0.25, Material.COD, "§bCá Tuyết Nhỏ"));
        register(new IngredientData("fish_cod_med", Category.FISH, 0.5, Material.COD, "§bCá Tuyết Vừa"));
        register(new IngredientData("fish_cod_large", Category.FISH, 1.0, Material.COD, "§bCá Tuyết Lớn"));
        register(new IngredientData("fish_salmon_small", Category.FISH, 0.25, Material.SALMON, "§dCá Hồi Nhỏ"));
        register(new IngredientData("fish_salmon_med", Category.FISH, 0.5, Material.SALMON, "§dCá Hồi Vừa"));
        register(new IngredientData("fish_salmon_large", Category.FISH, 1.0, Material.SALMON, "§dCá Hồi Lớn"));
        register(new IngredientData("fish_tropical", Category.FISH, 0.5, Material.TROPICAL_FISH, "§eCá Hề Biển Sâu"));

        // 3. NHÓM RAU QUẢ (VEG)
        register(new IngredientData("veg_pumpkin", Category.VEG, 1.0, Material.PUMPKIN, "§6Bí Ngô Nguyên Quả"));
        register(new IngredientData("veg_melon", Category.VEG, 0.5, Material.MELON_SLICE, "§cDưa Hấu Mọng Nước"));
        register(new IngredientData("veg_carrot", Category.VEG, 0.5, Material.CARROT, "§6Cà Rốt Ngọt"));
        register(new IngredientData("veg_beetroot", Category.VEG, 0.5, Material.BEETROOT, "§4Củ Dền Đỏ"));
        register(new IngredientData("veg_apple", Category.VEG, 0.5, Material.APPLE, "§cTáo Đỏ"));
        register(new IngredientData("veg_mushroom", Category.VEG, 0.25, Material.BROWN_MUSHROOM, "§eNấm Hương"));
        register(new IngredientData("veg_berries", Category.VEG, 0.25, Material.SWEET_BERRIES, "§cQuả Mọng Rừng"));
        register(new IngredientData("veg_kelp_fresh", Category.VEG, 0.25, Material.KELP, "§2Rong Biển Tươi"));
        register(new IngredientData("veg_kelp_dried", Category.VEG, 0.25, Material.DRIED_KELP, "§8Rong Biển Khô"));

        // KHOAI TÂY LÀ TRƯỜNG HỢP ĐẶC BIỆT: Khai báo 0.5 VEG, Engine sẽ tự buff thêm 0.5 STARCH
        register(new IngredientData("veg_potato", Category.VEG, 0.5, Material.POTATO, "§eKhoai Tây Vàng", "§8§o(Cung cấp thêm 0.5 Điểm Tinh bột)"));

        // 4. NHÓM TINH BỘT (STARCH)
        register(new IngredientData("starch_bread", Category.STARCH, 1.0, Material.BREAD, "§6Ổ Bánh Mì Nướng"));
        register(new IngredientData("starch_flour", Category.STARCH, 0.25, Material.SUGAR, "§fBột Mì Thượng Hạng"));

        // 5. NHÓM GIA VỊ (SPICE) - Tất cả đều là 0.5
        register(new IngredientData("spice_salt", Category.SPICE, 0.5, Material.WHITE_DYE, "§fMuối Biển"));
        register(new IngredientData("spice_pepper", Category.SPICE, 0.5, Material.GUNPOWDER, "§8Tiêu Đen"));
        register(new IngredientData("spice_chili", Category.SPICE, 0.5, Material.NETHER_WART, "§cBột Ớt Hiểm"));
        register(new IngredientData("spice_garlic", Category.SPICE, 0.5, Material.PUMPKIN_SEEDS, "§eTỏi Bóc Vỏ"));
        register(new IngredientData("spice_sugar", Category.SPICE, 0.5, Material.SUGAR, "§fĐường Tinh Luyện"));
        register(new IngredientData("spice_honey", Category.SPICE, 1.0, Material.HONEY_BOTTLE, "§6Mật Ong Rừng", "§8§o(Tương đương 2 Đường)"));
        register(new IngredientData("spice_sour", Category.SPICE, 0.5, Material.POTION, "§fGiấm Chua"));

        // 6. NHÓM CHẤT BÉO (FAT)
        register(new IngredientData("fat_egg", Category.FAT, 0.5, Material.EGG, "§fTrứng Gà Tươi"));
        register(new IngredientData("fat_sniffer", Category.FAT, 0.75, Material.SNIFFER_EGG, "§2Trứng Kì Dị"));
        register(new IngredientData("fat_milk", Category.FAT, 1.0, Material.MILK_BUCKET, "§fXô Sữa Bò Dinh Dưỡng"));

        // 7. NHÓM QUÁI VẬT (MONSTER)
        register(new IngredientData("monster_flesh", Category.MONSTER, 1.0, Material.ROTTEN_FLESH, "§2Thịt Thối Rữa", "§cNguy cơ làm hỏng món ăn rất cao!"));

        // 8. NHÓM MA THUẬT (MAGIC) - TẤT CẢ LÀ 0.75 ĐIỂM
        register(new IngredientData("magic_golden_carrot", Category.MAGIC, 0.75, Material.GOLDEN_CARROT, "§eCà Rốt Ma Thuật", "§8(Hiệu ứng: Mắt Mèo)"));
        register(new IngredientData("magic_blaze_powder", Category.MAGIC, 0.75, Material.BLAZE_POWDER, "§cMảnh Quỷ Lửa", "§8(Hiệu ứng: Kháng Lửa)"));
        register(new IngredientData("magic_fermented_eye", Category.MAGIC, 0.75, Material.FERMENTED_SPIDER_EYE, "§5Mắt Nhện Lên Men", "§8(Hiệu ứng: Hồi Phục Cao)"));
        register(new IngredientData("magic_pufferfish", Category.MAGIC, 0.75, Material.PUFFERFISH, "§eCá Nóc Kịch Độc", "§8(Hiệu ứng: Thở Dưới Nước)"));
        register(new IngredientData("magic_spider_eye", Category.MAGIC, 0.75, Material.SPIDER_EYE, "§4Mắt Nhện", "§8(Hiệu ứng: Độc Tố Tuyệt Đối)"));
        register(new IngredientData("magic_bat_wing", Category.MAGIC, 0.75, Material.POTION, "§cHuyết Tinh", "§8(Hiệu ứng: Hút Máu)"));
        register(new IngredientData("magic_glowstone", Category.MAGIC, 0.75, Material.GLOWSTONE_DUST, "§eBột Tiên", "§8(Hiệu ứng: Chí Mạng & May Mắn)"));
    }

    private void register(IngredientData data) {
        registry.put(data.id.toLowerCase(), data);
    }

    public ItemStack buildItem(String id, int amount) {
        IngredientData data = registry.get(id.toLowerCase());
        if (data == null) return null;

        ItemStack item = new ItemStack(data.material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(data.name);

        List<String> lore = new ArrayList<>();
        // Áp dụng format UI mới: §f[§a+§f] 0.5 Điểm Phân_loại
        lore.add("§f[§a+§f] §7" + data.points + " Điểm " + data.category.displayName);

        if (!data.customLore.isEmpty()) {
            lore.addAll(data.customLore);
        }
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(INGREDIENT_KEY, PersistentDataType.STRING, data.id);
        item.setItemMeta(meta);
        return item;
    }

    public String getIngredientId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(INGREDIENT_KEY, PersistentDataType.STRING);
    }

    public IngredientData getIngredientData(String id) {
        return registry.get(id);
    }

    public Set<String> getAllIds() {
        return registry.keySet();
    }
}