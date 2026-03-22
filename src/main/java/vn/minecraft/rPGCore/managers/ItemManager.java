package vn.minecraft.rPGCore.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ItemManager {

    private final JavaPlugin plugin;
    public final NamespacedKey ITEM_KEY;

    // =========================================
    // 1. ĐỘ HIẾM (ENGLISH) & PHÂN LOẠI (VIỆT)
    // =========================================
    public enum Rarity {
        COMMON("§7", "Common"),       // Xám
        UNCOMMON("§a", "Uncommon"),   // Xanh lá
        RARE("§b", "Rare"),           // Xanh dương
        EPIC("§5", "Epic"),           // Tím
        MYTHIC("§c", "Mythic"),       // Đỏ
        LEGENDARY("§6", "Legendary"); // Vàng

        public final String color;
        public final String name;
        Rarity(String color, String name) { this.color = color; this.name = name; }
    }

    public enum ItemType {
        COOKING("Nguyên liệu nấu ăn"),
        MATERIAL("Vật phẩm"),
        CURRENCY("Tiền tệ");

        public final String displayName;
        ItemType(String displayName) { this.displayName = displayName; }
    }

    public enum Category {
        MEAT(ItemType.COOKING, "Thịt"), VEG(ItemType.COOKING, "Rau quả"),
        FISH(ItemType.COOKING, "Hải sản"), FAT(ItemType.COOKING, "Chất béo"),
        STARCH(ItemType.COOKING, "Tinh bột"), SPICE(ItemType.COOKING, "Gia vị"),

        MONSTER(ItemType.MATERIAL, "Rớt từ quái"), MAGIC(ItemType.MATERIAL, "Ma thuật"),
        MISC(ItemType.MATERIAL, "Khác"),

        COIN(ItemType.CURRENCY, "Tiền kim loại");

        public final ItemType type;
        public final String displayName;
        Category(ItemType type, String displayName) { this.type = type; this.displayName = displayName; }
    }

    // =========================================
    // 2. CẤU TRÚC DỮ LIỆU VẬT PHẨM
    // =========================================
    public static class ItemData {
        public String id;
        public Rarity rarity;
        public Category category;
        public double points;
        public Material material;
        public String name;
        public List<String> description;
        public String effect;

        public ItemData(String id, Rarity rarity, Category category, double points, Material mat, String name, String effect, String... description) {
            this.id = id;
            this.rarity = rarity;
            this.category = category;
            this.points = points;
            this.material = mat;
            this.name = name;
            this.effect = effect;
            this.description = Arrays.asList(description);
        }
    }

    private final Map<String, ItemData> registry = new HashMap<>();

    public ItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ITEM_KEY = new NamespacedKey(plugin, "rpg_item_id");
        registerAllItems();
    }

    // =========================================
    // 3. ĐĂNG KÝ DANH SÁCH VẬT PHẨM (GỘP CŨ & MỚI)
    // =========================================
    private void registerAllItems() {

        // --- 1. NHÓM THỊT (MEAT) ---
        register(new ItemData("meat_beef", Rarity.COMMON, Category.MEAT, 1.0, Material.BEEF, "Thịt Bò Tươi", null, "Những thớ thịt đỏ rực, giàu protein.")); // Rớt từ: Bò Hoang Dã
        register(new ItemData("meat_pork", Rarity.COMMON, Category.MEAT, 1.0, Material.PORKCHOP, "Thịt Heo Rừng", null, "Thịt heo rừng hoang dã rất dai và chắc.")); // Rớt từ: Heo Hoang Dã
        register(new ItemData("meat_mutton", Rarity.COMMON, Category.MEAT, 1.0, Material.MUTTON, "Thịt Cừu Non", null, "Mềm mại, thơm mùi cỏ non."));
        register(new ItemData("meat_chicken", Rarity.COMMON, Category.MEAT, 0.5, Material.CHICKEN, "Thịt Gà Ta", null, "Dai ngon và rất dễ chế biến.")); // Rớt từ: Gà Hoang Dã, Gà Lôi
        register(new ItemData("meat_rabbit", Rarity.COMMON, Category.MEAT, 0.25, Material.RABBIT, "Thịt Thỏ Đồng", null, "Thịt nạc, ít mỡ."));

        // --- 2. NHÓM HẢI SẢN (FISH) ---
        register(new ItemData("fish_cod_small", Rarity.COMMON, Category.FISH, 0.25, Material.COD, "Cá Tuyết Nhỏ", null, "Một con cá tuyết bé xíu."));
        register(new ItemData("fish_cod_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.COD, "Cá Tuyết Vừa", null, "Kích cỡ tiêu chuẩn."));
        register(new ItemData("fish_cod_large", Rarity.RARE, Category.FISH, 1.0, Material.COD, "Cá Tuyết Lớn", null, "Con cá khổng lồ, rất nhiều thịt."));
        register(new ItemData("fish_salmon_small", Rarity.COMMON, Category.FISH, 0.25, Material.SALMON, "Cá Hồi Nhỏ", null, "Cá hồi non."));
        register(new ItemData("fish_salmon_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.SALMON, "Cá Hồi Vừa", null, "Cá hồi tươi ngon."));
        register(new ItemData("fish_salmon_large", Rarity.RARE, Category.FISH, 1.0, Material.SALMON, "Cá Hồi Lớn", null, "Thịt cá hồi hảo hạng."));
        register(new ItemData("fish_tropical", Rarity.UNCOMMON, Category.FISH, 0.5, Material.TROPICAL_FISH, "Cá Hề Biển Sâu", null, "Màu sắc sặc sỡ."));

        // --- 3. NHÓM RAU QUẢ (VEG) ---
        register(new ItemData("veg_pumpkin", Rarity.COMMON, Category.VEG, 1.0, Material.PUMPKIN, "Bí Ngô Nguyên Quả", null, "Trái bí ngô to tròn.")); // Rớt từ: SmolSlime
        register(new ItemData("veg_melon", Rarity.COMMON, Category.VEG, 0.5, Material.MELON_SLICE, "Dưa Hấu Mọng Nước", null, "Ngọt ngào và giải khát."));
        register(new ItemData("veg_carrot", Rarity.COMMON, Category.VEG, 0.5, Material.CARROT, "Cà Rốt Ngọt", null, "Tốt cho mắt."));
        register(new ItemData("veg_beetroot", Rarity.COMMON, Category.VEG, 0.5, Material.BEETROOT, "Củ Dền Đỏ", null, "Dùng để nấu canh rất ngon."));
        register(new ItemData("veg_apple", Rarity.COMMON, Category.VEG, 0.5, Material.APPLE, "Táo Đỏ", null, "Táo rừng chín mọng.")); // Rớt từ: SmolSlime
        register(new ItemData("veg_mushroom", Rarity.COMMON, Category.VEG, 0.25, Material.BROWN_MUSHROOM, "Nấm Hương", null, "Thơm lừng khi xào.")); // Rớt từ: SmolSlime
        register(new ItemData("veg_berries", Rarity.COMMON, Category.VEG, 0.25, Material.SWEET_BERRIES, "Quả Mọng Rừng", null, "Vị chua chua ngọt ngọt."));
        register(new ItemData("veg_kelp_fresh", Rarity.COMMON, Category.VEG, 0.25, Material.KELP, "Rong Biển Tươi", null, "Vớt từ dưới đáy biển lên.")); // Rớt từ: SmolSlime
        register(new ItemData("veg_kelp_dried", Rarity.COMMON, Category.VEG, 0.25, Material.DRIED_KELP, "Rong Biển Khô", null, "Đã được sấy khô để bảo quản."));
        register(new ItemData("veg_potato", Rarity.COMMON, Category.VEG, 0.5, Material.POTATO, "Khoai Tây Vàng", "Cung cấp thêm 0.5 Điểm Tinh bột", "Củ khoai tây bùi béo."));

        // --- 4. NHÓM TINH BỘT (STARCH) ---
        register(new ItemData("starch_bread", Rarity.COMMON, Category.STARCH, 1.0, Material.BREAD, "Ổ Bánh Mì Nướng", null, "Thơm mùi lúa mạch.")); // Rớt từ: SmolSlime
        register(new ItemData("starch_flour", Rarity.COMMON, Category.STARCH, 0.25, Material.SUGAR, "Bột Mì Thượng Hạng", null, "Xay nhuyễn từ lúa mì ngon nhất."));

        // --- 5. NHÓM GIA VỊ (SPICE) ---
        register(new ItemData("spice_salt", Rarity.COMMON, Category.SPICE, 0.5, Material.WHITE_DYE, "Muối Biển", null, "Gia vị không thể thiếu."));
        register(new ItemData("spice_pepper", Rarity.COMMON, Category.SPICE, 0.5, Material.GUNPOWDER, "Tiêu Đen", null, "Cay nồng."));
        register(new ItemData("spice_chili", Rarity.COMMON, Category.SPICE, 0.5, Material.NETHER_WART, "Bột Ớt Hiểm", null, "Cực kỳ cay."));
        register(new ItemData("spice_garlic", Rarity.COMMON, Category.SPICE, 0.5, Material.PUMPKIN_SEEDS, "Tỏi Bóc Vỏ", null, "Dùng để phi thơm."));
        register(new ItemData("spice_sugar", Rarity.COMMON, Category.SPICE, 0.5, Material.SUGAR, "Đường Tinh Luyện", null, "Ngọt ngào."));
        register(new ItemData("spice_honey", Rarity.UNCOMMON, Category.SPICE, 1.0, Material.HONEY_BOTTLE, "Mật Ong Rừng", "Giải độc tức thì (Tương đương 2 Đường)", "Mật đặc sánh và cực kỳ ngọt ngào.")); // Rớt từ: Ong Độc
        register(new ItemData("spice_sour", Rarity.COMMON, Category.SPICE, 0.5, Material.POTION, "Giấm Chua", null, "Tạo vị chua thanh."));

        // --- 6. NHÓM CHẤT BÉO (FAT) ---
        register(new ItemData("fat_egg", Rarity.COMMON, Category.FAT, 0.5, Material.EGG, "Trứng Gà Tươi", null, "Một quả trứng vừa mới đẻ.")); // Rớt từ: Gà Hoang Dã
        register(new ItemData("fat_sniffer", Rarity.RARE, Category.FAT, 0.75, Material.SNIFFER_EGG, "Trứng Cổ Đại", "Tăng 10% Tỉ lệ rớt đồ", "Toát ra một thứ hào quang cổ đại.")); // Rớt từ: Gà Hoang Dã, Gà Lôi
        register(new ItemData("fat_milk", Rarity.UNCOMMON, Category.FAT, 1.0, Material.MILK_BUCKET, "Sữa Bò Béo Ngậy", "Tăng 5% Tỉ lệ hồi phục", "Sữa vắt từ bò hoang dã, rất thơm.")); // Rớt từ: Bò Hoang Dã
        register(new ItemData("fat_lard", Rarity.UNCOMMON, Category.FAT, 0.5, Material.SNOWBALL, "Mỡ Lợn", null, "Dùng để chiên xào cực kỳ thơm.")); // Rớt từ: Heo Hoang Dã

        // --- 7. NHÓM QUÁI VẬT (MONSTER) ---
        register(new ItemData("monster_flesh", Rarity.COMMON, Category.MONSTER, 1.0, Material.ROTTEN_FLESH, "Thịt Thối Rữa", "Nguy cơ làm hỏng món ăn rất cao!", "Thịt của quái vật đột biến, không thể ăn được.")); // Rớt từ: Heo Nhiễm Bệnh

        // --- 8. NHÓM MA THUẬT (MAGIC) ---
        register(new ItemData("magic_golden_carrot", Rarity.RARE, Category.MAGIC, 0.75, Material.GOLDEN_CARROT, "Cà Rốt Ma Thuật", "Cung cấp hiệu ứng Mắt Mèo", "Trồng bằng phép thuật."));
        register(new ItemData("magic_blaze_powder", Rarity.RARE, Category.MAGIC, 0.75, Material.BLAZE_POWDER, "Mảnh Quỷ Lửa", "Cung cấp hiệu ứng Kháng Lửa", "Nóng rực trên tay."));
        register(new ItemData("magic_fermented_eye", Rarity.RARE, Category.MAGIC, 0.75, Material.FERMENTED_SPIDER_EYE, "Mắt Nhện Lên Men", "Cung cấp hiệu ứng Hồi Phục Cao", "Mùi vị cực kỳ kinh tởm."));
        register(new ItemData("magic_pufferfish", Rarity.RARE, Category.MAGIC, 0.75, Material.PUFFERFISH, "Cá Nóc Kịch Độc", "Cung cấp hiệu ứng Thở Dưới Nước", "Phải biết cách sơ chế mới dùng được."));
        register(new ItemData("magic_spider_eye", Rarity.UNCOMMON, Category.MAGIC, 0.75, Material.SPIDER_EYE, "Mắt Nhện Tinh Anh", "33% cơ hội gây Độc I khi tấn công", "Trông khá tởm, nhưng chứa đầy chất độc.")); // Rớt từ: Nhện Con
        register(new ItemData("magic_bat_wing", Rarity.RARE, Category.MAGIC, 0.75, Material.POTION, "Huyết Tinh", "Cung cấp hiệu ứng Hút Máu", "Máu của sinh vật bóng tối."));
        register(new ItemData("magic_glowstone", Rarity.RARE, Category.MAGIC, 0.75, Material.GLOWSTONE_DUST, "Bột Tiên", "Cung cấp Chí Mạng & May Mắn", "Bột phát sáng lấp lánh."));
        register(new ItemData("magic_goblin_ear", Rarity.RARE, Category.MAGIC, 0.75, Material.TURTLE_SCUTE, "Tai Goblin", "Mang theo người nhận +15% Tỉ lệ rớt đồ", "Một chiếc tai còn rỏ máu của Goblin.")); // Rớt từ: Goblin

        // --- 9. NHÓM KHÁC (MISC) ---
        register(new ItemData("misc_leather", Rarity.COMMON, Category.MISC, 0.0, Material.LEATHER, "Da Thuộc", null, "Nguyên liệu may mặc cơ bản.")); // Rớt từ: Bò Hoang Dã, Heo Hoang Dã, Heo Nhiễm Bệnh
        register(new ItemData("misc_feather", Rarity.COMMON, Category.MISC, 0.0, Material.FEATHER, "Lông Vũ Cơ Bản", null, "Nhẹ nhàng và mềm mại.")); // Rớt từ: Gà Hoang Dã
        register(new ItemData("misc_electric_feather", Rarity.RARE, Category.MISC, 0.0, Material.FEATHER, "Lông Vũ Nhiễm Điện", "30% cơ hội gọi Sét gây 150% Sát thương", "Vẫn còn phát ra những tia lửa lách tách...")); // Rớt từ: Gà Lôi
        register(new ItemData("misc_weird_leather", Rarity.UNCOMMON, Category.MISC, 0.0, Material.RABBIT_HIDE, "Da Thuộc Kì Lạ", null, "Bốc mùi thối rữa và rỉ dịch xanh.")); // Rớt từ: Heo Nhiễm Bệnh
        register(new ItemData("misc_pollen", Rarity.COMMON, Category.MISC, 0.0, Material.YELLOW_DYE, "Phấn Hoa Tự Nhiên", null, "Mùi thơm dịu nhẹ, dùng làm dược liệu.")); // Rớt từ: Ong Độc
        register(new ItemData("misc_poison_stinger", Rarity.RARE, Category.MISC, 0.0, Material.ARROW, "Ngòi Ong Độc", "Tấn công gây Độc I trong 5s", "Cực kỳ sắc bén, cẩn thận kẻo đứt tay.")); // Rớt từ: Ong Độc
        register(new ItemData("misc_armor_shard", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Mảnh Giáp Vụn", null, "Có thể nung chảy để tái chế tại Lò Rèn.")); // Rớt từ: Goblin
        register(new ItemData("misc_soft_string", Rarity.COMMON, Category.MISC, 0.0, Material.STRING, "Sợi Tơ Mềm", null, "Một cuộn tơ tằm mềm mại.")); // Rớt từ: Nhện Con
        register(new ItemData("misc_steel_string", Rarity.UNCOMMON, Category.MISC, 0.0, Material.STRING, "Sợi Tơ Thép", null, "Cứng cáp lạ thường, cực kỳ bền bỉ.")); // Rớt từ: Nhện Con
    }

    private void register(ItemData data) {
        registry.put(data.id.toLowerCase(), data);
    }

    // =========================================
    // GIAO DIỆN UI: MÔ TẢ NẰM DƯỚI CÙNG
    // =========================================
    public ItemStack buildItem(String id, int amount) {
        ItemData data = registry.get(id.toLowerCase());
        if (data == null) return null;

        ItemStack item = new ItemStack(data.material, amount);
        ItemMeta meta = item.getItemMeta();

        // Tên có màu theo Rarity
        meta.setDisplayName(data.rarity.color + "§l" + data.name);

        List<String> lore = new ArrayList<>();

        // Dòng 1: Phân loại
        lore.add("§8[ Phân loại: " + data.category.type.displayName + " - " + data.category.displayName + " ]");

        boolean hasAttributes = (data.points > 0) || (data.effect != null && !data.effect.isEmpty());
        if (hasAttributes) lore.add(""); // Tạo 1 dòng trống cho thoáng nếu có thuộc tính

        // Khối Thuộc tính & Hiệu ứng
        if (hasAttributes) {
            // Điểm nấu ăn
            if (data.points > 0) {
                lore.add("§f✦ Thuộc tính Nguyên liệu:");
                lore.add("§f ➥ §e+" + data.points + " §7Điểm " + data.category.displayName);
            }

            // Khoảng trống giữa 2 thuộc tính
            if (data.points > 0 && data.effect != null && !data.effect.isEmpty()) {
                lore.add("");
            }

            // Hiệu ứng
            if (data.effect != null && !data.effect.isEmpty()) {
                lore.add("§f✦ Hiệu quả Đặc biệt:");
                lore.add("§f ➥ §a" + data.effect);
            }
        }

        // Đường kẻ chốt sổ phần kỹ thuật
        lore.add("§8§m                                ");

        // Mô tả cốt truyện (Flavor text) nằm dưới cùng
        if (data.description != null && !data.description.isEmpty()) {
            for (String line : data.description) {
                lore.add("§7" + line); // Màu xám nhẹ cho giống text cốt truyện
            }
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, data.id);
        item.setItemMeta(meta);
        return item;
    }

    public List<String> getAllIds() {
        return new ArrayList<>(registry.keySet());
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    public ItemData getItemData(String id) {
        if (id == null) return null;
        return registry.get(id.toLowerCase());
    }
}