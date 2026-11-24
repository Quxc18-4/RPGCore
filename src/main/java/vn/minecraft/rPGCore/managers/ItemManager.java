package vn.minecraft.rPGCore.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ItemManager {

    private final JavaPlugin plugin;
    public final NamespacedKey ITEM_KEY;

    private final Random rand = new Random();

    public enum Rarity {
        COMMON("§7", "Common"),
        UNCOMMON("§a", "Uncommon"),
        RARE("§b", "Rare"),
        EPIC("§5", "Epic"),
        MYTHIC("§c", "Mythic"),
        LEGENDARY("§6", "Legendary");

        public final String color;
        public final String name;
        Rarity(String color, String name) { this.color = color; this.name = name; }
    }

    public enum ItemType {
        COOKING("Nguyên liệu nấu ăn"),
        MATERIAL("Vật phẩm"),
        CURRENCY("Tiền tệ"),
        CONSUMABLE("Vật phẩm tiêu hao");

        public final String displayName;
        ItemType(String displayName) { this.displayName = displayName; }
    }

    public enum Category {
        MEAT(ItemType.COOKING, "Thịt"), VEG(ItemType.COOKING, "Rau quả"),
        FISH(ItemType.COOKING, "Hải sản"), FAT(ItemType.COOKING, "Chất béo"),
        STARCH(ItemType.COOKING, "Tinh bột"), SPICE(ItemType.COOKING, "Gia vị"),
        MONSTER(ItemType.COOKING, "Quái vật"), MAGIC(ItemType.COOKING, "Ma thuật"),
        MISC(ItemType.MATERIAL, "Khác"), COIN(ItemType.CURRENCY, "Tiền kim loại"),
        TOOL(ItemType.MATERIAL, "Công cụ"), BAIT_BASE(ItemType.MATERIAL, "Mồi nền"),
        BAIT_BUFF(ItemType.MATERIAL, "Mồi đặc biệt"), HERB(ItemType.MATERIAL, "Thảo dược");

        public final ItemType type;
        public final String displayName;
        Category(ItemType type, String displayName) { this.type = type; this.displayName = displayName; }
    }

    public static class ItemData {
        public String id; public Rarity rarity; public Category category; public double points;
        public Material material; public String name; public String effect; public List<String> description;

        public ItemData(String id, Rarity rarity, Category category, double points, Material mat, String name, String effect, String... description) {
            this.id = id; this.rarity = rarity; this.category = category; this.points = points;
            this.material = mat; this.name = name; this.effect = effect; this.description = Arrays.asList(description);
        }
    }

    private final Map<String, ItemData> registry = new HashMap<>();

    public ItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ITEM_KEY = new NamespacedKey(plugin, "rpg_item_id");
        registerAllItems();
    }

    private void registerAllItems() {
        // --- 1. NHÓM THỊT ---
        register(new ItemData("meat_beef", Rarity.COMMON, Category.MEAT, 1.0, Material.BEEF, "Thịt Bò Tươi", null, "Những thớ thịt đỏ rực, giàu protein."));
        register(new ItemData("meat_pork", Rarity.COMMON, Category.MEAT, 1.0, Material.PORKCHOP, "Thịt Heo Rừng", null, "Thịt heo rừng hoang dã rất dai và chắc."));
        register(new ItemData("meat_mutton", Rarity.COMMON, Category.MEAT, 1.0, Material.MUTTON, "Thịt Cừu Non", null, "Mềm mại, thơm mùi cỏ non."));
        register(new ItemData("meat_chicken", Rarity.COMMON, Category.MEAT, 0.5, Material.CHICKEN, "Thịt Gà Ta", null, "Dai ngon và rất dễ chế biến."));
        register(new ItemData("meat_rabbit", Rarity.COMMON, Category.MEAT, 0.25, Material.RABBIT, "Thịt Thỏ Sống", null, "Thịt nạc, ít mỡ."));
        register(new ItemData("meat_wolf_bone", Rarity.COMMON, Category.MEAT, 0.25, Material.BONE, "Xương Sói", "Nấu: Nước hầm xương cung cấp 0.25 Điểm Gia vị", "Đoạn xương chứa rất nhiều tủy."));
        register(new ItemData("meat_polar_bear_foot", Rarity.COMMON, Category.MEAT, 2.0, Material.RABBIT_FOOT, "Chân Gấu Tuyết", null, "Món móng gấu dày dặn béo ngậy, chứa lớp mỡ dày tích tụ qua mùa đông giá rét."));

        // --- 2. NHÓM HẢI SẢN ---
        register(new ItemData("fish_cod_small", Rarity.COMMON, Category.FISH, 0.25, Material.COD, "Cá Tuyết Nhỏ", null, "Một con cá tuyết bé xíu."));
        register(new ItemData("fish_cod_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.COD, "Cá Tuyết Vừa", null, "Kích cỡ tiêu chuẩn."));
        register(new ItemData("fish_cod_large", Rarity.RARE, Category.FISH, 1.0, Material.COD, "Cá Tuyết Lớn", null, "Con cá khổng lồ, rất nhiều thịt."));
        register(new ItemData("bfish_cod_mutant", Rarity.EPIC, Category.FISH, 1.25, Material.COD, "Cá Tuyết Đột Biến", "Nấu: Cung cấp 5% Sát thương", "Thịt cá tuyết đột biến, đậm đà."));
        register(new ItemData("fish_salmon_small", Rarity.COMMON, Category.FISH, 0.25, Material.SALMON, "Cá Hồi Nhỏ", null, "Cá hồi non."));
        register(new ItemData("fish_salmon_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.SALMON, "Cá Hồi Vừa", null, "Cá hồi tươi ngon."));
        register(new ItemData("fish_salmon_large", Rarity.RARE, Category.FISH, 1.0, Material.SALMON, "Cá Hồi Lớn", null, "Thịt cá hồi hảo hạng."));
        register(new ItemData("bfish_salmon_mutant", Rarity.EPIC, Category.FISH, 1.25, Material.SALMON, "Cá Hồi Đột Biến", "Nấu: Tăng 500 Máu tối đa", "Thịt cá hồi có vân mỡ kỳ lạ."));
        register(new ItemData("fish_tropical_small", Rarity.COMMON, Category.FISH, 0.1, Material.TROPICAL_FISH, "Cá Hề Nhỏ", null, "Màu sắc bình thường."));
        register(new ItemData("fish_tropical_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.TROPICAL_FISH, "Cá Hề Biển Sâu", null, "Màu sắc sặc sỡ."));
        register(new ItemData("fish_tropical_large", Rarity.RARE, Category.FISH, 1.0, Material.TROPICAL_FISH, "Cá Hề Lớn", null, "Màu sắc rực rỡ hơn bình thường."));
        register(new ItemData("bfish_tropical_mutant", Rarity.EPIC, Category.FISH, 1.25, Material.TROPICAL_FISH, "Cá Hề Đột Biến", "Nấu: Cung cấp 10% Tốc độ di chuyển", "Phát ra ánh sáng nhẹ."));

        // --- 2. NHÓM HẢI SẢN (HỆ THỐNG PHÂN LOẠI CHUẨN) ---
        // Cá Biển Khơi (OCEAN)
        // Cá Ngừ (Base Med: 0.5, COMMON)
        register(new ItemData("fish_tuna_small", Rarity.COMMON, Category.FISH, 0.25, Material.COD, "Cá Ngừ Nhỏ", null, "Cá ngừ bé tí, chả làm được gì"));
        register(new ItemData("fish_tuna_med", Rarity.COMMON, Category.FISH, 0.5, Material.COD, "Cá Ngừ", null, "Thịt chắc, thích hợp làm Sashimi"));
        register(new ItemData("fish_tuna_large", Rarity.UNCOMMON, Category.FISH, 1.0, Material.COD, "Cá Ngừ Lớn", null, "Cá ngừ to đạt chuẩn"));
        register(new ItemData("bfish_tuna_mutant", Rarity.RARE, Category.FISH, 1.25, Material.COD, "Cá Ngừ Đột Biến", "Nấu: Cung cấp 5% Sát thương", "Sải vây rộng, bơi cực nhanh."));

        // Cá Kiếm (Base Med: 1.0, RARE)
        register(new ItemData("fish_marlin_small", Rarity.UNCOMMON, Category.FISH, 0.5, Material.SALMON, "Cá Kiếm Nhỏ", null, "Cá vừa mới nở, bắt làm gì"));
        register(new ItemData("fish_marlin_med", Rarity.RARE, Category.FISH, 1.0, Material.SALMON, "Cá Kiếm", null, "Tiêu chuẩn của mọi con cá kiếm"));
        register(new ItemData("fish_marlin_large", Rarity.EPIC, Category.FISH, 1.5, Material.SALMON, "Cá Kiếm Lớn", null, "Sống đủ lâu, kích thước phát triển mạnh mẽ"));
        register(new ItemData("bfish_marlin_mutant", Rarity.MYTHIC, Category.FISH, 1.75, Material.SALMON, "Cá Kiếm Đột Biến", "Nấu: Cung cấp 10% Tỉ lệ xuyên giáp", "Sải vây rộng, bơi cực nhanh."));

        // Cá Mập (Base Med: 2.0, MYTHIC)
        register(new ItemData("fish_shark_small", Rarity.EPIC, Category.FISH, 1.5, Material.COD, "Cá Mập Con", null, "Nhà vô địch trong bụng mẹ"));
        register(new ItemData("fish_shark_med", Rarity.MYTHIC, Category.FISH, 2.0, Material.COD, "Cá Mập", null, "Sát thủ của đại dương."));
        register(new ItemData("bfish_shark_large", Rarity.LEGENDARY, Category.FISH, 2.5, Material.COD, "Cá Mập Trắng", "Nấu: Cung cấp 10% Sát thương", "Hung thần của đại dương sâu thẳm"));
        register(new ItemData("bfish_shark_mutant", Rarity.LEGENDARY, Category.FISH, 2.75, Material.COD, "Cá Mập Megalodon", "Nấu: Cung cấp 10% Sát thương | 5% Tỉ lệ hút máu", "Đáng ra phải nên tuyệt chủng rồi chứ nhỉ..."));

        // Cá Nước Ngọt (RIVER)
        // Cá Chép (Base Med: 0.5, COMMON)
        register(new ItemData("fish_carp_small", Rarity.COMMON, Category.FISH, 0.25, Material.SALMON, "Cá Chép Nhỏ", null, "Một sinh vật với sức sống dẻo dai vừa trào đời"));
        register(new ItemData("fish_carp_med", Rarity.COMMON, Category.FISH, 0.5, Material.SALMON, "Cá Chép Béo", null, "Cá chép sông, vảy óng ánh."));
        register(new ItemData("fish_carp_large", Rarity.UNCOMMON, Category.FISH, 1.0, Material.SALMON, "Cá Chép Lớn", null, "Sống lâu hơn nữa, biết đâu nó sẽ hóa rồng?"));
        register(new ItemData("bfish_carp_mutant", Rarity.RARE, Category.FISH, 1.25, Material.SALMON, "Cá Chép Đột Biến", "Nấu: Cung cấp 5% Tỉ lệ hồi chiêu", "Vảy vàng óng ánh màu sắc kì lạ."));

        // Cá Trê (Base Med: 0.75, UNCOMMON)
        register(new ItemData("fish_catfish_small", Rarity.COMMON, Category.FISH, 0.25, Material.COD, "Cá Trê Nhỏ", null, "Sống dưới bùn lầy."));
        register(new ItemData("fish_catfish_med", Rarity.UNCOMMON, Category.FISH, 0.75, Material.COD, "Cá Trê Bùn", null, "Sống dưới bùn lầy."));
        register(new ItemData("fish_catfish_large", Rarity.RARE, Category.FISH, 1.25, Material.COD, "Cá Trê Lớn", null, "Kích thước lớn chưa từng được ghi nhận."));
        register(new ItemData("bfish_catfish_mutant", Rarity.EPIC, Category.FISH, 1.5, Material.COD, "Cá Trê Đột Biến", "Nấu: Cung cấp 500 Máu tối đa", "Sống dưới bùn lầy ven sông và sắp thành tinh"));

        // Cá Tầm (Base Med: 1.0, RARE)
        register(new ItemData("fish_sturgeon_small", Rarity.UNCOMMON, Category.FISH, 0.5, Material.SALMON, "Cá Tầm Nhỏ", null, "Cá tầm con vừa trào đời..."));
        register(new ItemData("fish_sturgeon_med", Rarity.RARE, Category.FISH, 1.0, Material.SALMON, "Cá Tầm Thượng Hạng", null, "Sinh sống ở vùng nước chảy xiết."));
        register(new ItemData("fish_sturgeon_large", Rarity.EPIC, Category.FISH, 1.5, Material.SALMON, "Cá Tầm Lớn", null, "Có thể cung cấp trứng cá muối."));
        register(new ItemData("bfish_sturgeon_mutant", Rarity.MYTHIC, Category.FISH, 1.75, Material.SALMON, "Cá Tầm Đột Biến", "Nấu: Cung cấp 5% Tỉ lệ hồi chiêu", "Cá tầm đột biến có thể đẻ ra trứng cá màu vàng kim?"));

        // Cá Hang Động Ngầm (CAVE)
        // Cá Mù (Base Med: 0.5, UNCOMMON)
        register(new ItemData("fish_blind_tetra_small", Rarity.COMMON, Category.FISH, 0.25, Material.TROPICAL_FISH, "Cá Mù Nhỏ", null, "Sinh ra trong bóng tối"));
        register(new ItemData("fish_blind_tetra_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.TROPICAL_FISH, "Cá Mù Hang Động", null, "Sống trong bóng tối."));
        register(new ItemData("fish_blind_tetra_large", Rarity.RARE, Category.FISH, 1.0, Material.TROPICAL_FISH, "Cá Mù Lớn", null, "Sống trong bóng tối."));
        register(new ItemData("bfish_blind_tetra_mutant", Rarity.EPIC, Category.FISH, 1.25, Material.TROPICAL_FISH, "Cá Mù Đột Biến", "Nấu: Cung cấp 10% Tốc độ di chuyển", "Mắt đã thoái hóa hoàn toàn."));

        // Mực Phát Quang (Base Med: 1.5, EPIC)
        register(new ItemData("fish_glow_squid_small", Rarity.RARE, Category.FISH, 1.0, Material.GLOW_INK_SAC, "Mực Sáng Nhỏ", null, "Tỏa sáng leo lét."));
        register(new ItemData("fish_glow_squid_med", Rarity.EPIC, Category.FISH, 1.5, Material.GLOW_INK_SAC, "Mực Phát Quang", null, "Tỏa sáng leo lét."));
        register(new ItemData("fish_glow_squid_large", Rarity.MYTHIC, Category.FISH, 2.0, Material.GLOW_INK_SAC, "Mực Lớn", null, "Tỏa sáng leo lét."));
        register(new ItemData("bfish_glow_squid_mutant", Rarity.LEGENDARY, Category.FISH, 2.25, Material.GLOW_INK_SAC, "Mực Đột Biến", "Nấu: Cung cấp 15% Tỉ lệ rớt đồ", "Làm sao nó có thể phát triển to trong môi trường này được nhỉ?"));

        // Cá Lồng Đèn (Base Med: 2.0, MYTHIC)
        register(new ItemData("fish_abyss_angler_small", Rarity.EPIC, Category.FISH, 1.5, Material.PUFFERFISH, "Cá Lồng Đèn Con", null, "Có 1 cái đèn nhỏ ngay trước đầu nó"));
        register(new ItemData("fish_abyss_angler_med", Rarity.MYTHIC, Category.FISH, 2.0, Material.PUFFERFISH, "Cá Lồng Đèn", null, "Răng sắc nhọn, kết hợp với đèn để săn mồi."));
        register(new ItemData("fish_abyss_angler_large", Rarity.LEGENDARY, Category.FISH, 2.5, Material.PUFFERFISH, "Cá Lồng Đèn Lớn", null, "Răng sắc nhọn, kết hợp với đèn để săn mồi."));
        register(new ItemData("bfish_abyss_angler_mutant", Rarity.LEGENDARY, Category.FISH, 2.75, Material.PUFFERFISH, "Cá Lồng Đèn Đột Biến", "Nấu: Cung cấp 5% Tỉ lệ hồi phục", "Răng sắc nhọn, kết hợp với đèn để săn mồi."));

        // Cá Đầm Lầy (SWAMP)
        // Cá Chạch (Base Med: 0.5, COMMON)
        register(new ItemData("fish_mud_loach_small", Rarity.COMMON, Category.FISH, 0.1, Material.COD, "Cá Chạch Nhỏ", null, "Trơn tuột."));
        register(new ItemData("fish_mud_loach_med", Rarity.COMMON, Category.FISH, 0.5, Material.COD, "Cá Chạch Bùn", null, "Trơn tuột, Trơn tuột."));
        register(new ItemData("fish_mud_loach_large", Rarity.UNCOMMON, Category.FISH, 1.0, Material.COD, "Cá Chạch Lớn", null, "Trơn tuột, Trơn tuột và Trơn tuột."));
        register(new ItemData("bfish_mud_loach_mutant", Rarity.RARE, Category.FISH, 1.25, Material.COD, "Cá Chạch Đột Biến", "Nấu: Cung cấp 5% Tỉ lệ hồi phục", "Trơn tuột, Trơn tuột và Trơn tuột."));

        // Lươn Đầm Lầy (Base Med: 0.75, UNCOMMON)
        register(new ItemData("fish_swamp_eel_small", Rarity.COMMON, Category.FISH, 0.25, Material.SALMON, "Lươn Nhỏ", null, "Chả có bao nhiêu thịt"));
        register(new ItemData("fish_swamp_eel_med", Rarity.UNCOMMON, Category.FISH, 0.75, Material.SALMON, "Lươn Đầm Lầy", null, "Thịt dai ngọt."));
        register(new ItemData("fish_swamp_eel_large", Rarity.RARE, Category.FISH, 1.25, Material.SALMON, "Lươn Lớn", null, "Lươn này quá bự rồi..."));
        register(new ItemData("bfish_swamp_eel_mutant", Rarity.EPIC, Category.FISH, 1.5, Material.SALMON, "Lươn Đột Biến", "Nấu: Cung cấp 10% Tỉ lệ hồi phục", "Hình như nó có tận 2 đầu?..."));

        // Cóc Độc (Base Med: 1.5, EPIC)
        register(new ItemData("fish_toxic_toad_small", Rarity.RARE, Category.FISH, 1.0, Material.PUFFERFISH, "Cóc Con", null, "Cóc con bé bằng đầu ngón tay"));
        register(new ItemData("fish_toxic_toad_med", Rarity.EPIC, Category.FISH, 1.5, Material.PUFFERFISH, "Cóc Độc", null, "Ăn vào cực độc"));
        register(new ItemData("fish_toxic_toad_large", Rarity.MYTHIC, Category.FISH, 2.0, Material.PUFFERFISH, "Cóc Lớn", null, "Ăn vào cực độc"));
        register(new ItemData("bfish_toxic_toad_mutant", Rarity.LEGENDARY, Category.FISH, 2.25, Material.PUFFERFISH, "Cóc Chúa", null, "Ăn vào cực độc"));

        // Huyền Thoại (Toàn thế giới)
        register(new ItemData("fish_golden_koi_small", Rarity.EPIC, Category.FISH, 2.5, Material.TROPICAL_FISH, "Koi Con", null, "Phiên bản chưa trưởng thành của thủy quái."));
        register(new ItemData("bfish_golden_koi_med", Rarity.LEGENDARY, Category.FISH, 3.0, Material.TROPICAL_FISH, "Cá Koi Hoàng Kim", "Nấu: Cung cấp 1500 Máu tối đa", "Thủy quái truyền thuyết."));
        register(new ItemData("bfish_golden_koi_large", Rarity.LEGENDARY, Category.FISH, 3.5, Material.TROPICAL_FISH, "Cá Koi Lớn", "Nấu: Cung cấp 2000 Máu tối đa", "Thủy quái truyền thuyết."));
        register(new ItemData("bfish_golden_koi_mutant", Rarity.LEGENDARY, Category.FISH, 3.75, Material.TROPICAL_FISH, "Cá Koi Cụ", "Nấu: Cung cấp 3000 Máu tối đa", "Thủy quái truyền thuyết."));

        register(new ItemData("fish_time_weaver_small", Rarity.RARE, Category.FISH, 1.5, Material.TROPICAL_FISH, "Cá Dệt Nhỏ", null, "Cá dệt thời không nhưng mà kích thước quá nhỏ."));
        register(new ItemData("bfish_time_weaver_med", Rarity.MYTHIC, Category.FISH, 2.0, Material.TROPICAL_FISH, "Cá Dệt Thời Gian", "Nấu: Cung cấp 15% tỉ lệ hồi chiêu", "Bơi xuyên thời gian."));
        register(new ItemData("bfish_time_weaver_large", Rarity.LEGENDARY, Category.FISH, 2.5, Material.TROPICAL_FISH, "Cá Dệt Lớn", "Nấu: Cung cấp 15% tỉ lệ hồi chiêu", "Bơi xuyên thời gian."));
        register(new ItemData("bfish_time_weaver_mutant", Rarity.LEGENDARY, Category.FISH, 2.75, Material.TROPICAL_FISH, "Cá Dệt Thần Thánh", "Nấu: Cung cấp 20% tỉ lệ hồi chiêu", "Bơi xuyên thời gian."));

        register(new ItemData("fish_starfall_ray_small", Rarity.UNCOMMON, Category.FISH, 1.0, Material.PHANTOM_MEMBRANE, "Đuối Sao Nhỏ", null, "Cá Đuối Sao nhỏ xíu"));
        register(new ItemData("bfish_starfall_ray_med", Rarity.EPIC, Category.FISH, 1.5, Material.PHANTOM_MEMBRANE, "Cá Đuối Sao Băng", "Nấu: Cung cấp 30% tốc độ di chuyển", "Sức mạnh sao trời."));
        register(new ItemData("bfish_starfall_ray_large", Rarity.MYTHIC, Category.FISH, 2.0, Material.PHANTOM_MEMBRANE, "Đuối Sao Lớn", "Nấu: Cung cấp 30% tốc độ di chuyển", "Sức mạnh sao trời."));
        register(new ItemData("bfish_starfall_ray_mutant", Rarity.LEGENDARY, Category.FISH, 2.25, Material.PHANTOM_MEMBRANE, "Đuối Sao Cụ", "Nấu: Cung cấp 50% tốc độ di chuyển", "Sức mạnh sao trời."));

        // --- 3. NHÓM RAU QUẢ & NÔNG SẢN ĐỘT BIẾN ---
        // [CÀ RỐT] Gốc 0.5 -> 0.75 -> 1.0
        register(new ItemData("veg_carrot", Rarity.COMMON, Category.VEG, 0.5, Material.CARROT, "Cà Rốt Ngọt", null, "Tốt cho mắt."));
        register(new ItemData("veg_carrot_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.CARROT, "Cà Rốt Đột Biến", null, "To hơn cà rốt bình thường một chút."));
        register(new ItemData("veg_carrot_rare", Rarity.RARE, Category.VEG, 1.0, Material.CARROT, "Cà Rốt Khổng Lồ", null, "Củ cà rốt khổng lồ, chứa đầy dinh dưỡng."));
        // [CỦ DỀN] Gốc 0.5 -> 0.75 -> 1.0
        register(new ItemData("veg_beetroot", Rarity.COMMON, Category.VEG, 0.5, Material.BEETROOT, "Củ Dền Đỏ", null, "Dùng để nấu canh rất ngon."));
        register(new ItemData("veg_beetroot_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.BEETROOT, "Củ Dền Đột Biến", null, "Màu sắc đậm đà hơn bình thường."));
        register(new ItemData("veg_beetroot_rare", Rarity.RARE, Category.VEG, 1.0, Material.BEETROOT, "Củ Dền Khổng Lồ", null, "Cực kỳ mọng nước và giàu dưỡng chất."));
        // [BÍ NGÔ] Gốc 1.0 -> 1.5 -> 2.0
        register(new ItemData("veg_pumpkin", Rarity.COMMON, Category.VEG, 1.0, Material.PUMPKIN, "Bí Ngô Nguyên Quả", null, "Trái bí ngô to tròn."));
        register(new ItemData("veg_pumpkin_uncommon", Rarity.UNCOMMON, Category.VEG, 1.5, Material.PUMPKIN, "Bí Ngô Đột Biến", null, "Lớp vỏ rất cứng."));
        register(new ItemData("veg_pumpkin_rare", Rarity.RARE, Category.VEG, 2.0, Material.PUMPKIN, "Bí Ngô Khổng Lồ", null, "Nặng đến mức một người ôm không xuể."));
        // [DƯA HẤU] Gốc 0.5 -> 0.75 -> 1.0
        register(new ItemData("veg_melon", Rarity.COMMON, Category.VEG, 0.5, Material.MELON_SLICE, "Dưa Hấu Mọng Nước", "Dùng ngay: Hồi 2% HP và 4 Độ no", "Ngọt ngào và giải khát."));
        register(new ItemData("veg_melon_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.MELON_SLICE, "Dưa Hấu Đột Biến", "Dùng ngay: Hồi 3% HP và 6 Độ no", "Miếng dưa rất ngọt và ít hạt."));
        register(new ItemData("veg_melon_rare", Rarity.RARE, Category.VEG, 1.0, Material.MELON_SLICE, "Dưa Hấu Khổng Lồ", "Dùng ngay: Hồi 5% HP và 8 Độ no", "Đỏ au và mọng nước tột độ."));
        // [KHOAI TÂY] Gốc 0.5 -> 0.75 -> 1.0
        register(new ItemData("veg_potato", Rarity.COMMON, Category.VEG, 0.5, Material.POTATO, "Khoai Tây Vàng", "Cung cấp thêm 0.5 Điểm Tinh bột", "Củ khoai tây bùi béo."));
        register(new ItemData("veg_potato_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.POTATO, "Khoai Tây Đột Biến", "Cung cấp thêm 0.75 Điểm Tinh bột", "Củ khoai tây khá bự."));
        register(new ItemData("veg_potato_rare", Rarity.RARE, Category.VEG, 1.0, Material.POTATO, "Khoai Tây Khổng Lồ", "Cung cấp thêm 1.0 Điểm Tinh bột", "Ăn một củ là no cả ngày."));
        // [LÚA MÌ] Gốc 0.25 -> 0.5 -> 0.75
        register(new ItemData("veg_wheat", Rarity.COMMON, Category.VEG, 0.25, Material.WHEAT, "Bó Lúa Mì", "Cung cấp thêm 0.5 Điểm Tinh bột", "Nguyên liệu để xay bột."));
        register(new ItemData("veg_wheat_uncommon", Rarity.UNCOMMON, Category.VEG, 0.5, Material.WHEAT, "Bó Lúa Mì Đột Biến", "Cung cấp thêm 0.75 Điểm Tinh bột", "Hạt lúa rất mẩy."));
        register(new ItemData("veg_wheat_rare", Rarity.RARE, Category.VEG, 0.75, Material.WHEAT, "Bó Lúa Mì Vàng", "Cung cấp thêm 1.0 Điểm Tinh bột", "Tỏa ra hương thơm tự nhiên ngào ngạt."));
        // [NẤM HƯƠNG] Gốc 0.25 -> 0.5 -> 0.75
        register(new ItemData("veg_mushroom", Rarity.COMMON, Category.VEG, 0.25, Material.BROWN_MUSHROOM, "Nấm Hương", null, "Thơm lừng khi xào."));
        register(new ItemData("veg_mushroom_uncommon", Rarity.UNCOMMON, Category.VEG, 0.5, Material.BROWN_MUSHROOM, "Nấm Hương Đột Biến", null, "Mũ nấm rất to."));
        register(new ItemData("veg_mushroom_rare", Rarity.RARE, Category.VEG, 0.75, Material.BROWN_MUSHROOM, "Nấm Hương Khổng Lồ", null, "Chứa hàm lượng dinh dưỡng cao."));
        // [NẤM ĐỎ] Gốc 0.25 -> 0.5 -> 0.75
        register(new ItemData("veg_red_mushroom", Rarity.COMMON, Category.VEG, 0.25, Material.RED_MUSHROOM, "Nấm Đỏ Tươi", null, "Màu sắc sặc sỡ nhưng an toàn."));
        register(new ItemData("veg_red_mushroom_uncommon", Rarity.UNCOMMON, Category.VEG, 0.5, Material.RED_MUSHROOM, "Nấm Đỏ Đột Biến", null, "Cánh nấm lốm đốm phát sáng."));
        register(new ItemData("veg_red_mushroom_rare", Rarity.RARE, Category.VEG, 0.75, Material.RED_MUSHROOM, "Nấm Đỏ Khổng Lồ", null, "Một cây nấm khổng lồ đầy dưỡng chất."));
        // [QUẢ MỌNG / SWEET BERRIES] Gốc 0.25 -> 0.5 -> 0.75
        register(new ItemData("veg_berries", Rarity.COMMON, Category.VEG, 0.25, Material.SWEET_BERRIES, "Quả Mọng Rừng", null, "Vị chua chua ngọt ngọt."));
        register(new ItemData("veg_berries_uncommon", Rarity.UNCOMMON, Category.VEG, 0.5, Material.SWEET_BERRIES, "Quả Mọng Đột Biến", null, "Lớn hơn và ngọt hơn bình thường."));
        register(new ItemData("veg_berries_rare", Rarity.RARE, Category.VEG, 0.75, Material.SWEET_BERRIES, "Quả Mọng Khổng Lồ", null, "Căng mọng và chứa rất nhiều nước."));
        // [QUẢ MỌNG SÁNG / GLOW BERRIES] Gốc 0.25 -> 0.5 -> 0.75
        register(new ItemData("veg_glow_berries", Rarity.COMMON, Category.VEG, 0.25, Material.GLOW_BERRIES, "Quả Mọng Sáng", null, "Trái cây mọc trong hang động tối."));
        register(new ItemData("veg_glow_berries_uncommon", Rarity.UNCOMMON, Category.VEG, 0.5, Material.GLOW_BERRIES, "Quả Mọng Sáng Đột Biến", null, "Phát ra ánh sáng ấm áp."));
        register(new ItemData("veg_glow_berries_rare", Rarity.RARE, Category.VEG, 0.75, Material.GLOW_BERRIES, "Quả Mọng Sáng Khổng Lồ", null, "Tỏa sáng rực rỡ như một chiếc đèn lồng."));
        // [RONG BIỂN] Gốc 0.25 -> 0.5 -> 0.75
        register(new ItemData("veg_kelp_fresh", Rarity.COMMON, Category.VEG, 0.25, Material.KELP, "Rong Biển Tươi", null, "Vớt từ dưới đáy biển lên."));
        register(new ItemData("veg_kelp_fresh_uncommon", Rarity.UNCOMMON, Category.VEG, 0.5, Material.KELP, "Rong Biển Dày", null, "Bản lá rất to và giòn."));
        register(new ItemData("veg_kelp_fresh_rare", Rarity.RARE, Category.VEG, 0.75, Material.KELP, "Rong Biển Khổng Lồ", null, "Rong biển thượng hạng từ biển sâu."));
        register(new ItemData("veg_kelp_dried", Rarity.COMMON, Category.VEG, 0.25, Material.DRIED_KELP, "Rong Biển Khô", "Dùng ngay: Tăng 15% Tốc chạy trong 5 giây", "Đã được sấy khô để bảo quản."));
        // [DƯA CHUỘT / CUCUMBER] Gốc 0.5 -> 0.75 -> 1.0
        register(new ItemData("veg_cucumber", Rarity.COMMON, Category.VEG, 0.5, Material.SEA_PICKLE, "Dưa Chuột Giòn", null, "Thanh mát và rất giòn."));
        register(new ItemData("veg_cucumber_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.SEA_PICKLE, "Dưa Chuột Đột Biến", null, "Quả dưa dài và đặc ruột."));
        register(new ItemData("veg_cucumber_rare", Rarity.RARE, Category.VEG, 1.0, Material.SEA_PICKLE, "Dưa Chuột Khổng Lồ", null, "To như một quả bí! Giải khát cực tốt."));
        register(new ItemData("veg_pickled_cucumber", Rarity.UNCOMMON, Category.VEG, 0.75, Material.SEA_PICKLE, "Dưa Chuột Muối", "Có thể giúp đánh lừa vị giác", "Chua chua giòn giòn, ăn rất cuốn."));
        // [CACAO] Gốc 0.5 -> 0.75 -> 1.0 (Xếp vào nhóm Củ Quả/Rau)
        register(new ItemData("veg_cocoa", Rarity.COMMON, Category.VEG, 0.5, Material.COCOA_BEANS, "Hạt Cacao", null, "Nguyên liệu tuyệt vời để tạo độ béo và ngọt."));
        register(new ItemData("veg_cocoa_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.COCOA_BEANS, "Hạt Cacao Đột Biến", null, "Hạt to, thơm nức mùi chocolate."));
        register(new ItemData("veg_cocoa_rare", Rarity.RARE, Category.VEG, 1.0, Material.COCOA_BEANS, "Hạt Cacao Khổng Lồ", null, "Chứa hàm lượng bơ cacao cực cao."));
        // [TÁO] Không có hạt giống, nên táo giữ nguyên 1 loại
        register(new ItemData("veg_apple", Rarity.COMMON, Category.VEG, 0.5, Material.APPLE, "Táo Đỏ", "Dùng ngay: Hồi 2% HP và 4 Độ no", "Táo rừng chín mọng."));

        // --- 4. NHÓM TINH BỘT ---
        register(new ItemData("starch_bread", Rarity.COMMON, Category.STARCH, 1.0, Material.BREAD, "Ổ Bánh Mì Nướng", "Dùng ngay: Hồi 8 Độ no", "Thơm mùi lúa mạch."));
        register(new ItemData("starch_flour", Rarity.COMMON, Category.STARCH, 0.25, Material.SUGAR, "Bột Mì Thượng Hạng", null, "Xay nhuyễn từ lúa mì ngon nhất."));

        // --- 5. NHÓM GIA VỊ ---
        register(new ItemData("spice_salt", Rarity.COMMON, Category.SPICE, 0.5, Material.WHITE_DYE, "Muối Biển", null, "Gia vị không thể thiếu."));
        register(new ItemData("spice_pepper", Rarity.COMMON, Category.SPICE, 0.5, Material.GUNPOWDER, "Tiêu Đen", null, "Cay nồng."));
        register(new ItemData("spice_chili", Rarity.COMMON, Category.SPICE, 0.5, Material.NETHER_WART, "Bột Ớt Hiểm", null, "Cực kỳ cay."));
        register(new ItemData("spice_garlic", Rarity.COMMON, Category.SPICE, 0.5, Material.PUMPKIN_SEEDS, "Tỏi Bóc Vỏ", null, "Dùng để phi thơm."));
        register(new ItemData("spice_sugar", Rarity.COMMON, Category.SPICE, 0.5, Material.SUGAR, "Đường Tinh Luyện", null, "Ngọt ngào."));
        register(new ItemData("spice_honey", Rarity.UNCOMMON, Category.SPICE, 1.0, Material.HONEY_BOTTLE, "Mật Ong Rừng", "Dùng ngay: Hồi phục 5% Máu tối đa", "Mật đặc sánh và cực kỳ ngọt ngào."));
        register(new ItemData("spice_sour", Rarity.COMMON, Category.SPICE, 0.5, Material.POTION, "Giấm Chua", null, "Tạo vị chua thanh."));
        register(new ItemData("spice_all_in_one", Rarity.EPIC, Category.SPICE, 0.5, Material.BROWN_DYE, "Gia Vị Tổng Hợp", "Cung cấp 0.5 điểm cho toàn bộ gia vị", "Sự kết hợp hoàn hảo của mặn, ngọt, chua, cay."));

        // --- 6. NHÓM CHẤT BÉO ---
        register(new ItemData("fat_egg", Rarity.COMMON, Category.FAT, 0.5, Material.EGG, "Trứng Gà Tươi", null, "Một quả trứng vừa mới đẻ."));
        register(new ItemData("fat_sniffer", Rarity.RARE, Category.FAT, 0.75, Material.SNIFFER_EGG, "Trứng Cổ Đại", null, "Toát ra một thứ hào quang cổ đại."));
        register(new ItemData("fat_milk", Rarity.UNCOMMON, Category.FAT, 1.0, Material.MILK_BUCKET, "Sữa Bò Béo Ngậy", "Dùng ngay: Xóa bỏ 1 Trạng thái xấu ngẫu nhiên", "Sữa vắt từ bò hoang dã, rất thơm."));
        register(new ItemData("fat_lard", Rarity.UNCOMMON, Category.FAT, 0.5, Material.SNOWBALL, "Mỡ Lợn", null, "Dùng để chiên xào cực kỳ thơm."));

        // --- 7. NHÓM QUÁI VẬT ---
        register(new ItemData("monster_flesh", Rarity.COMMON, Category.MONSTER, 1.0, Material.ROTTEN_FLESH, "Thịt Thối Rữa", "Nguy cơ làm hỏng món ăn rất cao!", "Thịt của quái vật đột biến, không thể ăn được."));

        // --- 8. NHÓM MA THUẬT ---
        register(new ItemData("magic_golden_carrot", Rarity.RARE, Category.MAGIC, 0.75, Material.GOLDEN_CARROT, "Cà Rốt Ma Thuật", "Cung cấp hiệu ứng Mắt Mèo", "Trồng bằng phép thuật."));
        register(new ItemData("magic_blaze_powder", Rarity.RARE, Category.MAGIC, 0.75, Material.BLAZE_POWDER, "Mảnh Quỷ Lửa", "Cung cấp hiệu ứng Kháng Lửa", "Nóng rực trên tay."));
        register(new ItemData("magic_healing_gland", Rarity.RARE, Category.MAGIC, 0.75, Material.FERMENTED_SPIDER_EYE, "Tuyến Tơ Nhện", "Dùng ngay: Hồi 8% HP | Nấu: Tăng 10% Tỉ lệ hồi phục", "Tơ mỏng nhẹ, dính và có tác dụng hồi phục cao."));
        register(new ItemData("magic_pufferfish", Rarity.RARE, Category.MAGIC, 0.75, Material.PUFFERFISH, "Cá Nóc Kịch Độc", "Cung cấp hiệu ứng Thở Dưới Nước", "Phải biết cách sơ chế mới dùng được."));
        register(new ItemData("magic_spider_eye", Rarity.UNCOMMON, Category.MAGIC, 0.75, Material.SPIDER_EYE, "Mắt Nhện Tinh Anh", "Dùng để hạ độc thức ăn.", "Trông khá tởm, nhưng chứa đầy chất độc."));
        register(new ItemData("magic_bat_wing", Rarity.RARE, Category.MAGIC, 0.75, Material.POTION, "Huyết Tinh", "Cung cấp thêm 10% tỉ lệ Hút Máu", "Máu của sinh vật bóng tối."));
        register(new ItemData("magic_glowstone", Rarity.RARE, Category.MAGIC, 0.75, Material.GLOWSTONE_DUST, "Bột Tiên", "Cung cấp 10% tỉ lệ Chí Mạng", "Bột phát sáng lấp lánh."));
        register(new ItemData("magic_goblin_ear", Rarity.RARE, Category.MAGIC, 0.75, Material.TURTLE_SCUTE, "Tai Goblin", "Cung cấp 10% tỉ lệ Rớt Đồ", "Một chiếc tai còn rỏ máu của Goblin."));
        register(new ItemData("magic_toxic_blood", Rarity.RARE, Category.MAGIC, 0.75, Material.SLIME_BALL, "Máu Kịch Độc", "Dùng để hạ độc thức ăn.", "Thứ chất lỏng đặc sệt bốc mùi hôi thối."));

        // --- 8.1. NÔNG SẢN MA THUẬT (FARMER EXCLUSIVE) ---
        register(new ItemData("magic_golden_carrot", Rarity.EPIC, Category.MAGIC, 0.75, Material.GOLDEN_CARROT, "Cà Rốt Vàng Chói", "Nấu: Hiệu ứng Nhìn Trong Đêm", "Trồng bằng phép thuật."));
        register(new ItemData("magic_flame_beetroot", Rarity.EPIC, Category.MAGIC, 0.75, Material.MAGMA_CREAM, "Củ Dền Hỏa Ngục", "Dùng ngay: Hồi 10% HP, Kháng lửa 1 phút | Nấu: Tăng 15% Tốc chạy & Kháng Lửa", "Cầm trên tay cũng thấy bỏng rát."));
        register(new ItemData("magic_golden_melon", Rarity.EPIC, Category.MAGIC, 0.75, Material.GLISTERING_MELON_SLICE, "Dưa Hấu Hoàng Kim", "Dùng ngay: Hồi 15% HP | Nấu: Tăng 2000 Máu tối đa", "Lấp lánh ánh kim vô giá."));
        register(new ItemData("magic_glowing_mushroom", Rarity.EPIC, Category.MAGIC, 0.75, Material.GLOW_INK_SAC, "Nấm Phát Quang", "Nấu: Tăng 5% Tỉ Lệ Chí Mạng & Phát sáng", "Mọc lên từ vùng đất giàu ma lực."));
        register(new ItemData("magic_star_wheat", Rarity.EPIC, Category.MAGIC, 0.75, Material.SUNFLOWER, "Lúa Mì Tinh Tú", "Nấu: Tăng 10% Tỉ Lệ Rớt Đồ", "Hấp thụ ánh sáng của các vì sao."));
        register(new ItemData("magic_soul_sweet_berry", Rarity.EPIC, Category.MAGIC, 0.75, Material.SWEET_BERRIES, "Quả Mọng Đoạt Hồn", "Nấu: Tăng 5% Tỉ Lệ Hút Máu", "Có màu đỏ thẫm đến rợn người."));
        register(new ItemData("magic_crystal_kelp", Rarity.EPIC, Category.MAGIC, 0.75, Material.PRISMARINE_CRYSTALS, "Tảo Pha Lê", "Dùng ngay: Thở dưới nước 3 phút | Nấu: Tăng 10% Xuyên Giáp & Thở Dưới Nước", "Sống ở vùng biển sâu ma thuật."));
        register(new ItemData("magic_dragon_cocoa", Rarity.EPIC, Category.MAGIC, 0.75, Material.DRAGON_BREATH, "Cacao Thanh Long", "Nấu: Tăng 10% Sát Thương", "Hạt giống được ấp trong vảy rồng."));
        register(new ItemData("magic_mana_potato", Rarity.EPIC, Category.MAGIC, 0.75, Material.POISONOUS_POTATO, "Khoai Tây Ma Năng", "Dùng ngay: Hồi 20% HP nhưng gây Mù | Nấu: Giảm 5% Hồi Chiêu Kỹ Năng", "Chứa nguồn ma lực chưa tinh chế."));
        register(new ItemData("magic_chaos_glow_berry", Rarity.EPIC, Category.MAGIC, 0.75, Material.CHORUS_FRUIT, "Dây Leo Hỗn Độn", "Dùng ngay: Hồi 10% HP, Tàng hình 30s | Nấu: Tăng 10% Tốc Độ Đánh", "Thay đổi không gian xung quanh nó."));

        // --- 9. NHÓM KHÁC ---
        register(new ItemData("misc_leather", Rarity.COMMON, Category.MISC, 0.0, Material.LEATHER, "Da Thuộc", null, "Nguyên liệu may mặc cơ bản."));
        register(new ItemData("misc_feather", Rarity.COMMON, Category.MISC, 0.0, Material.FEATHER, "Lông Vũ Cơ Bản", null, "Nhẹ nhàng và mềm mại."));
        register(new ItemData("misc_electric_feather", Rarity.RARE, Category.MISC, 0.0, Material.FEATHER, "Lông Vũ Nhiễm Điện", null, "Vẫn còn phát ra những tia lửa lách tách..."));
        register(new ItemData("misc_weird_leather", Rarity.UNCOMMON, Category.MISC, 0.0, Material.RABBIT_HIDE, "Da Thuộc Kì Lạ", null, "Bốc mùi thối rữa và rỉ dịch xanh."));
        register(new ItemData("misc_pollen", Rarity.COMMON, Category.MISC, 0.0, Material.YELLOW_DYE, "Phấn Hoa Tự Nhiên", null, "Mùi thơm dịu nhẹ, dùng làm dược liệu."));
        register(new ItemData("misc_poison_stinger", Rarity.UNCOMMON, Category.MISC, 0.0, Material.ARROW, "Ngòi Ong Độc", null, "Cực kỳ sắc bén, cẩn thận kẻo đứt tay."));
        register(new ItemData("misc_armor_shard", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Mảnh Giáp Vụn", null, "Có thể nung chảy để tái chế tại Lò Rèn."));
        register(new ItemData("misc_soft_string", Rarity.COMMON, Category.MISC, 0.0, Material.STRING, "Sợi Tơ Mềm", null, "Một cuộn tơ tằm mềm mại."));
        register(new ItemData("misc_steel_string", Rarity.UNCOMMON, Category.MISC, 0.0, Material.STRING, "Sợi Tơ Thép", null, "Cứng cáp lạ thường, cực kỳ bền bỉ."));
        register(new ItemData("misc_wolf_fang", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Nanh Sói", null, "Một chiếc nanh vô cùng sắc nhọn."));
        register(new ItemData("misc_leather_pouch_piece", Rarity.COMMON, Category.MISC, 0.0, Material.ARMADILLO_SCUTE, "Mảnh Túi Da Đạo Tặc", null, "Một mảnh da cắt ra từ túi của bọn Đạo Tặc."));
        register(new ItemData("misc_dark_wood", Rarity.UNCOMMON, Category.MISC, 0.0, Material.DARK_OAK_LOG, "Gỗ Quý Rừng Sâu", null, "Một loại gỗ đặc ruột tỏa ra mùi thơm hắc.", "Được Sơn Tặc đốn hạ sâu trong rừng già."));
        register(new ItemData("misc_feral_sinew", Rarity.UNCOMMON, Category.MISC, 0.0, Material.STRING, "Gân Thú Hoang", null, "Sợi gân vô cùng dẻo dai và đàn hồi.", "Chuyên dùng để kéo căng những cây cung", "săn mồi chết chóc của Sơn Tặc Cung Thủ."));
        register(new ItemData("misc_sand_flea_shell", Rarity.COMMON, Category.MISC, 0.0, Material.PHANTOM_MEMBRANE, "Vỏ Rận Cát", null, "Lớp vỏ mỏng dai, thô ráp bám đầy bụi cát Sa mạc."));
        register(new ItemData("misc_pure_sand", Rarity.RARE, Category.MISC, 0.0, Material.SUGAR, "Cát Tinh Khiết", null, "Những hạt cát lấp lánh phản chiếu ánh sáng dịu nhẹ,", "ẩn chứa bên trong một lượng mạt ma thuật thuần khiết."));
        register(new ItemData("misc_scorpion_tail", Rarity.COMMON, Category.MISC, 0.0, Material.PRISMARINE_SHARD, "Đuôi Bò Cạp", null, "Chiếc đuôi cong vút mang chiếc móc sắc lẻm của bầy bò cạp Sa mạc."));
        register(new ItemData("misc_scorpion_venom", Rarity.RARE, Category.MISC, 0.0, Material.DRAGON_BREATH, "Nọc Độc Bò Cạp", null, "Giọt tinh chất độc tố đậm đặc màu tím sẫm chiết xuất từ tuyến độc.", "Chuyên dùng để tinh luyện vũ khí ám sát cấp cao."));
        register(new ItemData("misc_sand_wolf_bone", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BONE, "Xương Sói Cát", null, "Khúc xương khô trắng của loài sói hoang mạc,", "cực kỳ cứng cáp do hấp thụ khoáng chất từ địa hình cát gắt gao."));
        register(new ItemData("misc_sand_wolf_hide", Rarity.RARE, Category.MISC, 0.0, Material.LEATHER, "Da Sói Cát", null, "Tấm da sói dày dặn và chống chịu nhiệt độ cao cực tốt,", "thường được giới thợ săn lùng sục ráo riết để may trang bị chống nóng."));
        register(new ItemData("misc_sand_wolf_fang", Rarity.RARE, Category.MISC, 0.0, Material.IRON_NUGGET, "Nanh Sói Cát Cổ Đại", null, "Chiếc nanh sói sắc bén tột độ mang theo sát khí,", "chứa đựng sức mạnh bầy đàn hung tợn của thủ lĩnh hoang mạc sa mạc."));
        register(new ItemData("misc_polar_bear_hide", Rarity.COMMON, Category.MISC, 0.0, Material.LEATHER, "Da Gấu Tuyết", null, "Tấm da lông gấu trắng muốt, cực kỳ dày và có khả năng giữ ấm cơ thể tuyệt vời."));
        register(new ItemData("misc_rabbit_tail_fur", Rarity.COMMON, Category.MISC, 0.0, Material.SNOWBALL, "Lông Đuôi Thỏ", null, "Một chùm lông đuôi thỏ trắng muốt, vô cùng mềm mại và giữ nhiệt tốt."));
        register(new ItemData("misc_frozen_bone", Rarity.COMMON, Category.MISC, 0.0, Material.BONE, "Xương Băng Giá", null, "Một khúc xương lạnh buốt, dường như không bao giờ tan chảy."));
        register(new ItemData("misc_frozen_soul", Rarity.RARE, Category.MISC, 0.0, Material.LIGHT_BLUE_DYE, "Linh Hồn Băng Giá", null, "Mảnh linh hồn lạnh toát của các cựu pháp sư,", "vẫn không ngừng tỏa ra mạt sương giá lập lòe sau khi chết."));
        register(new ItemData("misc_wooden_staff_piece", Rarity.COMMON, Category.MISC, 0.0, Material.COCOA_BEANS, "Mảnh Trượng Vỡ", null, "Một mảnh vỡ nhỏ từ trượng của Goblin Shaman, vẫn còn chứa ma lực bên trong."));
        register(new ItemData("misc_goblin_king_crown", Rarity.RARE, Category.MISC, 0.0, Material.GOLDEN_HELMET, "Vương Miện Goblin", null, "Chiếc vương miện bằng vàng ròng đã hoen ố", "Biểu tượng quyền lực tối cao của loài Goblin."));
        register(new ItemData("misc_loincloth", Rarity.COMMON, Category.MISC, 0.0, Material.RABBIT_HIDE, "Mảnh Vải Khố", null, "Một mảnh vải rách rưới bốc mùi hôi thối đặc trưng", "Trang phục quen thuộc của bọn Hobgoblin."));
        register(new ItemData("misc_hardwood_stick", Rarity.RARE, Category.MISC, 0.0, Material.STICK, "Khúc Gỗ Cứng", null, "Phần cán cực kỳ đặc chắc bị gãy văng ra từ những", "cú nện búa uy lực của Hobgoblin Tiên Phong."));
        register(new ItemData("misc_thorn_bush", Rarity.COMMON, Category.MISC, 0.0, Material.DEAD_BUSH, "Gai Nhọn Dính Dịch", null, "Một nhánh gai sắc nhọn bứt ra từ thân của Slime Gai", "Vẫn còn vương chút dịch nhờn xanh lá."));
        register(new ItemData("misc_slime_gel", Rarity.COMMON, Category.MISC, 0.0, Material.SLIME_BALL, "Chất Nhầy", null, "Một chút dịch cơ thể còn sót lại của Slime", "Có khả năng kết dính tốt dùng cho chế tạo vũ khí."));
        register(new ItemData("misc_pure_water", Rarity.RARE, Category.MISC, 0.0, Material.HEART_OF_THE_SEA, "Lõi Nước Trong", null, "Kết tinh ma hạch từ hạch tâm có Slime Nước", "Giá trị sử dụng cao trong việc nâng cao sức mạnh ma pháp."));
        register(new ItemData("misc_poison_slime_extract", Rarity.COMMON, Category.MISC, 0.0, Material.TURTLE_SCUTE, "Dịch Độc", null, "Chất lỏng màu xanh lục bốc mùi hăng hắc.", "Có tính axit nhẹ, cẩn thận đừng để dính vào vết thương!"));
        register(new ItemData("misc_poison_slime_core", Rarity.RARE, Category.MISC, 0.0, Material.ENDER_PEARL, "Hạch Nhân Độc Tố", null, "Lõi năng lượng cô đặc từ Slime Độc Biến Dị.", "Nhịp đập của nó phát ra những luồng khí độc lờ mờ."));
        register(new ItemData("misc_burning_gel", Rarity.COMMON, Category.MISC, 0.0, Material.RED_DYE, "Dịch Nóng Chảy", null, "Chất dịch đặc sệt, nóng chảy tựa như dung nham", "Thường dùng để làm chất dẫn cháy và tạo lửa."));
        register(new ItemData("misc_fire_slime_core", Rarity.RARE, Category.MISC, 0.0, Material.MAGMA_CREAM, "Lõi Phát Nhiệt", null, "Kết tinh ma hạch từ hạch tâm có Slime Lửa", "Luôn tỏa nhiệt, có thể cảm nhận hơi nóng khi cầm."));
        register(new ItemData("misc_pointy_rock", Rarity.COMMON, Category.MISC, 0.0, Material.POINTED_DRIPSTONE, "Gai Đá", null, "Một mảnh gai đá nhọn rơi ra từ Slime Đá", "Không quá sắc bén nhưng vẫn đủ nhọn để gây sát thương."));
        register(new ItemData("misc_shining_stone", Rarity.EPIC, Category.MISC, 0.0, Material.EMERALD, "Đá Sắc Màu", null, "Loại đá quý hiếm, đầy màu sắc và có ma lực tuôn trào", "Do Slime tiêu thụ các loại đá quý mà kết tinh thành."));
        register(new ItemData("misc_explosive_venom", Rarity.UNCOMMON, Category.MISC, 0.0, Material.GUNPOWDER, "Nọc Nổ", null, "Phần nọc độc chứa trong bụng của Nhện Nổ", "Hoạt tính mạnh mẽ đủ sức nổ tung tảng đá lớn."));
        register(new ItemData("misc_venomous_fang", Rarity.EPIC, Category.MISC, 0.0, Material.GOAT_HORN, "Nanh Nhện Chúa", null, "Một đoạn nanh dài, chứa độc tố chết người", "Có thể chế tạo thành một vũ khí ám sát tuyệt vời."));
        register(new ItemData("misc_spiderqueen_scale", Rarity.RARE, Category.MISC, 0.0, Material.FLINT, "Vảy Cứng", null, "Lớp vảy cứng cáp tựa như kim cương, đen tuyền", "Có ưu điểm giúp tăng thêm sức kháng vật lý cao."));

        register(new ItemData("misc_blacksmith_scrap", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Mạt Thợ Rèn", null, "Những mảnh vụn ma thuật xót lại khi đập đồ thất bại.", "Tích lũy đủ số lượng, chúng mang sức mạnh", "thần bí giúp đột phá trang bị thành công."));
        register(new ItemData("misc_job_change_card", Rarity.EPIC, Category.MISC, 0.0, Material.NAME_TAG, "Thẻ Học Nghề", null, "Giấy phép đặc biệt từ Hiệp hội Nghề nghiệp.", "Sẽ tự động tiêu hao khi bạn chọn đổi nghề."));
        register(new ItemData("misc_protection_charm", Rarity.EPIC, Category.MISC, 0.0, Material.PAPER, "Bùa Bảo Vệ Trang Bị", null, "Tự động tiêu hao khi rèn thất bại", "để ngăn chặn việc rớt 1 cấp độ."));
        register(new ItemData("misc_lucky_hammer", Rarity.RARE, Category.MISC, 0.0, Material.GOLDEN_AXE, "Búa Rèn May Mắn", null, "Để trong túi đồ, tự động tiêu hao", "để tăng thêm tỉ lệ thành công khi rèn."));

        // --- 10. TÚI GACHA ---
        register(new ItemData("bag_weapon_common", Rarity.COMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Thường", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Common.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_uncommon", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Hiếm", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Uncommon.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_rare", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Tuyệt Đỉnh", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Rare.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_epic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Sử Thi", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Epic.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_mythical", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Thần Thoại", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Mythical.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_heroic", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Anh Hùng", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Heroic.", "Cấp độ vũ khí bằng với cấp của bạn."));

        register(new ItemData("bag_armor_common", Rarity.COMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Thường", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Common.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_uncommon", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Hiếm", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Uncommon.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_rare", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Tuyệt Đỉnh", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Rare.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_epic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Sử Thi", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Epic.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_mythical", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Thần Thoại", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Mythical.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_heroic", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Anh Hùng", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Heroic.", "Cấp độ giáp bằng với cấp của bạn."));

        // [MỚI]: TÚI TRANG SỨC (TRINKETS)
        register(new ItemData("bag_trinket_common", Rarity.COMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Trang Sức Thường", null, "Chuột phải để mở nhận 1 Trang Sức Phẩm chất Common.", "Cấp độ bằng với cấp của bạn."));
        register(new ItemData("bag_trinket_uncommon", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Trang Sức Hiếm", null, "Chuột phải để mở nhận 1 Trang Sức Phẩm chất Uncommon.", "Cấp độ bằng với cấp của bạn."));
        register(new ItemData("bag_trinket_rare", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Trang Sức Tuyệt Đỉnh", null, "Chuột phải để mở nhận 1 Trang Sức Phẩm chất Rare.", "Cấp độ bằng với cấp của bạn."));
        register(new ItemData("bag_trinket_epic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Trang Sức Sử Thi", null, "Chuột phải để mở nhận 1 Trang Sức Phẩm chất Epic.", "Cấp độ bằng với cấp của bạn."));
        register(new ItemData("bag_trinket_mythical", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Trang Sức Thần Thoại", null, "Chuột phải để mở nhận 1 Trang Sức Phẩm chất Mythical.", "Cấp độ bằng với cấp của bạn."));
        register(new ItemData("bag_trinket_heroic", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Trang Sức Anh Hùng", null, "Chuột phải để mở nhận 1 Trang Sức Phẩm chất Heroic.", "Cấp độ bằng với cấp của bạn."));

        // --- 11. TINH THỂ ---
        register(new ItemData("crystal_fire", Rarity.MYTHIC, Category.MISC, 0.0, Material.BLAZE_POWDER, "Ngọn Lửa Khởi Nguyên", "Thức tỉnh Nguyên Tố Lửa", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_water", Rarity.MYTHIC, Category.MISC, 0.0, Material.HEART_OF_THE_SEA, "Nước Mắt Nữ Thần", "Thức tỉnh Nguyên Tố Nước", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_nature", Rarity.MYTHIC, Category.MISC, 0.0, Material.OAK_SAPLING, "Mầm Cây Thế Giới", "Thức tỉnh Nguyên Tố Tự Nhiên", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_wind", Rarity.MYTHIC, Category.MISC, 0.0, Material.WIND_CHARGE, "Hơi Tàn Của Tinh Linh Gió", "Thức tỉnh Nguyên Tố Gió", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_light", Rarity.MYTHIC, Category.MISC, 0.0, Material.NETHER_STAR, "Hào Quang Ánh Sáng", "Thức tỉnh Nguyên Tố Ánh Sáng", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_dark", Rarity.MYTHIC, Category.MISC, 0.0, Material.ECHO_SHARD, "Tinh Thể Hỗn Độn", "Thức tỉnh Nguyên Tố Bóng Tối", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));

        // --- 12. ĐẦU BẾP & TINH TÚY ẨM THỰC ---
        register(new ItemData("misc_culinary_essence", Rarity.EPIC, Category.MISC, 0.0, Material.GLOWSTONE_DUST, "Tinh Túy Ẩm Thực", null, "Kết tinh từ những món ăn tuyệt hảo.", "Được giới sành ăn săn lùng gắt gao."));
        register(new ItemData("potion_digestion", Rarity.RARE, Category.MISC, 0.0, Material.POTION, "Thuốc Giải Ngấy", "Dùng ngay: Giảm 50% lượng điểm Tuyệt thực", "Công thức bí truyền của Hội Đầu Bếp."));
        register(new ItemData("potion_anti_anorexia", Rarity.EPIC, Category.MISC, 0.0, Material.DRAGON_BREATH, "Thuốc Trị Tuyệt Thực", "Dùng ngay: Chữa khỏi hoàn toàn chứng biếng ăn", "Uống vào là thèm ăn ngay!"));
        register(new ItemData("misc_lunch_bag", Rarity.RARE, Category.MISC, 0.0, Material.PIGLIN_BANNER_PATTERN, "Túi Đồ Ăn Dự Phòng", "Dùng ngay: Mở ra 3 bát súp ngẫu nhiên", "Chuẩn bị sẵn cho những chuyến đi dài."));
        register(new ItemData("misc_preservative_powder", Rarity.MYTHIC, Category.MISC, 0.0, Material.GUNPOWDER, "Bột Bảo Quản Thần Kỳ", "Chặn 100% nguy cơ hỏng của món ăn", "Một loại bột sáng lấp lánh..."));

        // --- 13. NÔNG CỤ & HẠT GIỐNG (FARMING SYSTEM) ---
        // Nông cụ chuyên dụng
        register(new ItemData("tool_harvest_hoe", Rarity.UNCOMMON, Category.TOOL, 0.0, Material.IRON_HOE, "Cuốc Thu Hoạch", "Click phải để thu hoạch nông sản", "Nông cụ chuyên dụng giúp thu hồi nông sản", "và hạt giống mà không làm hỏng rễ cây."));
        // Hạt giống các loại
        register(new ItemData("seed_carrot", Rarity.COMMON, Category.VEG, 0.0, Material.CARROT, "Hạt Giống Cà Rốt", null, "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_potato", Rarity.COMMON, Category.VEG, 0.0, Material.POTATO, "Mầm Khoai Tây", null, "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_beetroot", Rarity.COMMON, Category.VEG, 0.0, Material.BEETROOT_SEEDS, "Hạt Giống Củ Dền", null, "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_wheat", Rarity.COMMON, Category.VEG, 0.0, Material.WHEAT_SEEDS, "Hạt Giống Lúa Mì", null, "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_pumpkin", Rarity.COMMON, Category.VEG, 0.0, Material.PUMPKIN_SEEDS, "Hạt Giống Bí Ngô", null, "Trồng trên đất cày. Cây đa vụ mùa."));
        register(new ItemData("seed_melon", Rarity.COMMON, Category.VEG, 0.0, Material.MELON_SEEDS, "Hạt Giống Dưa Hấu", null, "Trồng trên đất cày. Cây đa vụ mùa."));
        register(new ItemData("seed_sweet_berry", Rarity.COMMON, Category.VEG, 0.0, Material.SWEET_BERRIES, "Mầm Quả Mọng", null, "Trồng trên khối Đất/Cỏ. Cây đa vụ mùa."));
        register(new ItemData("seed_glow_berry", Rarity.COMMON, Category.VEG, 0.0, Material.GLOW_BERRIES, "Mầm Quả Mọng Sáng", null, "Trồng dưới trần nhà. Cây đa vụ mùa."));
        register(new ItemData("seed_mushroom", Rarity.COMMON, Category.VEG, 0.0, Material.BROWN_MUSHROOM, "Bào Tử Nấm Hương", null, "Trồng trên thân gỗ. Thời gian: 4 Giờ."));
        register(new ItemData("seed_red_mushroom", Rarity.COMMON, Category.VEG, 0.0, Material.RED_MUSHROOM, "Bào Tử Nấm Đỏ", null, "Trồng trên thân gỗ. Thời gian: 4 Giờ."));
        register(new ItemData("seed_kelp", Rarity.COMMON, Category.VEG, 0.0, Material.KELP, "Mầm Rong Biển", null, "Trồng dưới nước. Cây đa vụ mùa."));
        register(new ItemData("seed_cocoa", Rarity.COMMON, Category.VEG, 0.0, Material.COCOA_BEANS, "Hạt Cacao Thô", null, "Trồng trên thân Gỗ Rừng. Cây đa vụ mùa."));
        register(new ItemData("seed_cucumber", Rarity.COMMON, Category.VEG, 0.0, Material.PUMPKIN_SEEDS, "Hạt Giống Dưa Chuột", null, "Trồng dưới nước. Cây đa vụ mùa."));

        // Hạt giống Ma thuật đặc quyền
        register(new ItemData("seed_magic_golden_carrot", Rarity.EPIC, Category.VEG, 0.0, Material.GOLD_NUGGET, "Hạt Ma Thuật: Cà Rốt Vàng", "Yêu cầu: Nghề Nông Dân", "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_magic_flame_beetroot", Rarity.EPIC, Category.VEG, 0.0, Material.BEETROOT_SEEDS, "Hạt Ma Thuật: Hỏa Ngục", "Yêu cầu: Nghề Nông Dân", "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_magic_golden_melon", Rarity.EPIC, Category.VEG, 0.0, Material.MELON_SEEDS, "Hạt Ma Thuật: Dưa Hoàng Kim", "Yêu cầu: Nghề Nông Dân", "Trồng trên đất cày. Cây đa vụ mùa."));
        register(new ItemData("seed_magic_glowing_mushroom", Rarity.EPIC, Category.VEG, 0.0, Material.RED_MUSHROOM, "Bào Tử Ma Thuật: Nấm Quang", "Yêu cầu: Nghề Nông Dân", "Trồng trên thân gỗ. Cây đa vụ mùa."));
        register(new ItemData("seed_magic_star_wheat", Rarity.EPIC, Category.VEG, 0.0, Material.WHEAT_SEEDS, "Hạt Ma Thuật: Tinh Tú", "Yêu cầu: Nghề Nông Dân", "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_magic_soul_sweet_berry", Rarity.EPIC, Category.VEG, 0.0, Material.SWEET_BERRIES, "Mầm Ma Thuật: Đoạt Hồn", "Yêu cầu: Nghề Nông Dân", "Trồng trên Đất/Cỏ. Cây đa vụ mùa."));
        register(new ItemData("seed_magic_crystal_kelp", Rarity.EPIC, Category.VEG, 0.0, Material.KELP, "Mầm Ma Thuật: Tảo Pha Lê", "Yêu cầu: Nghề Nông Dân", "Trồng dưới nước. Cây đa vụ mùa."));
        register(new ItemData("seed_magic_dragon_cocoa", Rarity.EPIC, Category.VEG, 0.0, Material.COCOA_BEANS, "Hạt Ma Thuật: Thanh Long", "Yêu cầu: Nghề Nông Dân", "Trồng trên Gỗ Rừng. Cây đa vụ mùa."));
        register(new ItemData("seed_magic_mana_potato", Rarity.EPIC, Category.VEG, 0.0, Material.POTATO, "Mầm Ma Thuật: Khoai Ma Năng", "Yêu cầu: Nghề Nông Dân", "Trồng trên đất cày. Thời gian: 4 Giờ."));
        register(new ItemData("seed_magic_chaos_glow_berry", Rarity.EPIC, Category.VEG, 0.0, Material.GLOW_BERRIES, "Hạt Ma Thuật: Hỗn Độn", "Yêu cầu: Nghề Nông Dân", "Trồng dưới trần nhà. Cây đa vụ mùa."));

        // --- 14. ĐẶC QUYỀN NGHỀ NÔNG DÂN (FARMER) ---
        // Tinh chất & Túi hạt
        register(new ItemData("misc_nature_essence", Rarity.EPIC, Category.MISC, 0.0, Material.SLIME_BALL, "Tinh Chất Tự Nhiên", null, "Kết tinh từ sự sống của cỏ cây.", "Đơn vị tiền tệ chính của Hiệp Hội Nông Dân."));
        register(new ItemData("bag_seeds_common", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Hạt Giống Hỗn Hợp", "Dùng ngay: Nhận 16 hạt giống ngẫu nhiên", "Gói hạt giống bự dành cho người mới làm nông."));
        register(new ItemData("bag_seeds_magic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Hạt Ma Thuật", "Dùng ngay: Nhận 5-8 hạt ma thuật ngẫu nhiên", "Tỏa ra một luồng ma lực dịu nhẹ."));
        register(new ItemData("misc_watering_can", Rarity.RARE, Category.MISC, 0.0, Material.IRON_NUGGET, "Bình Tưới Nước", "Click vào cây: Giảm 1 tiếng cho cây chỉ định | Độ bền: 20 lần sử dụng.", "Cây cần nước, bạn cũng vậy, nhớ uống đủ nước mỗi ngày!"));
        register(new ItemData("misc_fertilizer", Rarity.EPIC, Category.MISC, 0.0, Material.BONE_MEAL, "Phân Bón Hảo Hạng", "Click vào cây: Giảm 1-2 tiếng cho cây xung quanh (3x3) | Độ bền: 10 lần sử dụng.", "Giải pháp đơn giản mà vô cùng hiệu quả giúp cây sinh trưởng"));
        register(new ItemData("armor_farmer_hat", Rarity.EPIC, Category.MISC, 0.0, Material.GOLDEN_HELMET, "Nón Lá Chân Truyền", "Tăng 10% tỉ lệ kích hoạt mọi Nội tại Nông dân | Độ bền: 300 lần thu hoạch.", "Hiệu quả khi đội lên đầu trong lúc thu hoạch"));
        register(new ItemData("misc_seed_pouch", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Trữ Hạt", "Chuột phải: Mở túi ma thuật chứa hạt giống", "Vật phẩm tối ưu cho việc trữ hạt giống.", "Chỉ có thể bỏ hạt giống vào bên trong."));

        // ==========================================
        // 14. NGƯ DÂN CÂU CÁ
        // ==========================================
        // ==========================================
        // [MỚI] TÚI CẦN CÂU & PHÔI CẦN CÂU
        // ==========================================
        register(new ItemData("misc_ocean_tear", Rarity.EPIC, Category.MISC, 0.0, Material.GHAST_TEAR, "Nước Mắt Đại Dương", null, "Kết tinh pha lê lấp lánh từ đại dương sâu thẳm", "Đơn vị trao đổi chính của Những Người Ngư Dân."));
        register(new ItemData("tool_fishing_rod_1", Rarity.COMMON, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Tập Sự", null, "Độ bền vô hạn. Không có hiệu ứng."));
        register(new ItemData("tool_fishing_rod_common", Rarity.COMMON, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Gỗ Sồi", "Sở hữu 1 Dòng thuộc tính", "Bền bỉ, mộc mạc."));
        register(new ItemData("tool_fishing_rod_uncommon", Rarity.UNCOMMON, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Trúc Xanh", "Sở hữu 1 Dòng thuộc tính mạnh", "Nhẹ nhàng, dẻo dai."));
        register(new ItemData("tool_fishing_rod_rare", Rarity.RARE, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Cốt Thép", "Sở hữu 2 Dòng thuộc tính", "Chịu lực kéo cực tốt."));
        register(new ItemData("tool_fishing_rod_epic", Rarity.EPIC, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Sợi Thủy Tinh", "Sở hữu 2 Dòng thuộc tính cực mạnh", "Công nghệ tiên tiến, độ cong hoàn hảo."));
        register(new ItemData("tool_fishing_rod_mythic", Rarity.MYTHIC, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Vảy Rồng", "Sở hữu 3 Dòng thuộc tính", "Bất phàm. Đủ sức kéo cả rồng biển."));
        register(new ItemData("tool_fishing_rod_legendary", Rarity.LEGENDARY, Category.TOOL, 0.0, Material.FISHING_ROD, "Cần Câu Hải Thần", "Sở hữu 3 Dòng thuộc tính Thần Thoại", "Bảo vật của đại dương."));

        register(new ItemData("bag_fishing_rod_common", Rarity.COMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Cần Câu Thường", "Click phải để nhận ngẫu nhiên 1 Cần Câu Sồi."));
        register(new ItemData("bag_fishing_rod_uncommon", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Cần Câu Tốt", "Click phải để nhận ngẫu nhiên 1 Cần Câu Trúc."));
        register(new ItemData("bag_fishing_rod_rare", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Cần Câu Hiếm", "Click phải để nhận ngẫu nhiên 1 Cần Câu Thép."));
        register(new ItemData("bag_fishing_rod_epic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Cần Câu Tinh Anh", "Click phải để nhận ngẫu nhiên 1 Cần Câu Thủy Tinh."));
        register(new ItemData("bag_fishing_rod_mythic", Rarity.MYTHIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Cần Câu Huyền Thoại", "Click phải để nhận ngẫu nhiên 1 Cần Câu Vảy Rồng."));
        register(new ItemData("bag_fishing_rod_legendary", Rarity.LEGENDARY, Category.MISC, 0.0, Material.BUNDLE, "Túi Cần Câu Tuyệt Thế", "Click phải để nhận ngẫu nhiên 1 Cần Câu Hải Thần."));
        // ==========================================
        // 14.1 MỒI NỀN (BASE BAITS) - Có giảm hiệu suất nếu > 3
        // ==========================================
        // Mọi nơi (ALL)
        register(new ItemData("bait_base_earthworm", Rarity.COMMON, Category.BAIT_BASE, 0.0, Material.BROWN_DYE, "Giun Đất", "Giảm 5% thời gian chờ", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_minnow", Rarity.COMMON, Category.BAIT_BASE, 0.0, Material.TROPICAL_FISH, "Cá Mồi Nhỏ", "Tăng 5% tỉ lệ cá khổng lồ", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_shiny_pebble", Rarity.RARE, Category.BAIT_BASE, 0.0, Material.PRISMARINE_CRYSTALS, "Sỏi Lấp Lánh", "Tăng 2% tỉ lệ Kho Báu", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_sludge", Rarity.COMMON, Category.BAIT_BASE, 0.0, Material.CLAY_BALL, "Bùn Thối", "Tăng 10% tỉ lệ câu Rác", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_corn", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.GOLD_NUGGET, "Hạt Bắp", "Giảm 5% tỉ lệ câu Rác", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_sticky_sap", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.SLIME_BALL, "Nhựa Cây Dính", "Tăng 50ms dung sai nhịp", "Tác dụng: Mọi môi trường."));

        // Biển (OCEAN)
        register(new ItemData("bait_base_sandworm", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.BONE_MEAL, "Giun Cát", "Giảm 10% thời gian chờ", "Tác dụng: Môi trường biển."));
        register(new ItemData("bait_base_squid_meat", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.GLOW_INK_SAC, "Thịt Mực", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: Môi trường biển."));
        register(new ItemData("bait_base_broken_shell", Rarity.RARE, Category.BAIT_BASE, 0.0, Material.NAUTILUS_SHELL, "Vỏ Sò Vỡ", "Tăng 5% tỉ lệ Kho Báu", "Tác dụng: Môi trường biển."));
        register(new ItemData("bait_base_dead_kelp", Rarity.COMMON, Category.BAIT_BASE, 0.0, Material.DRIED_KELP, "Rong Biển Chết", "Tăng 20% tỉ lệ câu Rác", "Tác dụng: Môi trường biển."));

        // Sông/Hồ (RIVER)
        register(new ItemData("bait_base_leech", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.INK_SAC, "Đỉa Sông", "Giảm 10% thời gian chờ", "Tác dụng: Môi trường sông hoặc hồ."));
        register(new ItemData("bait_base_loach", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.COD, "Cá Lòng Tong", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: Môi trường sông hoặc hồ."));
        register(new ItemData("bait_base_rusty_coin", Rarity.RARE, Category.BAIT_BASE, 0.0, Material.IRON_NUGGET, "Đồng Xu Gỉ", "Tăng 5% tỉ lệ Kho Báu", "Tác dụng: Môi trường sông hoặc hồ."));
        register(new ItemData("bait_base_moss", Rarity.COMMON, Category.BAIT_BASE, 0.0, Material.MOSS_BLOCK, "Rêu Ẩm Ướt", "Tăng 20% tỉ lệ câu Rác", "Tác dụng: Môi trường sông hoặc hồ."));

        // Hang động ngầm (CAVE)
        register(new ItemData("bait_base_blindworm", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.STRING, "Giun Mù", "Giảm 10% thời gian chờ", "Tác dụng: Môi trường hang động."));
        register(new ItemData("bait_base_blind_crab", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.SPIDER_EYE, "Cua Mù", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: Môi trường hang động."));
        register(new ItemData("bait_base_stalactite", Rarity.RARE, Category.BAIT_BASE, 0.0, Material.POINTED_DRIPSTONE, "Mảnh Thạch Nhũ", "Tăng 5% tỉ lệ Kho Báu", "Tác dụng: Môi trường hang động."));
        register(new ItemData("bait_base_mold", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.BROWN_MUSHROOM, "Nấm Mốc Độc", "Giảm 10% tỉ lệ câu Rác", "Tác dụng: Môi trường hang động."));

        // Đầm lầy (SWAMP)
        register(new ItemData("bait_base_swamp_snail", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.NAUTILUS_SHELL, "Ốc Sên Lầy", "Giảm 10% thời gian chờ", "Tác dụng: Môi trường đầm lầy."));
        register(new ItemData("bait_base_small_frog", Rarity.UNCOMMON, Category.BAIT_BASE, 0.0, Material.SLIME_BALL, "Ếch Nhỏ", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: Môi trường đầm lầy."));

        // ==========================================
        // 14.2. MỒI BUFF (MODIFIERS) - Không bị giảm hiệu suất
        // ==========================================
        // Buff Mọi nơi (ALL)
        register(new ItemData("bait_buff_pure_water", Rarity.UNCOMMON, Category.BAIT_BUFF, 0.0, Material.GHAST_TEAR, "Nước Tinh Khiết", "Giảm 30% tỉ lệ câu Rác", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_sonar", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.COMPASS, "Thiết Bị Dò Cá", "Tăng 15% cá khổng lồ", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_magnet", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.IRON_INGOT, "Lõi Từ Tính", "Tăng 10% Kho báu", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_metronome", Rarity.RARE, Category.BAIT_BUFF, 0.0, Material.CLOCK, "Con Lắc Nhịp", "Tăng 150ms dung sai nhịp", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_phoenix", Rarity.LEGENDARY, Category.BAIT_BUFF, 0.0, Material.FEATHER, "Lông Phượng Hoàng", "Tăng 50% cơ hội câu lại", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_lazy_charm", Rarity.RARE, Category.BAIT_BUFF, 0.0, Material.PAPER, "Bùa Lười Biếng", "20% Bỏ qua Minigame", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_wind_glyph", Rarity.RARE, Category.BAIT_BUFF, 0.0, Material.FEATHER, "Phù Hiệu Gió", "Giảm 20% thời gian chờ", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_ancient_bone", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.BONE, "Xương Cổ Đại", "Tăng 300ms dung sai nhịp", "Phụ gia mồi câu."));

        // Buff Tối Thượng (Ultimate - Mọi nơi)
        register(new ItemData("bait_buff_monster_blood", Rarity.MYTHIC, Category.BAIT_BUFF, 0.0, Material.REDSTONE, "Máu Vực Thẳm", "Chắc chắn gặp Thủy Quái", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_golden_lure", Rarity.LEGENDARY, Category.BAIT_BUFF, 0.0, Material.RAW_GOLD, "Lưỡi Câu Vàng", "Chắc chắn x3 sản lượng", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_pirate_coin", Rarity.MYTHIC, Category.BAIT_BUFF, 0.0, Material.SUNFLOWER, "Đồng Xu Hải Tặc", "Chắc chắn câu được Rương", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_lightning", Rarity.MYTHIC, Category.BAIT_BUFF, 0.0, Material.GLOWSTONE_DUST, "Tinh Hoa Sấm Sét", "Cá cắn câu ngay lập tức", "Phụ gia mồi câu."));

        // Buff Đặc quyền Môi trường (Cực mạnh)
        register(new ItemData("bait_buff_glowing_oil", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.GLOW_INK_SAC, "Dầu Cá Phát Quang", "Tăng 30% cá khổng lồ", "Tác dụng: Môi trường hang động."));
        register(new ItemData("bait_buff_coral_powder", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.BONE_MEAL, "Bột San Hô", "Tăng 20% Kho Báu", "Tác dụng: Môi trường biển."));
        register(new ItemData("bait_buff_freshwater_crystal", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.PRISMARINE_CRYSTALS, "Tinh Thể Nước Ngọt", "Giảm 40% thời gian chờ", "Tác dụng: Môi trường sông hoặc hồ."));
        register(new ItemData("bait_buff_spider_venom", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.SPIDER_EYE, "Nọc Nhện Hang", "Giảm 40% thời gian chờ", "Tác dụng: Môi trường hang động."));
        register(new ItemData("bait_buff_mutant_lilypad", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.LILY_PAD, "Hoa Súng Đột Biến", "Tăng 30% cá khổng lồ", "Tác dụng: Môi trường đầm lầy."));
        register(new ItemData("bait_buff_mermaid_tear", Rarity.LEGENDARY, Category.BAIT_BUFF, 0.0, Material.GHAST_TEAR, "Nước Mắt Nhân Ngư", "Tăng 100% cơ hội câu lại", "Tác dụng: Môi trường biển."));
        register(new ItemData("bait_buff_river_demon_eye", Rarity.MYTHIC, Category.BAIT_BUFF, 0.0, Material.ENDER_EYE, "Mắt Quỷ Sông", "Chắc chắn gặp Thủy Quái", "Tác dụng: Môi trường sông hoặc hồ."));
        register(new ItemData("bait_buff_swamp_gas", Rarity.EPIC, Category.BAIT_BUFF, 0.0, Material.DRAGON_BREATH, "Khí Đầm Lầy", "50% Bỏ qua Minigame", "Tác dụng: Môi trường đầm lầy."));

        // ==========================================
        // 14.3. THÀNH PHẨM (CUSTOM BAIT)
        // ==========================================
        register(new ItemData("bait_custom", Rarity.EPIC, Category.MISC, 0.0, Material.MAGMA_CREAM, "§d§lMồi Câu Đặc Chế", "Cầm ở tay phụ để sử dụng.", "Hỗn hợp pha trộn hoàn hảo."));

        // ==========================================
        // 15. HỆ THỐNG NUÔI THÚ CƯỠI & THƯƠNG NHÂN
        // ==========================================
        // Thêm dòng này vào phần NHÓM KHÁC trong ItemManager.java
        register(new ItemData("misc_merchant_token", Rarity.EPIC, Category.MISC, 0.0, Material.PAPER, "Chứng Nhận Thương Nhân", null,"Vật phẩm đặc quyền của Thương Nhân.", "Dùng để mua các vật phẩm đặc biệt."));
        register(new ItemData("misc_mount_upgrade", Rarity.RARE, Category.MISC, 0.0, Material.PAPER, "Giấy Nâng Cấp Thú Cưỡi", null,"Dùng để nâng cấp các chỉ số", "cho thú cưỡi tại Chuồng Ngựa."));
        // [CÁC ITEM CỦA SHOP THƯƠNG NHÂN]
        register(new ItemData("misc_gold_scale", Rarity.EPIC, Category.MISC, 0.0, Material.FIELD_MASONED_BANNER_PATTERN, "Cân Tiểu Ly Mạ Vàng", "Tăng 50% tỉ lệ rớt tiền từ quái vật.", "Chưa kích hoạt. Click chuột phải để dùng (24h)."));
        register(new ItemData("bag_cloth", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Vải Thô", "Lưu trữ tối đa 18 vật phẩm", "Trang bị sức chứa cơ bản."));
        register(new ItemData("bag_leather", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Da Cao Cấp", "Lưu trữ tối đa 36 vật phẩm", "Trang bị sức chứa nâng cao."));
        register(new ItemData("bag_dragon_scale", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Vảy Rồng", "Lưu trữ tối đa 54 vật phẩm", "Sức chứa khổng lồ cho dân buôn."));
        register(new ItemData("misc_stable_expansion", Rarity.MYTHIC, Category.MISC, 0.0, Material.PAPER, "Giấy Phép Mở Rộng Chuồng", "Sử dụng để +1 ô chứa thú cưỡi.", "Giấy phép mở rộng chuồng có giá trị tuương đương."));

        // [CÁC ITEM NÂNG CẤP VÀ ĐÓNG GÓI THÚ CƯỠI]
        register(new ItemData("misc_mount_retrieval", Rarity.EPIC, Category.MISC, 0.0, Material.PAPER, "Giấy Thu Hồi Thú Cưỡi", "Hiệu quả: Bảo toàn mọi chỉ số và túi đồ.", "Click phải vào thú cưỡi đang triệu hồi", "để đóng gói và giao dịch thú cưỡi."));
        register(new ItemData("misc_mount_upgrade_speed", Rarity.RARE, Category.MISC, 0.0, Material.SUGAR, "Bùa Nâng Cấp: Tốc Độ", "Sử dụng: Tăng lập tức 1 bậc Tốc Độ.", "Click phải vào thú cưỡi đang triệu hồi để sử dụng."));
        register(new ItemData("misc_mount_upgrade_jump", Rarity.RARE, Category.MISC, 0.0, Material.RABBIT_FOOT, "Bùa Nâng Cấp: Sức Nhảy", "Sử dụng: Tăng lập tức 1 bậc Sức Nhảy.", "Click phải vào thú cưỡi đang triệu hồi để sử dụng."));
        register(new ItemData("misc_mount_upgrade_durability", Rarity.RARE, Category.MISC, 0.0, Material.LEATHER, "Bùa Nâng Cấp: Sức Bền", "Sử dụng: Tăng lập tức 1 bậc Sức Bền.", "Click phải vào thú cưỡi đang triệu hồi để sử dụng."));
        register(new ItemData("misc_mount_upgrade_cooldown", Rarity.RARE, Category.MISC, 0.0, Material.CLOCK, "Bùa Nâng Cấp: Hồi Chiêu", "Sử dụng: Tăng lập tức 1 bậc Hồi Chiêu.", "Click phải vào thú cưỡi đang triệu hồi để sử dụng."));
        register(new ItemData("misc_mount_upgrade_storage", Rarity.RARE, Category.MISC, 0.0, Material.CHEST, "Bùa Nâng Cấp: Kho Chứa", "Sử dụng: Tăng lập tức 1 bậc Kho Chứa.", "Click phải vào thú cưỡi đang triệu hồi để sử dụng."));

        // Vật phẩm đặc biệt (Tạo ra khi đóng gói)
        register(new ItemData("misc_mount_packaged", Rarity.EPIC, Category.MISC, 0.0, Material.NAME_TAG, "Thẻ Bài Thú Cưỡi", "Sử dụng: Nhận ngay 1 thú cưỡi khi sử dụng", "Hợp đồng chuyển nhượng thú cưỡi, mua từ thương nhân."));

        // ==========================================
        // 16. HỆ THỐNG THẢO DƯỢC (DƯỢC SƯ - ALCHEMIST)
        // ==========================================

        // --- VÙNG 1: NGOẠI Ô YÊN BÌNH (COMMON) ---
        register(new ItemData("herb_blood_grass", Rarity.COMMON, Category.HERB, 0.0, Material.FERN, "Cỏ Máu", "Vị Đắng | Pha chế: +10% Hồi máu tức thì", "Loài cỏ mang sắc đỏ như máu tươi.", "Mọc rải rác ở các vùng đồng bằng."));
        register(new ItemData("herb_blue_fern", Rarity.COMMON, Category.HERB, 0.0, Material.BLUE_ORCHID, "Dương Xỉ Lam", "Vị Đắng | Pha chế: +10% Hồi năng lượng tức thì", "Lá cây có màu xanh biếc.", "Thường mọc cạnh các bờ suối."));
        register(new ItemData("herb_vitality_shroom", Rarity.COMMON, Category.HERB, 0.0, Material.RED_MUSHROOM, "Nấm Sinh Khí", "Vị Ngọt | Pha chế: +10% Tỉ lệ hồi phục", "Hút lấy sinh khí từ những gốc cây mục nát.", "Mang lại sức sống dồi dào cho người sử dụng."));
        register(new ItemData("herb_swift_maple", Rarity.COMMON, Category.HERB, 0.0, Material.MANGROVE_LEAVES, "Lá Phong Rừng", "Vị Chua | Pha chế: +15% Tốc độ di chuyển", "Nhẹ bẫng tựa như không có trọng lượng.", "Người ta đồn nó có thể nương theo chiều gió."));
        register(new ItemData("herb_muscle_root", Rarity.COMMON, Category.HERB, 0.0, Material.HANGING_ROOTS, "Rễ Lực Sĩ", "Vị Đắng | Pha chế: +10% Sát thương", "Đâm rễ xuyên qua cả những tầng đá cứng nhất.", "Nhựa cây chứa đầy sự cuồng bạo."));
        register(new ItemData("herb_spring_leaf", Rarity.COMMON, Category.HERB, 0.0, Material.BAMBOO, "Cỏ Lò Xo", "Vị Mặn | Pha chế: Hiệu ứng Nhảy cao", "Thân cây xoắn lại kỳ lạ.", "Chạm nhẹ cũng có thể khiến nó bật nảy lên."));
        register(new ItemData("herb_wild_ember", Rarity.COMMON, Category.HERB, 0.0, Material.DEAD_BUSH, "Bụi Than Hoang", "Vị Cay | Pha chế: Hiệu ứng Kháng lửa", "Mọc trên lớp tro tàn của núi lửa.", "Lúc nào cũng tỏa ra nhiệt lượng âm ỉ."));
        register(new ItemData("herb_toxic_toadstool", Rarity.COMMON, Category.HERB, 0.0, Material.BROWN_MUSHROOM, "Nấm Cóc Độc", "Vị Đắng | Pha chế: Hiệu ứng Độc", "Có những đốm xanh lá gớm ghiếc.", "Chỉ chạm nhẹ vào cũng thấy da tay tê rần."));
        register(new ItemData("herb_wither_weed", Rarity.COMMON, Category.HERB, 0.0, Material.WITHER_ROSE, "Cỏ Héo Rũ", "Vị Chua | Pha chế: Thuốc Chậm chạp", "Rút cạn sinh khí của mọi sinh vật xung quanh.", "Cầm lâu trên tay sẽ có cảm giác rã rời."));
        register(new ItemData("herb_dry_vine", Rarity.COMMON, Category.HERB, 0.0, Material.VINE, "Dây Cỏ Khô", "Vô vị | Pha chế: Chất kết dính", "Mộc mạc, khô quắt và cực kỳ dẻo dai.", "Tuy không có dược tính nhưng thiếu nó thì", "các loại thuốc khác sẽ phản ứng bùng nổ."));

        // --- VÙNG 2: RỪNG SÂU NGUY HIỂM (UNCOMMON / RARE) ---
        register(new ItemData("herb_ruby_petal", Rarity.UNCOMMON, Category.HERB, 0.0, Material.RED_TULIP, "Cánh Hoa Hồng Ngọc", "Vị Ngọt | Pha chế: +15% Hồi máu tức thì", "Đỏ thắm như một viên hồng ngọc tỏa sáng.", "Thấm đẫm sinh khí từ lòng đất mẹ."));
        register(new ItemData("herb_sapphire_leaf", Rarity.UNCOMMON, Category.HERB, 0.0, Material.CORNFLOWER, "Lá Ngọc Bích", "Vị Ngọt | Pha chế: +15% Hồi năng lượng tức thì", "Mang sắc xanh của bầu trời huyền bí.", "Giúp khôi phục lại tinh thần một cách nhanh chóng."));
        register(new ItemData("herb_glow_bloom", Rarity.UNCOMMON, Category.HERB, 0.0, Material.GLOW_LICHEN, "Hoa Dạ Quang", "Vị Ngọt | Pha chế: Hiệu ứng Nhìn trong đêm", "Chỉ nở rộ và phát sáng khi màn đêm buông xuống.", "Là nguồn sống của các bầy đom đóm rừng."));
        register(new ItemData("herb_water_lung_moss", Rarity.UNCOMMON, Category.HERB, 0.0, Material.SEAGRASS, "Rêu Phổi Nước", "Vị Mặn | Pha chế: Hiệu ứng Thở dưới nước", "Lá rêu giữ lại hàng vạn bọng khí li ti.", "Giúp người ta trao đổi oxy ngay cả khi ngập nước."));
        register(new ItemData("herb_mist_petal", Rarity.UNCOMMON, Category.HERB, 0.0, Material.ALLIUM, "Cánh Sương Mù", "Vô vị | Pha chế: Hiệu ứng Tàng hình", "Cánh hoa gần như trong suốt.", "Tan biến ngay lập tức nếu có ánh sáng gắt chiếu vào."));
        register(new ItemData("herb_feather_leaf", Rarity.UNCOMMON, Category.HERB, 0.0, Material.TALL_GRASS, "Lông Chim Rơi", "Vô vị | Pha chế: Hiệu ứng Rơi chậm", "Lơ lửng giữa không trung rất lâu trước khi chạm đất.", "Hoàn toàn miễn nhiễm với trọng lực."));
        register(new ItemData("herb_mana_shroom", Rarity.UNCOMMON, Category.HERB, 0.0, Material.WARPED_FUNGUS, "Nấm Lam", "Vị Ngọt | Pha chế: +15% Tỉ lệ hồi năng lượng", "Hấp thụ dòng chảy ma lực dưới lòng đất.", "Phát ra một luồng lam quang êm dịu."));
        register(new ItemData("herb_stone_root", Rarity.UNCOMMON, Category.HERB, 0.0, Material.POINTED_DRIPSTONE, "Rễ Đá Cuội", "Vị Mặn | Pha chế: +15% Giáp", "Cứng như đá tảng, đập vào phát ra tiếng leng keng.", "Có thể mài ra để gia cố lớp biểu bì da."));
        register(new ItemData("herb_thorn_daisy", Rarity.UNCOMMON, Category.HERB, 0.0, Material.CORNFLOWER, "Cúc Gai", "Vị Cay | Pha chế: +10% Tỉ lệ đỡ đòn", "Xung quanh đài hoa là hàng rào gai nhọn hoắt.", "Kẻ nào vô ý chạm vào chắc chắn sẽ đổ máu."));
        register(new ItemData("herb_wild_licorice", Rarity.RARE, Category.HERB, 0.0, Material.SUGAR_CANE, "Rễ Cam Thảo Rừng", "Vị Ngọt | Pha chế: Khử Đắng / Khử Chua", "Vị ngọt gắt nhưng để lại hậu vị vô cùng êm ái.", "Chất điều vị không thể thiếu để tạo ra các Thần dược", "dễ uống, giúp tối đa hóa công dụng của thuốc."));
        register(new ItemData("herb_frost_sprout", Rarity.RARE, Category.HERB, 0.0, Material.BLUE_ORCHID, "Mầm Hàn Băng", "Vị Cay | Pha chế: +15% Kháng hiệu ứng", "Vẫn vươn mình sống sót qua những mùa đông kỷ băng hà.", "Lá cây buốt giá như vừa lấy ra từ hầm băng."));
        register(new ItemData("herb_crimson_lotus", Rarity.RARE, Category.HERB, 0.0, Material.ROSE_BUSH, "Huyết Liên Hoa", "Vị Đắng | Pha chế: +20% Hồi máu tức thì", "Bông sen đỏ thấm đẫm huyết khí chiến trường.", "Khả năng cầm máu thuộc hàng kỳ tích."));
        register(new ItemData("herb_azure_bloom", Rarity.RARE, Category.HERB, 0.0, Material.LILY_OF_THE_VALLEY, "Hoa Lưu Ly Xanh", "Vị Đắng | Pha chế: +20% Hồi năng lượng tức thì", "Bông hoa mọc ở những giao điểm ma pháp.", "Mang một lượng mana thuần khiết có thể dùng ngay."));

        // --- VÙNG 3: VÙNG ĐẤT KHẮC NGHIỆT (RARE / EPIC) ---
        register(new ItemData("herb_sword_grass", Rarity.RARE, Category.HERB, 0.0, Material.SHORT_GRASS, "Cỏ Kiếm", "Vị Cay | Pha chế: +10% Tỉ lệ xuyên giáp", "Phiến lá mỏng dính nhưng cực kỳ sắc bén.", "Một cơn gió thổi qua cũng đủ cắt đứt gỗ mục."));
        register(new ItemData("herb_prism_orchid", Rarity.EPIC, Category.HERB, 0.0, Material.LILY_OF_THE_VALLEY, "Phong Lan Lăng Kính", "Vị Ngọt | Pha chế: +20% Kháng phép", "Bông hoa trong suốt, bẻ cong mọi ánh sáng đi qua nó.", "Được cho là có thể làm lệch hướng cả ma pháp."));
        register(new ItemData("herb_mirage_leaf", Rarity.EPIC, Category.HERB, 0.0, Material.LARGE_FERN, "Lá Ảo Ảnh", "Vị Đắng | Pha chế: +5% Tỉ lệ né tránh", "Nhìn chằm chằm vào nó sẽ sinh ra ảo giác.", "Dược tính gây ức chế hệ thần kinh thị giác."));
        register(new ItemData("herb_wolf_sand_seed", Rarity.EPIC, Category.HERB, 0.0, Material.COCOA_BEANS, "Hạt Cát Sói", "Vị Cay | Pha chế: +15% Tỉ lệ chí mạng", "Chỉ chịu nảy mầm khi hấp thụ ánh trăng tròn.", "Kích phát bản năng hoang dã tiềm ẩn trong huyết quản."));
        register(new ItemData("herb_venom_tongue", Rarity.EPIC, Category.HERB, 0.0, Material.SWEET_BERRIES, "Quả Lưỡi Độc", "Vị Chua | Pha chế: Sát thương phép thuật", "Sinh trưởng ở những bãi lầy độc hại nhất.", "Dịch quả khiến mọi vết thương không thể khép miệng."));
        register(new ItemData("herb_carnivore_flower", Rarity.EPIC, Category.HERB, 0.0, Material.CRIMSON_FUNGUS, "Hoa Ăn Thịt", "Vị Đắng | Pha chế: +10% Tỉ lệ hút máu", "Đài hoa có màu đỏ sẫm và tanh nồng mùi máu.", "Nó được nuôi dưỡng bằng xác của những con ma thú."));
        //register(new ItemData("herb_ancient_water", Rarity.EPIC, Category.HERB, 0.0, Material.POTION, "Nước Tinh Khiết Cổ Đại", "Vô vị | Pha chế: Dung môi (Tăng 50% Tỉ lệ thành công)", "Lấy từ khe nứt sâu thẳm dưới lòng đất.", "Thứ chất lỏng thuần khiết duy nhất có thể kìm hãm", "sự phản ứng mãnh liệt của các loại Thần Dược."));
        register(new ItemData("herb_phoenix_tear", Rarity.EPIC, Category.HERB, 0.0, Material.WEEPING_VINES, "Giọt Lệ Phượng Hoàng", "Vị Cay | Pha chế: +25% Hồi máu tức thì", "Rơi xuống từ bầu trời rực lửa.", "Mỗi giọt mang sức sống tái sinh của truyền thuyết."));
        register(new ItemData("herb_astral_fruit", Rarity.EPIC, Category.HERB, 0.0, Material.CHORUS_FLOWER, "Quả Tinh Tú", "Vị Mặn | Pha chế: +25% Hồi năng lượng tức thì", "Ngưng tụ ánh sáng các vì sao.", "Ăn vào cảm giác như vũ trụ thu nhỏ trong dạ dày."));

        // --- VÙNG 4: VỰC THẲM & TẬN CÙNG THẾ GIỚI (MYTHIC) ---
        register(new ItemData("herb_ancient_blood_bud", Rarity.MYTHIC, Category.HERB, 0.0, Material.POPPY, "Nụ Hoa Máu Cổ Đại", "Vị Ngọt | Pha chế: +100% Hồi máu tức thì", "Nở ra từ lòng đất của những chiến trường thượng cổ.", "Mang trong mình sức sống mãnh liệt trào dâng."));
        register(new ItemData("herb_mana_root", Rarity.MYTHIC, Category.HERB, 0.0, Material.WEEPING_VINES, "Rễ Ma Năng", "Vị Đắng | Pha chế: +100% Hồi năng lượng tức thì", "Rễ cây rung giật liên hồi vì chứa quá nhiều năng lượng.", "Hấp thu trực tiếp ma lực nguyên thủy của thế giới."));
        register(new ItemData("herb_earth_heart", Rarity.MYTHIC, Category.HERB, 0.0, Material.POTATO, "Quả Tim Đất", "Vị Cay | Pha chế: +1000 Máu tối đa", "Đập từng nhịp thình thịch như một trái tim thật.", "Sự kết tinh hoàn hảo giữa mạch nước ngầm và đá Magma."));
        register(new ItemData("herb_abyss_algae", Rarity.MYTHIC, Category.HERB, 0.0, Material.KELP, "Tảo Vực Thẳm", "Vị Mặn | Pha chế: +20% Tốc độ đánh", "Sống sót dưới áp lực nước hàng vạn tấn.", "Không một sức mạnh vật lý nào có thể cản bước."));
        register(new ItemData("herb_magma_seed", Rarity.MYTHIC, Category.HERB, 0.0, Material.MAGMA_CREAM, "Hạt Dung Nham", "Vị Cay | Pha chế: +20% Tỉ lệ xuyên kháng phép", "Cầm trên tay vẫn còn rực cháy.", "Thiêu rụi mọi lớp giáp kháng ma thuật cứng cáp nhất."));
        register(new ItemData("herb_fairy_tear", Rarity.MYTHIC, Category.HERB, 0.0, Material.GHAST_TEAR, "Nước Mắt Tinh Linh", "Vị Mặn | Pha chế: Thanh tẩy siêu cấp", "Lấp lánh rực rỡ như một viên kim cương.", "Thứ nước thánh thanh tẩy mọi sự ô uế trên đời."));

        // --- VÙNG 5: DỊ BIẾN KHÔNG GIAN (LEGENDARY) ---
        register(new ItemData("herb_immortal_bloom", Rarity.LEGENDARY, Category.HERB, 0.0, Material.DANDELION, "Hoa Kim Tiên", "Vị Ngọt | Pha chế: Bất Tử (HP không dưới 1 trong 10s)", "Những cánh hoa như được đúc từ vàng ròng nguyên khối.", "Thách thức lại cả quy luật sinh tử của luân hồi."));
        register(new ItemData("herb_wisdom_fruit", Rarity.LEGENDARY, Category.HERB, 0.0, Material.CHORUS_FRUIT, "Quả Trí Tuệ", "Vị Chua | Pha chế: +100% Nhận Kinh nghiệm", "Trái cấm của thần linh rơi rớt xuống trần gian.", "Mở mang tri thức vượt khỏi tầm hiểu biết của phàm nhân."));
        register(new ItemData("herb_dark_lotus", Rarity.LEGENDARY, Category.HERB, 0.0, Material.WITHER_ROSE, "Hắc Liên Hoa", "Vị Đắng | Pha chế: Độc Tử Thần", "Đóa sen đen ngòm thấm đẫm oán khí của vạn vật.", "Loài hoa biểu tượng của sát thủ bóng đêm."));
        register(new ItemData("herb_yggdrasil_sprout", Rarity.LEGENDARY, Category.HERB, 0.0, Material.OAK_SAPLING, "Mầm Cây Thế Giới Yggdrasil", "Vị Ngọt | Pha chế: Hồi sinh tại chỗ", "Chứa đựng sinh khí của cả một cõi thế giới.", "Truyền thuyết nói rằng thân cây mẹ của nó", "là cột trụ chống đỡ cả bầu trời."));

        // --- 16.1 CÁC LOẠI NƯỚC PHA CHẾ ---
        register(new ItemData("water_sweet", Rarity.COMMON, Category.MISC, 0.0, Material.POTION, "Nước Sông Tinh Khiết", "Vị Ngọt | Dùng để pha chế", "Lấy từ những dòng sông chảy xiết."));
        register(new ItemData("water_tasteless", Rarity.COMMON, Category.MISC, 0.0, Material.POTION, "Nước Hồ Tĩnh Lặng", "Vô vị | Dùng để pha chế", "Lấy từ những hồ nước ngọt yên ả."));
        register(new ItemData("water_salty", Rarity.COMMON, Category.MISC, 0.0, Material.POTION, "Nước Biển Mặn", "Vị Mặn | Dùng để pha chế", "Lấy từ đại dương mênh mông."));
        register(new ItemData("water_cave", Rarity.UNCOMMON, Category.MISC, 0.0, Material.POTION, "Nước Mạch Ngầm", "Vị Cay | Dùng để pha chế", "Chứa nhiều khoáng chất từ lòng đất sâu."));

        // --- ĐẠO CỤ DƯỢC SƯ ĐÃ ĐƯỢC CHỈNH SỬA ---
        // Thêm hiệu ứng GLOW (Enchant ẩn) cho Thìa bạc và Kính lúp để nhìn xịn hơn
        register(new ItemData("alch_silver_spoon", Rarity.RARE, Category.MISC, 0.0, Material.IRON_SHOVEL, "§fMuôi Trộn Bạc Cổ", "Tiêu hao: Giúp tăng 5% hiệu quả chế thuốc", "Được rèn từ bạc nguyên chất, dùng để khuấy vạc giúp tăng cường dược tính."));
        register(new ItemData("alch_magnifying_glass", Rarity.RARE, Category.MISC, 0.0, Material.GHAST_TEAR, "§fKính Lúp Thảo Mộc", "Tiêu hao: Giúp tăng thêm 50% tầm nhìn thảo dược", "Dùng để soi tìm các dược tính ẩn. Tăng cường Giác Quan Dược Sư lên tối đa."));
        register(new ItemData("alch_lost_recipe", Rarity.EPIC, Category.MISC, 0.0, Material.ENCHANTED_BOOK, "§d§lSách Công Thức Thất Truyền", "Sử dụng: Tiêu hao để mở khóa 1 công thức chế thuốc thất truyền.", "Ghi chép 1 trong 10 công thức luyện Thần Dược cổ đại."));

        // --- HỆ THỐNG BÌNH SỨ CÁCH Nhiệt (TÁCH THÀNH 4 LOẠI NƯỚC ĐỂ GIỮ NGŨ VỊ) ---
        register(new ItemData("alch_thermal_flask_water_empty", Rarity.UNCOMMON, Category.MISC, 0.0, Material.GLASS_BOTTLE, "§fBình Sứ Cách Nhiệt Rỗng", "Vô vị | Đạo cụ pha chế", "Dùng để nạp nước tự nhiên. Giảm 20% Độc tính khi nấu thuốc."));
        register(new ItemData("alch_thermal_flask_water_sweet", Rarity.UNCOMMON, Category.MISC, 0.0, Material.HONEY_BOTTLE, "§bBình Sứ: Nước Sông", "Vị Ngọt | Đạo cụ pha chế", "Giảm 20% Độc tính thành phẩm."));
        register(new ItemData("alch_thermal_flask_water_tasteless", Rarity.UNCOMMON, Category.MISC, 0.0, Material.POTION, "§bBình Sứ: Nước Hồ", "Vô vị | Đạo cụ pha chế", "Giảm 20% Độc tính thành phẩm."));
        register(new ItemData("alch_thermal_flask_water_salty", Rarity.UNCOMMON, Category.MISC, 0.0, Material.POTION, "§bBình Sứ: Nước Biển", "Vị Mặn | Đạo cụ pha chế", "Giảm 20% Độc tính thành phẩm."));
        register(new ItemData("alch_thermal_flask_water_cave", Rarity.UNCOMMON, Category.MISC, 0.0, Material.POTION, "§bBình Sứ: Nước Mạch Ngầm", "Vị Cay | Đạo cụ pha chế", "Giảm 20% Độc tính thành phẩm."));

        // ==========================================
        // CÁC LOẠI DUNG MÔI ĐẶC BIỆT (SOLVENT HERBS)
        // ==========================================

        // 1. Dung Môi Trung Hòa
        register(new ItemData("herb_solvent_dilution", Rarity.UNCOMMON, Category.HERB, 0.0, Material.SUGAR_CANE, "Cỏ Dịu Mát", "Vô vị | Dung môi: Tăng 20% Tỉ lệ | Giảm 20% Hiệu quả thuốc", "Loài cỏ tiết ra lớp nhựa làm chậm mọi phản ứng ma pháp."));

        // 2. Dung Môi Hỗn Mang
        register(new ItemData("herb_solvent_volatile", Rarity.RARE, Category.HERB, 0.0, Material.REDSTONE_TORCH, "Hoa Nổ Thạch Anh", "Vị Cay | Dung môi: Giảm 15% Tỉ lệ | Tăng 30% Hiệu quả thuốc)", "Nhiệt lượng sinh ra có thể làm nứt cả thành vạc."));

        // 3. Dung Môi Thanh Tẩy
        register(new ItemData("herb_solvent_purifying", Rarity.RARE, Category.HERB, 0.0, Material.LILY_PAD, "Rễ Nước Trong", "Vị Ngọt | Dung môi: Xóa bỏ Độc tính | Giảm 50% Thời gian duy trì)", "Lọc sạch mọi tạp chất, kể cả những tinh chất có lợi."));

        // 4. Dung Môi Ngưng Đọng
        register(new ItemData("herb_solvent_stasis", Rarity.EPIC, Category.HERB, 0.0, Material.BROWN_MUSHROOM, "Nấm Thời Gian", "Vị Đắng | Dung môi: Tăng 100% Thời gian duy trì | +10 Độc Tính)", "Ép cơ thể phải duy trì trạng thái buff bằng cách vắt kiệt sinh lực."));

        // 5. Dung Môi Lan Tỏa (Buff AoE)
        register(new ItemData("herb_solvent_resonance", Rarity.EPIC, Category.HERB, 0.0, Material.DANDELION, "Bồ Công Anh Gió", "Vị Chua | Dung môi: Buff 1 Đồng minh 5m, Giảm 30% Hiệu quả thuốc)", "Hoa bồ công anh mang theo dược lực nương theo chiều gió."));

        // 6. Dung Môi Song Sinh
        register(new ItemData("herb_solvent_twin", Rarity.MYTHIC, Category.HERB, 0.0, Material.WEEPING_VINES, "Dây Leo Bện Song Sinh", "Vô vị | Dung môi: Giảm 30% Tỉ lệ | x2 Sản lượng)", "Hình thái kỳ lạ, luôn luôn tồn tại song song hai thực thể."));

        // 7. Dung Môi Huyết Tế
        register(new ItemData("herb_solvent_sacrifice", Rarity.LEGENDARY, Category.HERB, 0.0, Material.CRIMSON_ROOTS, "Lá Máu Cuồng Nộ", "Vị Đắng | Dung môi: Tăng 100% Tỉ lệ | Rút 90% HP & Cấm Hồi Máu)", "Nở rộ trên xác của những ma thú tàn bạo nhất."));
        // --- 16.3 NGUYÊN LIỆU ĐẶC BIỆT ---

        register(new ItemData("essence_mana", Rarity.EPIC, Category.MISC, 0.0, Material.PRISMARINE_CRYSTALS, "§bTinh Chất Ma Lực", null, "Kết tinh từ sự giao thoa giữa thảo dược và ma lực vạc pha chế."));

        // ==========================================
        // --- 17. NGUYÊN LIỆU TRANG SỨC (TRINKET) ---
        // ==========================================
        register(new ItemData("misc_trinket_crystal", Rarity.EPIC, Category.MISC, 0.0, Material.AMETHYST_SHARD, "Tinh Thể Pha Lê", null, "Nguyên liệu dùng tại Bàn Giả Kim", "Có tác dụng nâng cấp hoặc ghép trang sức"));
        register(new ItemData("trinket_blank_ring", Rarity.RARE, Category.TOOL, 0.0, Material.GOLD_NUGGET, "Phôi Nhẫn Ma Thuật", null, "Khuôn đúc cơ bản để chế tác Nhẫn ngẫu nhiên."));
        register(new ItemData("trinket_blank_necklace", Rarity.RARE, Category.TOOL, 0.0, Material.END_CRYSTAL, "Phôi Dây Chuyền Ma Thuật", null, "Khuôn đúc cơ bản để chế tác Dây Chuyền ngẫu nhiên."));
        register(new ItemData("trinket_blank_bracelet", Rarity.RARE, Category.TOOL, 0.0, Material.STRING, "Phôi Vòng Tay Ma Thuật", null, "Khuôn đúc cơ bản để chế tác Vòng Tay ngẫu nhiên."));
        // ==========================================
        // --- 18. ĐỒ ĐỔI CLASS / ATTR ---
        // ==========================================
        register(new ItemData("class_change_scroll", Rarity.LEGENDARY, Category.MISC, 0.0, Material.ENCHANTED_BOOK,
                "Cuộn Giấy Chuyển Chức Nghiệp",
                "Mang trong túi đồ khi mở /class để đổi Chức Nghiệp",
                "Chứng thư hoàng gia cho phép thay đổi Chức Nghiệp.",
                "Giữ nguyên Cấp độ & Điểm Tiềm Năng hiện tại.",
                "Tự động tiêu hao 1 cuộn khi xác nhận đổi."));

        register(new ItemData("attr_change", Rarity.LEGENDARY, Category.MISC, 0.0, Material.PAPER,
                "Cuộn Giấy Tái Lập Chỉ Số",
                "Sử dụng trong /attr để hoàn trả 100% Điểm Tiềm Năng",
                "Khế ước cổ xưa đặt lại 5 chỉ số cơ bản về 0.",
                "Hoàn trả nguyên vẹn 100% số điểm đã cộng",
                "mà không bị hao hụt 20% như cách thông thường."));
    }

    private void register(ItemData data) {
        registry.put(data.id.toLowerCase(), data);
    }

    public ItemStack buildItem(String id, int amount) {
        ItemData data = registry.get(id.toLowerCase());
        if (data == null) return null;

        ItemStack item = new ItemStack(data.material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(data.rarity.color + "§l" + data.name);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, data.id);

        List<String> lore = new ArrayList<>();
        lore.add("§8[ Phân loại: " + data.category.type.displayName + " - " + data.category.displayName + " ]");

        // ==========================================
        // [HỆ THỐNG CẦN CÂU ĐỘNG] - ĐỔ XÚC XẮC CHỈ SỐ
        // ==========================================
        if (data.id.startsWith("tool_fishing_rod_") && !data.id.equals("tool_fishing_rod_1")) {
            generateDynamicRodStats(data, meta, lore);
        } else {
            // Đối với các Item bình thường
            if (data.points > 0 || (data.effect != null && !data.effect.isEmpty())) lore.add("");
            if (data.points > 0) {
                lore.add("§fThuộc tính Nguyên liệu:");
                lore.add("  §e+" + data.points + " §7Điểm " + data.category.displayName);
            }
            if (data.effect != null && !data.effect.isEmpty()) {
                if (data.points > 0) lore.add("");
                lore.add("§fHiệu quả Đặc biệt:");
                lore.add("§f  ➥ §a" + data.effect);
            }
            lore.add("§8§m                                ");
            if (data.description != null && !data.description.isEmpty()) {
                for (String line : data.description) lore.add("§7" + line);
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    public List<String> getAllIds() { return new ArrayList<>(registry.keySet()); }
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }
    public ItemData getItemData(String id) {
        if (id == null) return null;
        return registry.get(id.toLowerCase());
    }

    public boolean isMatch(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null || item1.getType() == Material.AIR) return false;
        String id1 = getItemId(item1);
        String id2 = getItemId(item2);
        if (id1 != null && id2 != null) return id1.equalsIgnoreCase(id2);
        return item1.isSimilar(item2);
    }

    public int countItem(org.bukkit.entity.Player player, ItemStack template) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMatch(item, template)) count += item.getAmount();
        }
        return count;
    }

    public boolean takeItem(org.bukkit.entity.Player player, ItemStack template, int amountToRemove) {
        if (countItem(player, template) < amountToRemove) return false;
        org.bukkit.inventory.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isMatch(item, template)) {
                int amt = item.getAmount();
                if (amt > amountToRemove) {
                    item.setAmount(amt - amountToRemove);
                    return true;
                } else {
                    inv.setItem(i, null);
                    amountToRemove -= amt;
                    if (amountToRemove <= 0) return true;
                }
            }
        }
        return true;
    }

    // [CƠ CHẾ RANDOM] Tự động sinh dòng thuộc tính và Độ bền
    private void generateDynamicRodStats(ItemData data, ItemMeta meta, List<String> lore) {
        int buffCount = 1;
        int duraMin = 300, duraMax = 400;
        double statMin = 5.0, statMax = 12.0;

        switch (data.rarity) {
            case UNCOMMON: buffCount = 1; statMin = 10.0; statMax = 18.0; duraMin = 400; duraMax = 600; break;
            case RARE: buffCount = 2; statMin = 16.0; statMax = 24.0; duraMin = 600; duraMax = 800; break;
            case EPIC: buffCount = 2; statMin = 20.0; statMax = 30.0; duraMin = 800; duraMax = 1000; break;
            case MYTHIC: buffCount = 3; statMin = 25.0; statMax = 40.0; duraMin = 1000; duraMax = 1200; break;
            case LEGENDARY: buffCount = 3; statMin = 35.0; statMax = 50.0; duraMin = 1200; duraMax = 1500; break;
            default: break;
        }

        int maxDura = duraMin + rand.nextInt(duraMax - duraMin + 1);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "rod_durability"), PersistentDataType.INTEGER, maxDura);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "rod_max_durability"), PersistentDataType.INTEGER, maxDura);

        lore.add("");
        lore.add("§7Độ bền: §f" + maxDura + " §8lần câu");
        lore.add("");
        lore.add("§f§lThuộc tính Cần Câu:");

        String[] availableBuffs = {"bite_time", "big_fish", "treasure", "trash", "tolerance", "second_chance", "auto_catch", "unbreaking"};
        List<String> chosenBuffs = new ArrayList<>(Arrays.asList(availableBuffs));
        Collections.shuffle(chosenBuffs);

        for (int i = 0; i < buffCount; i++) {
            String buff = chosenBuffs.get(i);
            double value = statMin + (rand.nextDouble() * (statMax - statMin));

            NamespacedKey key = new NamespacedKey(plugin, "rod_" + buff);

            switch (buff) {
                case "bite_time":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §a-" + String.format("%.1f", value) + "% Thời gian cá cắn câu"); break;
                case "big_fish":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §c+" + String.format("%.1f", value) + "% Tỉ lệ Thủy Quái"); break;
                case "treasure":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §6+" + String.format("%.1f", value) + "% Tỉ lệ Kho báu"); break;
                case "trash":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §7-" + String.format("%.1f", value) + "% Tỉ lệ dính Rác"); break;
                case "tolerance":
                    int tolValue = (int) (value * 10); // Transform % to ms (eg 10% = 100ms)
                    meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, tolValue);
                    lore.add("§f ➥ §e+" + tolValue + "ms Dung sai nhịp điệu"); break;
                case "second_chance":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §a+" + String.format("%.1f", value) + "% Cơ hội hồi sinh dây"); break;
                case "auto_catch":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §b+" + String.format("%.1f", value) + "% Tự động bắt cá"); break;
                case "unbreaking":
                    meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                    lore.add("§f ➥ §d+" + String.format("%.1f", value) + "% Không tiêu hao độ bền"); break;
            }
        }
        lore.add("§8§m                                ");
        for (String line : data.description) lore.add("§7" + line);
    }
}