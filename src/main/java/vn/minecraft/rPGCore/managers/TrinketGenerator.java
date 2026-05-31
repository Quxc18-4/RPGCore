package vn.minecraft.rPGCore.managers;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class TrinketGenerator {

    private static final Object[][] TIERS = {
            {"§7", "common", 1.0}, {"§a", "uncommon", 1.2}, {"§b", "rare", 1.5}, {"§5", "epic", 1.8},
            {"§d", "mythical", 2.2}, {"§6", "heroic", 2.8}, {"§c", "legendary", 3.5}, {"§e", "divine", 5.0}
    };

    private static final int[] TIER_WEIGHTS = { 5000, 2500, 1000, 400, 150, 50, 10, 5 };
    private static final List<String> ALL_LINEAGES = Arrays.asList("vampiric", "experienced", "hunter", "magical", "giant", "armored", "scavenger", "swift", "shattering");

    // ==========================================
    // [MỚI]: BÊ NGUYÊN HỆ THỐNG QUALITY TỪ EQUIPMENT SANG
    // ==========================================
    private static final Object[][] QUALITY_DB = {
            {new String[]{"Rusty", "Broken", "Ruined", "Chipped"}, 0.40, 0.70, 100},
            {new String[]{"Old", "Worn", "Dull", "Faded"}, 0.60, 0.85, 80},
            {new String[]{"Normal", "Standard", "Decent", "Common"}, 0.80, 1.00, 60},
            {new String[]{"Fine", "Polished", "Refined", "Sturdy"}, 0.90, 1.05, 40},
            {new String[]{"Reinforced", "Flawless", "Exceptional", "Immaculate"}, 0.95, 1.10, 20},
            {new String[]{"Masterwork", "Heroic", "Glorious", "Majestic"}, 1.00, 1.15, 10},
            {new String[]{"Legendary", "Exquisite", "Perfect", "Peerless"}, 1.05, 1.15, 5},
            {new String[]{"Divine", "Godly", "Ascended", "Transcendent"}, 1.10, 1.20, 2}
    };

    // Bể Bơi Chỉ số (Gồm cả Phụ bản cũ và 6 Chỉ số Độc quyền mới của Trinket)
    private static final Object[][] RANDOM_STATS_DB = {
            // [NHÓM TRUYỀN THỐNG]
            {"Tỉ lệ chí mạng", 100, 50}, {"Sát thương chí mạng", 0, 50}, {"Tỉ lệ xuyên giáp", 80, 30},
            {"Tỉ lệ xuyên kháng phép", 80, 30}, {"Tỉ lệ hút máu", 50, 15}, {"Tỉ lệ hút máu phép", 50, 15},
            {"Tỉ lệ đỡ đòn", 50, 60}, {"Tỉ lệ né tránh", 50, 60}, {"Tỉ lệ hồi phục", 0, 100},
            {"Tốc độ đánh", 100, 40}, {"Giảm hồi chiêu", 40, 10}, {"Tốc độ di chuyển", 0, 80},
            {"Kháng hiệu ứng", 80, 30}, {"Tỉ lệ Giáp", 0, 40}, {"Tỉ lệ Kháng phép", 0, 40},
            {"Nhận Kinh nghiệm", 0, 60}, {"Tỉ lệ rớt đồ", 0, 50},

            // [NHÓM 6 CHỈ SỐ ĐỘC QUYỀN TRINKET MỚI]
            {"Sát thương chuẩn", 80, 20},            // % Tỉ lệ gây sát thương chuẩn
            {"Sát thương lên Boss", 50, 25},         // % Tăng dmg khi đánh MythicMobs
            {"Phản sát thương", 40, 30},             // % Phản (Có Cap % Máu)
            {"Hộ Thể Tuần Hoàn", 30, 20},            // Tạo Giáp Ảo bằng % MaxHP
            {"Giảm tiêu hao Năng lượng", 0, 40},     // % Giảm Cost (Cap 33%)
            {"Hồi Năng lượng Tĩnh", 0, 50}           // Hồi X Flat Mana / giây
    };

    public static ItemStack createTrinket(String type, int level) {
        Random rand = new Random();

        // 1. Roll Tier (Phẩm chất)
        int tierIndex = 0;
        int roll = rand.nextInt(9515); // Tổng Weight
        int currentWeightRoll = 0;
        for (int i = 0; i < TIER_WEIGHTS.length; i++) {
            currentWeightRoll += TIER_WEIGHTS[i];
            if (roll < currentWeightRoll) { tierIndex = i; break; }
        }

        // ==========================================
        // 2. Roll Lineage & Quality (Chuẩn format EquipmentCommand)
        // ==========================================
        String lineage = ALL_LINEAGES.get(rand.nextInt(ALL_LINEAGES.size()));
        String tierColor = (String) TIERS[tierIndex][0];
        double tierMulti = (double) TIERS[tierIndex][2];

        // Gacha Quality
        int totalQualityWeight = 0;
        for (Object[] q : QUALITY_DB) totalQualityWeight += (int) q[3];
        int qualityRoll = rand.nextInt(totalQualityWeight);
        int currentWeightQ = 0;
        Object[] chosenQuality = QUALITY_DB[0];
        for (Object[] q : QUALITY_DB) {
            currentWeightQ += (int) q[3];
            if (qualityRoll < currentWeightQ) { chosenQuality = q; break; }
        }

        String[] qualityNames = (String[]) chosenQuality[0];
        String qualityName = qualityNames[rand.nextInt(qualityNames.length)];
        double minVar = (double) chosenQuality[1];
        double maxVar = (double) chosenQuality[2];
        double qualityMod = minVar + (rand.nextDouble() * (maxVar - minVar));

        // Tên gốc thuần Anh
        Material mat; String baseName;
        if (type.equalsIgnoreCase("ring")) {
            mat = rand.nextBoolean() ? Material.GOLD_NUGGET : Material.IRON_NUGGET;
            baseName = "Ring";
        } else if (type.equalsIgnoreCase("necklace")) {
            mat = Material.END_CRYSTAL;
            baseName = "Necklace";
        } else {
            mat = Material.STRING;
            baseName = "Bracelet"; // Hoặc có thể dùng "Bangle"
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // Hợp nhất tên chuẩn form: [Màu] + [Quality] + [Lineage] + [Tên Đồ]
        // VD: §e§lFlawless Shattering Ring
        meta.setDisplayName(tierColor + "§l" + qualityName + " " + capitalize(lineage) + " " + baseName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8▶ Thuộc class: §fTất cả"); // Giúp RPGPlayer chấp nhận Item này
        lore.add("§8▶ Yêu cầu cấp độ: " + level);
        lore.add("");

        // Tính toán Power Rating
        double levelFactor = Math.min(level, 100) / 100.0;
        double tierFactor = (double) tierIndex / (TIERS.length - 1);
        double qualityFactor = (qualityMod - 0.4) / (1.2 - 0.4);
        double powerRating = (levelFactor * 0.45) + (tierFactor * 0.35) + (qualityFactor * 0.20);

        // [ĐÃ NERF MẠNH]: Giảm scale để tránh lạm phát (Ví dụ: Max né tránh chỉ ra 5-8%)
        double statMulti = 1.0 + (6.0 * powerRating);

        lore.add(tierColor + "§l✦ Thuộc tính Dòng dõi (" + capitalize(lineage) + "):");
        applyLineageText(lore, lineage, statMulti);
        lore.add("");

        // ==========================================
        // THUẬT TOÁN GACHA "DECK LỌC" - CHỐNG TRÙNG LẶP 100%
        // ==========================================
        lore.add("§d§l✦ Thuộc tính Ma thuật:");

        int subStats = 3 + rand.nextInt(3); // Ra 3 đến 5 dòng ngẫu nhiên

        // Đổ toàn bộ dữ liệu vào một mảng List động (như 1 bộ bài)
        List<Object[]> availableStats = new ArrayList<>();
        Collections.addAll(availableStats, RANDOM_STATS_DB);

        for (int i = 0; i < subStats; i++) {
            if (availableStats.isEmpty()) break;

            int totalDbWeight = 0;
            for (Object[] statData : availableStats) {
                totalDbWeight += (int) statData[2];
            }

            int subRoll = rand.nextInt(totalDbWeight);
            int subWeightRoll = 0;
            int chosenIndex = -1;

            for (int j = 0; j < availableStats.size(); j++) {
                subWeightRoll += (int) availableStats.get(j)[2];
                if (subRoll < subWeightRoll) {
                    chosenIndex = j;
                    break;
                }
            }

            if (chosenIndex != -1) {
                Object[] chosenStat = availableStats.get(chosenIndex);
                String statName = (String) chosenStat[0];
                // Chỉ số cơ bản sinh ra nhỏ hơn để cân bằng
                double value = (0.3 + rand.nextDouble() * 0.7) * statMulti;

                // [CUSTOM SCALE]: Một vài chỉ số độc quyền cần chỉnh lại giá trị hiển thị cho hợp lý
                if (statName.equals("Hồi Năng lượng cố định")) {
                    lore.add("§7 - " + statName + ": §e+" + String.format("%.1f", value / 10.0));
                } else if (statName.equals("Hộ Thể Tuần Hoàn")) {
                    // Thay vì ghi "% Max HP", ta ghi "% (Max HP)"
                    lore.add("§7 - " + statName + ": §e" + String.format("%.1f", Math.min(value / 3.0, 15.0)) + "% §8(Max HP)");
                } else if (statName.equals("Phản sát thương")) {
                    lore.add("§7 - " + statName + ": §e" + String.format("%.1f", value) + "% §8(Max 10% HP)");
                } else {
                    lore.add("§7 - " + statName + ": §e+" + String.format("%.1f", value) + "%");
                }

                // XÓA NGAY LẬP TỨC CHỈ SỐ VỪA ROLL KHỎI BỘ BÀI -> CHỐNG TRÙNG LẶP
                availableStats.remove(chosenIndex);
            }
        }

        lore.add("");
        applySetBonusText(lore, lineage);
        lore.add("");

        meta.setUnbreakable(true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        // Gắn ID Ngầm để hệ thống nhận diện
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "trinket_type"), org.bukkit.persistence.PersistentDataType.STRING, type.toLowerCase());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private static String capitalize(String str) { return str == null || str.isEmpty() ? str : str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase(); }

    private static void applyLineageText(List<String> lore, String lineage, double statMulti) {
        // [Giữ nguyên như cũ]
        switch (lineage) {
            case "vampiric": lore.add("§c - Tỉ lệ hút máu: +" + String.format("%.1f", 1.5 * statMulti) + "%"); lore.add("§c - Tỉ lệ hút máu phép: +" + String.format("%.1f", 1.5 * statMulti) + "%"); break;
            case "experienced": lore.add("§b - Nhận Kinh nghiệm: +" + String.format("%.1f", 2.5 * statMulti) + "%"); lore.add("§b - Tỉ lệ rớt đồ: +" + String.format("%.1f", 1.5 * statMulti) + "%"); break;
            case "hunter": lore.add("§e - Tỉ lệ chí mạng: +" + String.format("%.1f", 1.0 * statMulti) + "%"); lore.add("§e - Sát thương chí mạng: +" + String.format("%.1f", 4.0 * statMulti) + "%"); break;
            case "magical": lore.add("§9 - Hồi Năng lượng: +" + String.format("%.1f", 2.5 * statMulti) + "%"); lore.add("§9 - Giảm hồi chiêu: +" + String.format("%.1f", 1.5 * statMulti) + "%"); break;
            case "giant": lore.add("§a - Máu tối đa: +" + String.format("%.1f", 2.5 * statMulti) + "%"); lore.add("§a - Tỉ lệ hồi phục: +" + String.format("%.1f", 1.5 * statMulti) + "%"); break;
            case "armored": lore.add("§6 - Tỉ lệ Giáp: +" + String.format("%.1f", 2.0 * statMulti) + "%"); lore.add("§6 - Tỉ lệ Kháng phép: +" + String.format("%.1f", 2.0 * statMulti) + "%"); break;
            case "scavenger": lore.add("§e - Tỉ lệ rớt đồ: +" + String.format("%.1f", 2.0 * statMulti) + "%"); lore.add("§e - Tốc độ di chuyển: +" + String.format("%.1f", 1.5 * statMulti) + "%"); break;
            case "swift": lore.add("§f - Tốc độ di chuyển: +" + String.format("%.1f", 1.5 * statMulti) + "%"); lore.add("§f - Kháng hiệu ứng: +" + String.format("%.1f", 1.5 * statMulti) + "%"); break;
            case "shattering": lore.add("§4 - Tỉ lệ xuyên giáp: +" + String.format("%.1f", 2.0 * statMulti) + "%"); lore.add("§4 - Tỉ lệ xuyên kháng phép: +" + String.format("%.1f", 2.0 * statMulti) + "%"); break;
        }
    }

    private static void applySetBonusText(List<String> lore, String lineage) {
        // [Giữ nguyên như cũ]
        switch (lineage) {
            case "vampiric": lore.add("§8[Set 3 mảnh]: +15% Hút máu và Hút máu phép"); lore.add("§8[Set 4 mảnh]: Hút máu dư chuyển thành Máu ảo"); break;
            case "experienced": lore.add("§8[Set 3 mảnh]: +20% Kinh nghiệm nhận được"); lore.add("§8[Set 4 mảnh]: Khi nhặt đồ có 5% rơi ra xu"); break;
            case "hunter": lore.add("§8[Set 3 mảnh]: +10% Tỉ lệ chí mạng"); lore.add("§8[Set 4 mảnh]: Đòn bạo kích tăng 15% Tốc chạy"); break;
            case "magical": lore.add("§8[Set 3 mảnh]: +25% Tốc độ hồi Năng lượng"); lore.add("§8[Set 4 mảnh]: Kỹ năng tiêu hao giảm 20% Năng lượng"); break;
            case "giant": lore.add("§8[Set 3 mảnh]: +20% Máu tối đa"); lore.add("§8[Set 4 mảnh]: Đứng yên 3s sẽ tự động hồi HP"); break;
            case "armored": lore.add("§8[Set 3 mảnh]: +20% Tỉ lệ Giáp và Kháng Phép"); lore.add("§8[Set 4 mảnh]: Chặn 1 đòn sát thương chí mạng"); break;
            case "scavenger": lore.add("§8[Set 3 mảnh]: +25% Tỉ lệ rớt đồ"); lore.add("§8[Set 4 mảnh]: Có 10% x2 số lượng vật phẩm nhặt được"); break;
            case "swift": lore.add("§8[Set 3 mảnh]: +15% Kháng hiệu ứng"); lore.add("§8[Set 4 mảnh]: Miễn nhiễm hoàn toàn với Trói Chân"); break;
            case "shattering": lore.add("§8[Set 3 mảnh]: +15% Xuyên Giáp và Xuyên Kháng Phép"); lore.add("§8[Set 4 mảnh]: Gây thêm 10% Sát thương chuẩn"); break;
        }
    }
}