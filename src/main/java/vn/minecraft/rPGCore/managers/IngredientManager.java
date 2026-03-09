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

    // Phân loại Nguyên liệu
    public enum Category {
        MEAT("Thịt"), VEG("Rau củ"), FISH("Hải sản"), FAT("Chất béo"),
        STARCH("Tinh bột"), MONSTER("Quái vật"), MAGIC("Ma thuật"),
        SPICE("Gia vị"), MISC("Khác");

        public final String displayName;
        Category(String displayName) { this.displayName = displayName; }
    }

    // Class chứa dữ liệu nội bộ
    public static class IngredientData {
        public String id;
        public Category category;
        public double points; // Số điểm (0.25 -> 1.0)
        public Material material;
        public String name;
        public List<String> lore;

        public IngredientData(String id, Category category, double points, Material mat, String name, String... loreLines) {
            this.id = id;
            this.category = category;
            this.points = points;
            this.material = mat;
            this.name = name;
            this.lore = Arrays.asList(loreLines);
        }
    }

    // Kho lưu trữ vĩnh viễn
    private final Map<String, IngredientData> registry = new HashMap<>();

    public IngredientManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.INGREDIENT_KEY = new NamespacedKey(plugin, "rpg_ingredient_id");
        registerAllIngredients();
    }

    // ===============================================
    // ĐĂNG KÝ DANH SÁCH NGUYÊN LIỆU TẠI ĐÂY
    // ===============================================
    private void registerAllIngredients() {
        // 1. NHÓM THỊT (MEAT)
        register(new IngredientData("meat_beef", Category.MEAT, 1.0, Material.BEEF, "§cThịt Bò Tươi", "§7Cung cấp 1.0 Điểm Thịt"));
        register(new IngredientData("meat_chicken", Category.MEAT, 0.5, Material.CHICKEN, "§fThịt Gà Ta", "§7Cung cấp 0.5 Điểm Thịt"));
        register(new IngredientData("meat_bird", Category.MEAT, 0.25, Material.CHICKEN, "§fThịt Chim Rừng", "§7Cung cấp 0.25 Điểm Thịt", "§8§o(Săn bắn từ chim hoang dã)"));

        // 2. NHÓM RAU (VEG)
        register(new IngredientData("veg_carrot", Category.VEG, 0.5, Material.CARROT, "§6Cà Rốt Ngọt", "§7Cung cấp 0.5 Điểm Rau củ"));
        register(new IngredientData("veg_mushroom", Category.VEG, 0.25, Material.BROWN_MUSHROOM, "§eNấm Hương", "§7Cung cấp 0.25 Điểm Rau củ"));

        // 3. NHÓM GIA VỊ (SPICE) - Biến tấu từ vật phẩm Vanilla
        register(new IngredientData("spice_salt", Category.SPICE, 0.5, Material.SUGAR, "§fMuối Biển", "§7Tăng hệ số thời gian buff", "§8§o(Dùng nhiều sẽ gây khát nước)"));
        register(new IngredientData("spice_pepper", Category.SPICE, 0.5, Material.GUNPOWDER, "§8Tiêu Đen", "§7Tăng hệ số sức mạnh", "§8§o(Hơi cay nồng)"));
        register(new IngredientData("spice_chili", Category.SPICE, 1.0, Material.NETHER_WART, "§cBột Ớt Hiểm", "§7Giúp giữ ấm cơ thể", "§8§o(Dùng nhiều sẽ gây bỏng rát)"));
        register(new IngredientData("spice_garlic", Category.SPICE, 1.0, Material.PUMPKIN_SEEDS, "§eTỏi Bóc Vỏ", "§7Gia vị khử mùi hôi của thịt quái", "§8§o(Giảm tỉ lệ nấu hỏng)"));

        // 4. NHÓM QUÁI VẬT & MA THUẬT (MONSTER / MAGIC)
        register(new IngredientData("monster_flesh", Category.MONSTER, 1.0, Material.ROTTEN_FLESH, "§2Thịt Thối Rữa", "§7Cung cấp 1.0 Điểm Quái vật", "§cNguy cơ làm hỏng món ăn rất cao!"));
        register(new IngredientData("magic_bone", Category.MAGIC, 0.5, Material.BONE, "§bXương Cốt Tủy", "§7Cung cấp 0.5 Điểm Ma Thuật", "§7Dùng để ninh nước dùng đặc biệt."));

        // 5. NHÓM CHẤT BÉO (FAT)
        register(new IngredientData("fat_egg", Category.FAT, 0.5, Material.EGG, "§fTrứng Gà Tươi", "§7Cung cấp 0.5 Điểm Béo", "§7Giúp món ăn thêm đậm đà."));
    }

    private void register(IngredientData data) {
        registry.put(data.id.toLowerCase(), data);
    }

    // ===============================================
    // CÁC HÀM XUẤT/NHẬP DỮ LIỆU
    // ===============================================
    // Tạo ra vật phẩm thực tế để đưa cho người chơi
    public ItemStack buildItem(String id, int amount) {
        IngredientData data = registry.get(id.toLowerCase());
        if (data == null) return null;

        ItemStack item = new ItemStack(data.material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(data.name);

        List<String> lore = new ArrayList<>(data.lore);
        lore.add("");
        lore.add("§8▶ Phân loại: §e" + data.category.displayName);
        meta.setLore(lore);

        // GẮN MÃ ẨN VÀO VẬT PHẨM (Để vạc nhận diện)
        meta.getPersistentDataContainer().set(INGREDIENT_KEY, PersistentDataType.STRING, data.id);

        item.setItemMeta(meta);
        return item;
    }

    // Kiểm tra xem 1 ItemStack có phải là nguyên liệu hợp lệ không
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