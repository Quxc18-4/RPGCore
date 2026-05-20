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
        MONSTER(ItemType.MATERIAL, "Rớt từ quái"), MAGIC(ItemType.MATERIAL, "Ma thuật"),
        MISC(ItemType.MATERIAL, "Khác"), COIN(ItemType.CURRENCY, "Tiền kim loại");

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
        register(new ItemData("meat_rabbit", Rarity.COMMON, Category.MEAT, 0.25, Material.RABBIT, "Thịt Thỏ Đồng", null, "Thịt nạc, ít mỡ."));
        register(new ItemData("meat_wolf_bone", Rarity.COMMON, Category.MEAT, 0.25, Material.BONE, "Xương Sói", "Nước hầm xương cung cấp 0.25 Điểm Gia vị", "Đoạn xương chứa rất nhiều tủy."));

        // --- 2. NHÓM HẢI SẢN ---
        register(new ItemData("fish_cod_small", Rarity.COMMON, Category.FISH, 0.25, Material.COD, "Cá Tuyết Nhỏ", null, "Một con cá tuyết bé xíu."));
        register(new ItemData("fish_cod_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.COD, "Cá Tuyết Vừa", null, "Kích cỡ tiêu chuẩn."));
        register(new ItemData("fish_cod_large", Rarity.RARE, Category.FISH, 1.0, Material.COD, "Cá Tuyết Lớn", null, "Con cá khổng lồ, rất nhiều thịt."));
        register(new ItemData("fish_salmon_small", Rarity.COMMON, Category.FISH, 0.25, Material.SALMON, "Cá Hồi Nhỏ", null, "Cá hồi non."));
        register(new ItemData("fish_salmon_med", Rarity.UNCOMMON, Category.FISH, 0.5, Material.SALMON, "Cá Hồi Vừa", null, "Cá hồi tươi ngon."));
        register(new ItemData("fish_salmon_large", Rarity.RARE, Category.FISH, 1.0, Material.SALMON, "Cá Hồi Lớn", null, "Thịt cá hồi hảo hạng."));
        register(new ItemData("fish_tropical", Rarity.UNCOMMON, Category.FISH, 0.5, Material.TROPICAL_FISH, "Cá Hề Biển Sâu", null, "Màu sắc sặc sỡ."));

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
        register(new ItemData("veg_melon", Rarity.COMMON, Category.VEG, 0.5, Material.MELON_SLICE, "Dưa Hấu Mọng Nước", "Dùng ngay: Hồi 2% HP và 1 Điểm đói", "Ngọt ngào và giải khát."));
        register(new ItemData("veg_melon_uncommon", Rarity.UNCOMMON, Category.VEG, 0.75, Material.MELON_SLICE, "Dưa Hấu Đột Biến", "Dùng ngay: Hồi 3% HP và 2 Điểm đói", "Miếng dưa rất ngọt và ít hạt."));
        register(new ItemData("veg_melon_rare", Rarity.RARE, Category.VEG, 1.0, Material.MELON_SLICE, "Dưa Hấu Khổng Lồ", "Dùng ngay: Hồi 5% HP và 3 Điểm đói", "Đỏ au và mọng nước tột độ."));
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
        register(new ItemData("veg_apple", Rarity.COMMON, Category.VEG, 0.5, Material.APPLE, "Táo Đỏ", "Dùng ngay: Hồi 2% HP và 1 Điểm đói", "Táo rừng chín mọng."));

        // --- 4. NHÓM TINH BỘT ---
        register(new ItemData("starch_bread", Rarity.COMMON, Category.STARCH, 1.0, Material.BREAD, "Ổ Bánh Mì Nướng", "Dùng ngay: Hồi 5 Điểm đói", "Thơm mùi lúa mạch."));
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
        register(new ItemData("magic_fermented_eye", Rarity.RARE, Category.MAGIC, 0.75, Material.FERMENTED_SPIDER_EYE, "Mắt Nhện Lên Men", "Cung cấp hiệu ứng Hồi Phục Cao", "Mùi vị cực kỳ kinh tởm."));
        register(new ItemData("magic_pufferfish", Rarity.RARE, Category.MAGIC, 0.75, Material.PUFFERFISH, "Cá Nóc Kịch Độc", "Cung cấp hiệu ứng Thở Dưới Nước", "Phải biết cách sơ chế mới dùng được."));
        register(new ItemData("magic_spider_eye", Rarity.UNCOMMON, Category.MAGIC, 0.75, Material.SPIDER_EYE, "Mắt Nhện Tinh Anh", "Dùng để hạ độc thức ăn.", "Trông khá tởm, nhưng chứa đầy chất độc."));
        register(new ItemData("magic_bat_wing", Rarity.RARE, Category.MAGIC, 0.75, Material.POTION, "Huyết Tinh", "Cung cấp thêm 10% tỉ lệ Hút Máu", "Máu của sinh vật bóng tối."));
        register(new ItemData("magic_glowstone", Rarity.RARE, Category.MAGIC, 0.75, Material.GLOWSTONE_DUST, "Bột Tiên", "Cung cấp 10% tỉ lệ Chí Mạng", "Bột phát sáng lấp lánh."));
        register(new ItemData("magic_goblin_ear", Rarity.RARE, Category.MAGIC, 0.75, Material.TURTLE_SCUTE, "Tai Goblin", "Cung cấp 10% Tỉ lệ rớt đồ", "Một chiếc tai còn rỏ máu của Goblin."));
        register(new ItemData("magic_toxic_blood", Rarity.UNCOMMON, Category.MAGIC, 0.75, Material.SLIME_BALL, "Máu Kịch Độc", "Dùng để hạ độc thức ăn.", "Thứ chất lỏng đặc sệt bốc mùi hôi thối."));

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
        register(new ItemData("misc_poison_stinger", Rarity.RARE, Category.MISC, 0.0, Material.ARROW, "Ngòi Ong Độc", null, "Cực kỳ sắc bén, cẩn thận kẻo đứt tay."));
        register(new ItemData("misc_armor_shard", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Mảnh Giáp Vụn", null, "Có thể nung chảy để tái chế tại Lò Rèn."));
        register(new ItemData("misc_soft_string", Rarity.COMMON, Category.MISC, 0.0, Material.STRING, "Sợi Tơ Mềm", null, "Một cuộn tơ tằm mềm mại."));
        register(new ItemData("misc_steel_string", Rarity.UNCOMMON, Category.MISC, 0.0, Material.STRING, "Sợi Tơ Thép", null, "Cứng cáp lạ thường, cực kỳ bền bỉ."));
        register(new ItemData("misc_wolf_fang", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Nanh Sói", null, "Một chiếc nanh vô cùng sắc nhọn."));
        register(new ItemData("misc_leather_pouch_piece", Rarity.COMMON, Category.MISC, 0.0, Material.ARMADILLO_SCUTE, "Mảnh Túi Da Đạo Tặc", null, "Một mảnh da cắt ra từ túi của bọn Đạo Tặc."));

        register(new ItemData("misc_blacksmith_scrap", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_NUGGET, "Mạt Thợ Rèn", null, "Những mảnh vụn ma thuật xót lại khi đập đồ thất bại.", "Tích lũy đủ số lượng, chúng mang sức mạnh", "thần bí giúp đột phá trang bị thành công."));
        register(new ItemData("misc_job_change_card", Rarity.EPIC, Category.MISC, 0.0, Material.NAME_TAG, "Thẻ Học Nghề", null, "Giấy phép đặc biệt từ Hiệp hội Nghề nghiệp.", "Sẽ tự động tiêu hao khi bạn chọn đổi nghề."));
        register(new ItemData("misc_protection_charm", Rarity.EPIC, Category.MISC, 0.0, Material.PAPER, "Bùa Bảo Vệ Trang Bị", null, "Tự động tiêu hao khi rèn thất bại", "để ngăn chặn việc rớt 1 cấp độ."));
        register(new ItemData("misc_lucky_hammer", Rarity.RARE, Category.MISC, 0.0, Material.GOLDEN_AXE, "Búa Rèn May Mắn", null, "Để trong túi đồ, tự động tiêu hao", "để tăng thêm tỉ lệ thành công khi rèn."));

        // --- 10. TÚI GACHA ---
        register(new ItemData("bag_weapon_common", Rarity.COMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Thường", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Common.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_uncommon", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Hiếm", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Uncommon.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_rare", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Tuyệt Đỉnh", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Rare.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_weapon_epic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Vũ Khí Sử Thi", null, "Chuột phải để mở nhận 1 Vũ khí Phẩm chất Epic.", "Cấp độ vũ khí bằng với cấp của bạn."));
        register(new ItemData("bag_armor_common", Rarity.COMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Thường", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Common.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_uncommon", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Hiếm", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Uncommon.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_rare", Rarity.RARE, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Tuyệt Đỉnh", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Rare.", "Cấp độ giáp bằng với cấp của bạn."));
        register(new ItemData("bag_armor_epic", Rarity.EPIC, Category.MISC, 0.0, Material.BUNDLE, "Túi Phòng Cụ Sử Thi", null, "Chuột phải để mở nhận 1 Giáp Phẩm chất Epic.", "Cấp độ giáp bằng với cấp của bạn."));

        // --- 11. TINH THỂ ---
        register(new ItemData("crystal_fire", Rarity.EPIC, Category.MISC, 0.0, Material.BLAZE_POWDER, "Ngọn Lửa Khởi Nguyên", "Thức tỉnh Nguyên Tố Lửa", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_water", Rarity.EPIC, Category.MISC, 0.0, Material.HEART_OF_THE_SEA, "Nước Mắt Nữ Thần", "Thức tỉnh Nguyên Tố Nước", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_nature", Rarity.EPIC, Category.MISC, 0.0, Material.OAK_SAPLING, "Mầm Cây Thế Giới", "Thức tỉnh Nguyên Tố Tự Nhiên", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_wind", Rarity.EPIC, Category.MISC, 0.0, Material.WIND_CHARGE, "Hơi Tàn Của Tinh Linh Gió", "Thức tỉnh Nguyên Tố Gió", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_light", Rarity.EPIC, Category.MISC, 0.0, Material.NETHER_STAR, "Hào Quang Ánh Sáng", "Thức tỉnh Nguyên Tố Ánh Sáng", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));
        register(new ItemData("crystal_dark", Rarity.EPIC, Category.MISC, 0.0, Material.ECHO_SHARD, "Tinh Thể Hỗn Độn", "Thức tỉnh Nguyên Tố Bóng Tối", "Vật phẩm bí ẩn chứa đựng sức mạnh nguyên tố."));

        // --- 12. ĐẦU BẾP & TINH TÚY ẨM THỰC ---
        register(new ItemData("misc_culinary_essence", Rarity.EPIC, Category.MISC, 0.0, Material.GLOWSTONE_DUST, "Tinh Túy Ẩm Thực", null, "Kết tinh từ những món ăn tuyệt hảo.", "Được giới sành ăn săn lùng gắt gao."));
        register(new ItemData("potion_digestion", Rarity.RARE, Category.MISC, 0.0, Material.POTION, "Thuốc Giải Ngấy", "Dùng ngay: Giảm 50% lượng điểm Tuyệt thực", "Công thức bí truyền của Hội Đầu Bếp."));
        register(new ItemData("potion_anti_anorexia", Rarity.EPIC, Category.MISC, 0.0, Material.DRAGON_BREATH, "Thuốc Trị Tuyệt Thực", "Dùng ngay: Chữa khỏi hoàn toàn chứng biếng ăn", "Uống vào là thèm ăn ngay!"));
        register(new ItemData("misc_lunch_bag", Rarity.RARE, Category.MISC, 0.0, Material.PIGLIN_BANNER_PATTERN, "Túi Đồ Ăn Dự Phòng", "Dùng ngay: Mở ra 3 bát súp ngẫu nhiên", "Chuẩn bị sẵn cho những chuyến đi dài."));
        register(new ItemData("misc_preservative_powder", Rarity.MYTHIC, Category.MISC, 0.0, Material.GUNPOWDER, "Bột Bảo Quản Thần Kỳ", "Chặn 100% nguy cơ hỏng của món ăn", "Một loại bột sáng lấp lánh..."));

        // --- 13. NÔNG CỤ & HẠT GIỐNG (FARMING SYSTEM) ---
        // Nông cụ chuyên dụng
        register(new ItemData("tool_harvest_hoe", Rarity.UNCOMMON, Category.MISC, 0.0, Material.IRON_HOE, "Cuốc Thu Hoạch", "Click phải để thu hoạch nông sản", "Nông cụ chuyên dụng giúp thu hồi nông sản", "và hạt giống mà không làm hỏng rễ cây."));
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
        register(new ItemData("tool_fishing_rod_1", Rarity.UNCOMMON, Category.MISC, 0.0, Material.FISHING_ROD, "Cần Câu Gỗ Tập Sự", "Kích hoạt Minigame Câu Cá", "Cần câu được thiết kế đặc biệt."));

        // ==========================================
        // 14.1 MỒI NỀN (BASE BAITS) - Có giảm hiệu suất nếu > 3
        // ==========================================
        // Mọi nơi (ALL)
        register(new ItemData("bait_base_earthworm", Rarity.COMMON, Category.MISC, 0.0, Material.BROWN_DYE, "Giun Đất", "Giảm 5% thời gian chờ", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_minnow", Rarity.COMMON, Category.MISC, 0.0, Material.TROPICAL_FISH, "Cá Mồi Nhỏ", "Tăng 5% tỉ lệ cá khổng lồ", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_shiny_pebble", Rarity.RARE, Category.MISC, 0.0, Material.PRISMARINE_CRYSTALS, "Sỏi Lấp Lánh", "Tăng 2% tỉ lệ Kho Báu", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_sludge", Rarity.COMMON, Category.MISC, 0.0, Material.CLAY_BALL, "Bùn Thối", "Tăng 10% tỉ lệ câu Rác", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_corn", Rarity.UNCOMMON, Category.MISC, 0.0, Material.GOLD_NUGGET, "Hạt Bắp", "Giảm 5% tỉ lệ câu Rác", "Tác dụng: Mọi môi trường."));
        register(new ItemData("bait_base_sticky_sap", Rarity.UNCOMMON, Category.MISC, 0.0, Material.SLIME_BALL, "Nhựa Cây Dính", "Tăng 50ms dung sai nhịp", "Tác dụng: Mọi môi trường."));

        // Biển (OCEAN)
        register(new ItemData("bait_base_sandworm", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BONE_MEAL, "Giun Cát", "Giảm 10% thời gian chờ", "Tác dụng: CHỈ Ở BIỂN."));
        register(new ItemData("bait_base_squid_meat", Rarity.UNCOMMON, Category.MISC, 0.0, Material.GLOW_INK_SAC, "Thịt Mực", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: CHỈ Ở BIỂN."));
        register(new ItemData("bait_base_broken_shell", Rarity.RARE, Category.MISC, 0.0, Material.NAUTILUS_SHELL, "Vỏ Sò Vỡ", "Tăng 5% tỉ lệ Kho Báu", "Tác dụng: CHỈ Ở BIỂN."));
        register(new ItemData("bait_base_dead_kelp", Rarity.COMMON, Category.MISC, 0.0, Material.DRIED_KELP, "Rong Biển Chết", "Tăng 20% tỉ lệ câu Rác", "Tác dụng: CHỈ Ở BIỂN."));

        // Sông/Hồ (RIVER)
        register(new ItemData("bait_base_leech", Rarity.UNCOMMON, Category.MISC, 0.0, Material.INK_SAC, "Đỉa Sông", "Giảm 10% thời gian chờ", "Tác dụng: CHỈ Ở SÔNG/HỒ."));
        register(new ItemData("bait_base_loach", Rarity.UNCOMMON, Category.MISC, 0.0, Material.COD, "Cá Lòng Tong", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: CHỈ Ở SÔNG/HỒ."));
        register(new ItemData("bait_base_rusty_coin", Rarity.RARE, Category.MISC, 0.0, Material.IRON_NUGGET, "Đồng Xu Gỉ", "Tăng 5% tỉ lệ Kho Báu", "Tác dụng: CHỈ Ở SÔNG/HỒ."));
        register(new ItemData("bait_base_moss", Rarity.COMMON, Category.MISC, 0.0, Material.MOSS_BLOCK, "Rêu Ẩm Ướt", "Tăng 20% tỉ lệ câu Rác", "Tác dụng: CHỈ Ở SÔNG/HỒ."));

        // Hang động ngầm (CAVE)
        register(new ItemData("bait_base_blindworm", Rarity.UNCOMMON, Category.MISC, 0.0, Material.STRING, "Giun Mù", "Giảm 10% thời gian chờ", "Tác dụng: CHỈ Ở HANG ĐỘNG."));
        register(new ItemData("bait_base_blind_crab", Rarity.UNCOMMON, Category.MISC, 0.0, Material.SPIDER_EYE, "Cua Mù", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: CHỈ Ở HANG ĐỘNG."));
        register(new ItemData("bait_base_stalactite", Rarity.RARE, Category.MISC, 0.0, Material.POINTED_DRIPSTONE, "Mảnh Thạch Nhũ", "Tăng 5% tỉ lệ Kho Báu", "Tác dụng: CHỈ Ở HANG ĐỘNG."));
        register(new ItemData("bait_base_mold", Rarity.UNCOMMON, Category.MISC, 0.0, Material.BROWN_MUSHROOM, "Nấm Mốc Độc", "Giảm 10% tỉ lệ câu Rác", "Tác dụng: CHỈ Ở HANG ĐỘNG."));

        // Đầm lầy (SWAMP)
        register(new ItemData("bait_base_swamp_snail", Rarity.UNCOMMON, Category.MISC, 0.0, Material.NAUTILUS_SHELL, "Ốc Sên Lầy", "Giảm 10% thời gian chờ", "Tác dụng: CHỈ Ở ĐẦM LẦY."));
        register(new ItemData("bait_base_small_frog", Rarity.UNCOMMON, Category.MISC, 0.0, Material.SLIME_BALL, "Ếch Nhỏ", "Tăng 10% tỉ lệ cá khổng lồ", "Tác dụng: CHỈ Ở ĐẦM LẦY."));

        // ==========================================
        // 14.2. MỒI BUFF (MODIFIERS) - Không bị giảm hiệu suất
        // ==========================================
        // Buff Mọi nơi (ALL)
        register(new ItemData("bait_buff_pure_water", Rarity.UNCOMMON, Category.MISC, 0.0, Material.GHAST_TEAR, "Nước Tinh Khiết", "Giảm 30% tỉ lệ câu Rác", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_sonar", Rarity.EPIC, Category.MISC, 0.0, Material.COMPASS, "Thiết Bị Dò Cá", "Tăng 15% cá khổng lồ", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_magnet", Rarity.EPIC, Category.MISC, 0.0, Material.IRON_INGOT, "Lõi Từ Tính", "Tăng 10% Kho báu", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_metronome", Rarity.RARE, Category.MISC, 0.0, Material.CLOCK, "Con Lắc Nhịp", "Tăng 150ms dung sai nhịp", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_phoenix", Rarity.LEGENDARY, Category.MISC, 0.0, Material.FEATHER, "Lông Phượng Hoàng", "Tăng 50% cơ hội câu lại", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_lazy_charm", Rarity.RARE, Category.MISC, 0.0, Material.PAPER, "Bùa Lười Biếng", "20% Bỏ qua Minigame", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_wind_glyph", Rarity.RARE, Category.MISC, 0.0, Material.FEATHER, "Phù Hiệu Gió", "Giảm 20% thời gian chờ", "Phụ gia mồi câu."));
        register(new ItemData("bait_buff_ancient_bone", Rarity.EPIC, Category.MISC, 0.0, Material.BONE, "Xương Cổ Đại", "Tăng 300ms dung sai nhịp", "Phụ gia mồi câu."));

        // Buff Tối Thượng (Ultimate - Mọi nơi)
        register(new ItemData("bait_buff_monster_blood", Rarity.MYTHIC, Category.MISC, 0.0, Material.REDSTONE, "Máu Vực Thẳm", "Chắc chắn gặp Thủy Quái", "Đòi hỏi Kỹ năng Tối Thượng."));
        register(new ItemData("bait_buff_golden_lure", Rarity.LEGENDARY, Category.MISC, 0.0, Material.RAW_GOLD, "Lưỡi Câu Vàng", "Chắc chắn x3 sản lượng", "Đòi hỏi Kỹ năng Tối Thượng."));
        register(new ItemData("bait_buff_pirate_coin", Rarity.MYTHIC, Category.MISC, 0.0, Material.SUNFLOWER, "Đồng Xu Hải Tặc", "Chắc chắn câu được Rương", "Đòi hỏi Kỹ năng Tối Thượng."));
        register(new ItemData("bait_buff_lightning", Rarity.MYTHIC, Category.MISC, 0.0, Material.GLOWSTONE_DUST, "Tinh Hoa Sấm Sét", "Cá cắn câu ngay lập tức", "Đòi hỏi Kỹ năng Tối Thượng."));

        // Buff Đặc quyền Môi trường (Cực mạnh)
        register(new ItemData("bait_buff_glowing_oil", Rarity.EPIC, Category.MISC, 0.0, Material.GLOW_INK_SAC, "Dầu Cá Phát Quang", "Tăng 30% cá khổng lồ", "Tác dụng: CHỈ Ở HANG ĐỘNG."));
        register(new ItemData("bait_buff_coral_powder", Rarity.EPIC, Category.MISC, 0.0, Material.BONE_MEAL, "Bột San Hô", "Tăng 20% Kho Báu", "Tác dụng: CHỈ Ở BIỂN."));
        register(new ItemData("bait_buff_freshwater_crystal", Rarity.EPIC, Category.MISC, 0.0, Material.PRISMARINE_CRYSTALS, "Tinh Thể Nước Ngọt", "Giảm 40% thời gian chờ", "Tác dụng: CHỈ Ở SÔNG/HỒ."));
        register(new ItemData("bait_buff_spider_venom", Rarity.EPIC, Category.MISC, 0.0, Material.SPIDER_EYE, "Nọc Nhện Hang", "Giảm 40% thời gian chờ", "Tác dụng: CHỈ Ở HANG ĐỘNG."));
        register(new ItemData("bait_buff_mutant_lilypad", Rarity.EPIC, Category.MISC, 0.0, Material.LILY_PAD, "Hoa Súng Đột Biến", "Tăng 30% cá khổng lồ", "Tác dụng: CHỈ Ở ĐẦM LẦY."));
        register(new ItemData("bait_buff_mermaid_tear", Rarity.LEGENDARY, Category.MISC, 0.0, Material.GHAST_TEAR, "Nước Mắt Nhân Ngư", "Tăng 100% cơ hội câu lại", "Tác dụng: CHỈ Ở BIỂN."));
        register(new ItemData("bait_buff_river_demon_eye", Rarity.MYTHIC, Category.MISC, 0.0, Material.ENDER_EYE, "Mắt Quỷ Sông", "Chắc chắn gặp Thủy Quái", "Tác dụng: CHỈ Ở SÔNG/HỒ."));
        register(new ItemData("bait_buff_swamp_gas", Rarity.EPIC, Category.MISC, 0.0, Material.DRAGON_BREATH, "Khí Đầm Lầy", "50% Bỏ qua Minigame", "Tác dụng: CHỈ Ở ĐẦM LẦY."));

        // ==========================================
        // 14.3. THÀNH PHẨM (CUSTOM BAIT)
        // ==========================================
        register(new ItemData("bait_custom", Rarity.EPIC, Category.MISC, 0.0, Material.MAGMA_CREAM, "§d§lMồi Câu Đặc Chế", "Cầm ở tay phụ để sử dụng.", "Hỗn hợp pha trộn hoàn hảo."));
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

        List<String> lore = new ArrayList<>();

        // [CẬP NHẬT 1]: Phân loại MỒI NỀN và MỒI BUFF rõ ràng trong Lore
        if (id.startsWith("bait_base_")) {
            lore.add("§8[ §fPhân loại: §eMỒI NỀN §8]");
        } else if (id.startsWith("bait_buff_")) {
            lore.add("§8[ §fPhân loại: §dMỒI BUFF §8]");
        } else if (id.equals("bait_custom")) {
            // Không set phân loại ở đây cho bait_custom, vì BaitManager sẽ set sau
        } else {
            lore.add("§8[ Phân loại: " + data.category.type.displayName + " - " + data.category.displayName + " ]");
        }

        boolean hasAttributes = (data.points > 0) || (data.effect != null && !data.effect.isEmpty());
        if (hasAttributes) lore.add("");

        if (hasAttributes) {
            if (data.points > 0) {
                lore.add("§fThuộc tính Nguyên liệu:");
                lore.add("  §e+" + data.points + " §7Điểm " + data.category.displayName);
            }
            if (data.points > 0 && data.effect != null && !data.effect.isEmpty()) lore.add("");
            if (data.effect != null && !data.effect.isEmpty()) {
                lore.add("§fHiệu quả Đặc biệt:");
                lore.add("§f  ➥ §a" + data.effect);
            }
        }

        lore.add("§8§m                                ");

        if (data.description != null && !data.description.isEmpty()) {
            for (String line : data.description) lore.add("§7" + line);
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, data.id);
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
}