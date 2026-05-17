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

        // --- 3. NHÓM RAU QUẢ ---
        register(new ItemData("veg_pumpkin", Rarity.COMMON, Category.VEG, 1.0, Material.PUMPKIN, "Bí Ngô Nguyên Quả", null, "Trái bí ngô to tròn."));
        register(new ItemData("veg_melon", Rarity.COMMON, Category.VEG, 0.5, Material.MELON_SLICE, "Dưa Hấu Mọng Nước", "Dùng ngay: Hồi 2% HP và 1 Điểm đói", "Ngọt ngào và giải khát."));
        register(new ItemData("veg_carrot", Rarity.COMMON, Category.VEG, 0.5, Material.CARROT, "Cà Rốt Ngọt", null, "Tốt cho mắt."));
        register(new ItemData("veg_beetroot", Rarity.COMMON, Category.VEG, 0.5, Material.BEETROOT, "Củ Dền Đỏ", null, "Dùng để nấu canh rất ngon."));
        register(new ItemData("veg_apple", Rarity.COMMON, Category.VEG, 0.5, Material.APPLE, "Táo Đỏ", "Dùng ngay: Hồi 2% HP và 1 Điểm đói", "Táo rừng chín mọng."));        register(new ItemData("veg_mushroom", Rarity.COMMON, Category.VEG, 0.25, Material.BROWN_MUSHROOM, "Nấm Hương", null, "Thơm lừng khi xào."));
        register(new ItemData("veg_berries", Rarity.COMMON, Category.VEG, 0.25, Material.SWEET_BERRIES, "Quả Mọng Rừng", null, "Vị chua chua ngọt ngọt."));
        register(new ItemData("veg_kelp_fresh", Rarity.COMMON, Category.VEG, 0.25, Material.KELP, "Rong Biển Tươi", null, "Vớt từ dưới đáy biển lên."));
        register(new ItemData("veg_kelp_dried", Rarity.COMMON, Category.VEG, 0.25, Material.DRIED_KELP, "Rong Biển Khô", "Dùng ngay: Tăng 15% Tốc độ chạy trong 5 giây", "Đã được sấy khô để bảo quản."));
        register(new ItemData("veg_potato", Rarity.COMMON, Category.VEG, 0.5, Material.POTATO, "Khoai Tây Vàng", "Cung cấp thêm 0.5 Điểm Tinh bột", "Củ khoai tây bùi béo."));

        // --- 4. NHÓM TINH BỘT ---
        register(new ItemData("starch_bread", Rarity.COMMON, Category.STARCH, 1.0, Material.BREAD, "Ổ Bánh Mì Nướng", "Dùng ngay: Hồi 5 Điểm đói", "Thơm mùi lúa mạch."));
        register(new ItemData("starch_flour", Rarity.COMMON, Category.STARCH, 0.25, Material.SUGAR, "Bột Mì Thượng Hạng", null, "Xay nhuyễn từ lúa mì ngon nhất."));

        // --- 5. NHÓM GIA VỊ (XÓA CÁC EFFECT ẢO) ---
        register(new ItemData("spice_salt", Rarity.COMMON, Category.SPICE, 0.5, Material.WHITE_DYE, "Muối Biển", null, "Gia vị không thể thiếu."));
        register(new ItemData("spice_pepper", Rarity.COMMON, Category.SPICE, 0.5, Material.GUNPOWDER, "Tiêu Đen", null, "Cay nồng."));
        register(new ItemData("spice_chili", Rarity.COMMON, Category.SPICE, 0.5, Material.NETHER_WART, "Bột Ớt Hiểm", null, "Cực kỳ cay."));
        register(new ItemData("spice_garlic", Rarity.COMMON, Category.SPICE, 0.5, Material.PUMPKIN_SEEDS, "Tỏi Bóc Vỏ", null, "Dùng để phi thơm."));
        register(new ItemData("spice_sugar", Rarity.COMMON, Category.SPICE, 0.5, Material.SUGAR, "Đường Tinh Luyện", null, "Ngọt ngào."));
        register(new ItemData("spice_honey", Rarity.UNCOMMON, Category.SPICE, 1.0, Material.HONEY_BOTTLE, "Mật Ong Rừng", "Dùng ngay: Hồi phục 5% Máu tối đa", "Mật đặc sánh và cực kỳ ngọt ngào."));
        register(new ItemData("spice_sour", Rarity.COMMON, Category.SPICE, 0.5, Material.POTION, "Giấm Chua", null, "Tạo vị chua thanh."));
        register(new ItemData("spice_all_in_one", Rarity.EPIC, Category.SPICE, 0.5, Material.BROWN_DYE, "Gia Vị Tổng Hợp", "Cung cấp 0.5 điểm cho toàn bộ gia vị", "Sự kết hợp hoàn hảo của mặn, ngọt, chua, cay."));

        // --- 6. NHÓM CHẤT BÉO (XÓA CÁC EFFECT ẢO) ---
        register(new ItemData("fat_egg", Rarity.COMMON, Category.FAT, 0.5, Material.EGG, "Trứng Gà Tươi", null, "Một quả trứng vừa mới đẻ."));
        register(new ItemData("fat_sniffer", Rarity.RARE, Category.FAT, 0.75, Material.SNIFFER_EGG, "Trứng Cổ Đại", null, "Toát ra một thứ hào quang cổ đại."));
        register(new ItemData("fat_milk", Rarity.UNCOMMON, Category.FAT, 1.0, Material.MILK_BUCKET, "Sữa Bò Béo Ngậy", "Dùng ngay: Xóa bỏ 1 Trạng thái xấu ngẫu nhiên", "Sữa vắt từ bò hoang dã, rất thơm."));
        register(new ItemData("fat_lard", Rarity.UNCOMMON, Category.FAT, 0.5, Material.SNOWBALL, "Mỡ Lợn", null, "Dùng để chiên xào cực kỳ thơm."));

        // --- 7. NHÓM QUÁI VẬT ---
        register(new ItemData("monster_flesh", Rarity.COMMON, Category.MONSTER, 1.0, Material.ROTTEN_FLESH, "Thịt Thối Rữa", "Nguy cơ làm hỏng món ăn rất cao!", "Thịt của quái vật đột biến, không thể ăn được."));

        // --- 8. NHÓM MA THUẬT (GIỮ LẠI EFFECT VÌ CÓ CODE XỬ LÝ) ---
        register(new ItemData("magic_golden_carrot", Rarity.RARE, Category.MAGIC, 0.75, Material.GOLDEN_CARROT, "Cà Rốt Ma Thuật", "Cung cấp hiệu ứng Mắt Mèo", "Trồng bằng phép thuật."));
        register(new ItemData("magic_blaze_powder", Rarity.RARE, Category.MAGIC, 0.75, Material.BLAZE_POWDER, "Mảnh Quỷ Lửa", "Cung cấp hiệu ứng Kháng Lửa", "Nóng rực trên tay."));
        register(new ItemData("magic_fermented_eye", Rarity.RARE, Category.MAGIC, 0.75, Material.FERMENTED_SPIDER_EYE, "Mắt Nhện Lên Men", "Cung cấp hiệu ứng Hồi Phục Cao", "Mùi vị cực kỳ kinh tởm."));
        register(new ItemData("magic_pufferfish", Rarity.RARE, Category.MAGIC, 0.75, Material.PUFFERFISH, "Cá Nóc Kịch Độc", "Cung cấp hiệu ứng Thở Dưới Nước", "Phải biết cách sơ chế mới dùng được."));
        register(new ItemData("magic_spider_eye", Rarity.UNCOMMON, Category.MAGIC, 0.75, Material.SPIDER_EYE, "Mắt Nhện Tinh Anh", "Dùng để hạ độc thức ăn.", "Trông khá tởm, nhưng chứa đầy chất độc."));
        register(new ItemData("magic_bat_wing", Rarity.RARE, Category.MAGIC, 0.75, Material.POTION, "Huyết Tinh", "Cung cấp thêm 10% tỉ lệ Hút Máu", "Máu của sinh vật bóng tối."));
        register(new ItemData("magic_glowstone", Rarity.RARE, Category.MAGIC, 0.75, Material.GLOWSTONE_DUST, "Bột Tiên", "Cung cấp 10% tỉ lệ Chí Mạng", "Bột phát sáng lấp lánh."));
        register(new ItemData("magic_goblin_ear", Rarity.RARE, Category.MAGIC, 0.75, Material.TURTLE_SCUTE, "Tai Goblin", "Cung cấp 10% Tỉ lệ rớt đồ", "Một chiếc tai còn rỏ máu của Goblin."));
        register(new ItemData("magic_toxic_blood", Rarity.UNCOMMON, Category.MAGIC, 0.75, Material.SLIME_BALL, "Máu Kịch Độc", "Dùng để hạ độc thức ăn.", "Thứ chất lỏng đặc sệt bốc mùi hôi thối."));

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
        //register(new ItemData("spice_all_in_one", Rarity.EPIC, Category.SPICE, 0.5, Material.SUGAR, "Gia Vị Tổng Hợp", "Cung cấp 0.5 điểm cho TẤT CẢ gia vị", "Sự kết hợp hoàn hảo của mặn, ngọt, chua, cay.")); Đã thêm ở trên
        register(new ItemData("misc_lunch_bag", Rarity.RARE, Category.MISC, 0.0, Material.PIGLIN_BANNER_PATTERN, "Túi Đồ Ăn Dự Phòng", "Dùng ngay: Mở ra 3 bát súp ngẫu nhiên", "Chuẩn bị sẵn cho những chuyến đi dài."));
        register(new ItemData("misc_preservative_powder", Rarity.MYTHIC, Category.MISC, 0.0, Material.GUNPOWDER, "Bột Bảo Quản Thần Kỳ", "Chặn 100% nguy cơ hỏng của món ăn", "Một loại bột sáng lấp lánh..."));
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
        lore.add("§8[ Phân loại: " + data.category.type.displayName + " - " + data.category.displayName + " ]");

        boolean hasAttributes = (data.points > 0) || (data.effect != null && !data.effect.isEmpty());
        if (hasAttributes) lore.add("");

        // [TỐI GIẢN GIAO DIỆN LORE]
        if (hasAttributes) {
            if (data.points > 0) {
                lore.add("§fThuộc tính Nguyên liệu:");
                lore.add("  §e+" + data.points + " §7Điểm " + data.category.displayName);
            }
            if (data.points > 0 && data.effect != null && !data.effect.isEmpty()) lore.add("");
            if (data.effect != null && !data.effect.isEmpty()) {
                lore.add("§fHiệu quả Đặc biệt:");
                lore.add("§f  ➥ §a" + data.effect); // Chỉ dùng mũi tên cho hiệu quả đặc biệt
            }
        }

        lore.add("§8§m                                ");

        if (data.description != null && !data.description.isEmpty()) {
            for (String line : data.description) lore.add("§7" + line);
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, data.id);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
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