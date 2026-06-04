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

    private static final Object[][] RANDOM_STATS_DB = {
            {"Tỉ lệ chí mạng", 100, 50}, {"Sát thương chí mạng", 0, 50}, {"Tỉ lệ xuyên giáp", 80, 30},
            {"Tỉ lệ xuyên kháng phép", 80, 30}, {"Tỉ lệ hút máu", 50, 15}, {"Tỉ lệ hút máu phép", 50, 15},
            {"Tỉ lệ đỡ đòn", 50, 60}, {"Tỉ lệ né tránh", 50, 60}, {"Tỉ lệ hồi phục", 0, 100},
            {"Tốc độ đánh", 100, 40}, {"Giảm hồi chiêu", 40, 10}, {"Tốc độ di chuyển", 0, 80},
            {"Kháng hiệu ứng", 80, 30}, {"Tỉ lệ Giáp", 0, 40}, {"Tỉ lệ Kháng phép", 0, 40},
            {"Nhận Kinh nghiệm", 0, 60}, {"Tỉ lệ rớt đồ", 0, 50},
            {"Sát thương chuẩn", 80, 20}, {"Sát thương lên Boss", 50, 25}, {"Phản sát thương", 40, 30},
            {"Hộ Thể Tuần Hoàn", 30, 20}, {"Giảm tiêu hao Năng lượng", 0, 40}, {"Hồi Năng lượng cố định", 0, 50}
    };

    // Hàm gọi nhanh (Chỉ truyền level, mọi thứ random)
    public static ItemStack createTrinket(String type, int level) {
        return createTrinket(type, level, null, -1, -1);
    }

    // Hàm cầu nối (Wrapper) cho TrinketRandomCraftGUI để tránh lỗi Code
    public static ItemStack createTrinketExact(String type, int level, String forceLineage, int forceSubStats, int exactTier) {
        return createTrinket(type, level, forceLineage, exactTier, forceSubStats);
    }

    // ==========================================
    // HÀM MASTER: XỬ LÝ TOÀN BỘ LOGIC GACHA (GỘP CHUNG)
    // ==========================================
    public static ItemStack createTrinket(String type, int level, String inputLineage, int inputTier, int inputSubStats) {
        Random rand = new Random();

        // 1. Xử lý Tier (Phẩm chất)
        int tierIndex = inputTier;
        if (tierIndex == -1) {
            int totalWeight = 0;
            for (int w : TIER_WEIGHTS) totalWeight += w;
            int roll = rand.nextInt(totalWeight);
            int currentWeightRoll = 0;
            for (int i = 0; i < TIER_WEIGHTS.length; i++) {
                currentWeightRoll += TIER_WEIGHTS[i];
                if (roll < currentWeightRoll) { tierIndex = i; break; }
            }
            if (tierIndex == -1) tierIndex = 0;
        } else {
            tierIndex = Math.min(TIERS.length - 1, Math.max(0, tierIndex));
        }

        // 2. Xử lý Lineage (Dòng dõi)
        String lineage;
        if (inputLineage != null && ALL_LINEAGES.contains(inputLineage.toLowerCase())) {
            lineage = inputLineage.toLowerCase();
        } else {
            lineage = ALL_LINEAGES.get(rand.nextInt(ALL_LINEAGES.size()));
        }

        String tierColor = (String) TIERS[tierIndex][0];
        double tierMulti = (double) TIERS[tierIndex][2];

        // 3. Xử lý Quality (Độ hoàn thiện)
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

        // 4. Khởi tạo Material
        Material mat = Material.GOLDEN_AXE;
        int customModelData = 1001;
        String baseName;

        if (type.equalsIgnoreCase("ring")) {
            baseName = "Ring";
            customModelData = 1001;
        } else if (type.equalsIgnoreCase("necklace")) {
            baseName = "Necklace";
            customModelData = 1101;
        } else {
            baseName = "Bracelet";
            customModelData = 1201;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(tierColor + "§l" + qualityName + " " + capitalize(lineage) + " " + baseName);
        meta.setCustomModelData(customModelData);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8▶ Thuộc class: §fTất cả");
        lore.add("§8▶ Yêu cầu cấp độ: " + level);
        lore.add("");

        double levelFactor = Math.min(level, 100) / 100.0;
        double tierFactor = (double) tierIndex / (TIERS.length - 1);
        double qualityFactor = (qualityMod - 0.4) / (1.2 - 0.4);
        double powerRating = (levelFactor * 0.45) + (tierFactor * 0.35) + (qualityFactor * 0.20);
        double statMulti = 1.0 + (6.0 * powerRating);

        // 5. Gắn chỉ số Dòng dõi
        lore.add(tierColor + "§l✦ Thuộc tính Dòng dõi (" + capitalize(lineage) + "):");
        List<String> excludedStats = applyLineageText(lore, lineage, statMulti);
        lore.add("");
        lore.add("§d§l✦ Thuộc tính Ma thuật:");

        // 6. Tính toán & Gắn chỉ số Phụ (Substats)
        int subStats = (inputSubStats != -1) ? inputSubStats : getSubStatsCount(tierIndex, rand);

        List<Object[]> availableStats = new ArrayList<>();
        for (Object[] statData : RANDOM_STATS_DB) {
            if (!excludedStats.contains((String) statData[0])) availableStats.add(statData);
        }
        List<String> preferredStats = getPreferredStats(lineage);
        int synergyBoost = 2 + (tierIndex * 2);

        for (int i = 0; i < subStats; i++) {
            if (availableStats.isEmpty()) break;

            int totalDbWeight = 0;
            for (Object[] statData : availableStats) {
                String statName = (String) statData[0];
                int baseWeight = (int) statData[2];
                if (preferredStats.contains(statName)) baseWeight *= synergyBoost;
                totalDbWeight += baseWeight;
            }

            int subRoll = rand.nextInt(totalDbWeight);
            int subWeightRoll = 0;
            int chosenIndex = -1;

            for (int j = 0; j < availableStats.size(); j++) {
                String statName = (String) availableStats.get(j)[0];
                int baseWeight = (int) availableStats.get(j)[2];
                if (preferredStats.contains(statName)) baseWeight *= synergyBoost;

                subWeightRoll += baseWeight;
                if (subRoll < subWeightRoll) { chosenIndex = j; break; }
            }

            if (chosenIndex != -1) {
                Object[] chosenStat = availableStats.get(chosenIndex);
                String statName = (String) chosenStat[0];
                double value = (0.3 + rand.nextDouble() * 0.7) * statMulti;

                if (statName.equals("Hồi Năng lượng cố định")) lore.add("§7 - " + statName + ": §e+" + String.format("%.1f", value / 10.0));
                else if (statName.equals("Hộ Thể Tuần Hoàn")) lore.add("§7 - " + statName + ": §e" + String.format("%.1f", Math.min(value / 3.0, 15.0)) + "% §8(HP tối đa)");
                else if (statName.equals("Phản sát thương")) lore.add("§7 - " + statName + ": §e" + String.format("%.1f", value) + "% §8(10% HP tối đa)");
                else lore.add("§7 - " + statName + ": §e+" + String.format("%.1f", value) + "%");

                availableStats.remove(chosenIndex);
            }
        }

        lore.add(""); applySetBonusText(lore, lineage); lore.add("");

        meta.setUnbreakable(true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        // 7. Chống Stack vĩnh viễn bằng UUID
        org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "trinket_type"), org.bukkit.persistence.PersistentDataType.STRING, type.toLowerCase());
        data.set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "trinket_uuid"), org.bukkit.persistence.PersistentDataType.STRING, UUID.randomUUID().toString());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private static String capitalize(String str) { return str == null || str.isEmpty() ? str : str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase(); }

    private static List<String> applyLineageText(List<String> lore, String lineage, double statMulti) {
        List<String> excluded = new ArrayList<>();
        switch (lineage) {
            case "vampiric":
                lore.add("§c - Tỉ lệ hút máu: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                lore.add("§c - Tỉ lệ hút máu phép: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ hút máu", "Tỉ lệ hút máu phép")); break;
            case "experienced":
                lore.add("§b - Nhận Kinh nghiệm: +" + String.format("%.1f", 2.5 * statMulti) + "%");
                lore.add("§b - Tỉ lệ rớt đồ: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Nhận Kinh nghiệm", "Tỉ lệ rớt đồ")); break;
            case "hunter":
                lore.add("§e - Tỉ lệ chí mạng: +" + String.format("%.1f", 1.0 * statMulti) + "%");
                lore.add("§e - Sát thương chí mạng: +" + String.format("%.1f", 4.0 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ chí mạng", "Sát thương chí mạng")); break;
            case "magical":
                lore.add("§9 - Hồi Năng lượng: +" + String.format("%.1f", 2.5 * statMulti) + "%");
                lore.add("§9 - Giảm hồi chiêu: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Hồi Năng lượng", "Giảm hồi chiêu")); break;
            case "giant":
                lore.add("§a - Máu tối đa: +" + String.format("%.1f", 2.5 * statMulti) + "%");
                lore.add("§a - Tỉ lệ hồi phục: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Máu tối đa", "Tỉ lệ hồi phục")); break;
            case "armored":
                lore.add("§6 - Tỉ lệ Giáp: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                lore.add("§6 - Tỉ lệ Kháng phép: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ Giáp", "Tỉ lệ Kháng phép")); break;
            case "scavenger":
                lore.add("§e - Tỉ lệ rớt đồ: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                lore.add("§e - Tốc độ di chuyển: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ rớt đồ", "Tốc độ di chuyển")); break;
            case "swift":
                lore.add("§f - Tốc độ di chuyển: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                lore.add("§f - Kháng hiệu ứng: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tốc độ di chuyển", "Kháng hiệu ứng")); break;
            case "shattering":
                lore.add("§4 - Tỉ lệ xuyên giáp: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                lore.add("§4 - Tỉ lệ xuyên kháng phép: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ xuyên giáp", "Tỉ lệ xuyên kháng phép")); break;
        }
        return excluded;
    }

    private static void applySetBonusText(List<String> lore, String lineage) {
        switch (lineage) {
            case "vampiric":
                lore.add("§8[Set 3 mảnh]: +15% Hút máu và Hút máu phép");
                lore.add("§8[Set 4 mảnh]: Hút máu dư chuyển thành Máu ảo (Tối đa 20% Max HP)"); break;
            case "experienced":
                lore.add("§8[Set 3 mảnh]: +20% Kinh nghiệm nhận được");
                lore.add("§8[Set 4 mảnh]: Khi nhặt đồ có 5% rơi ra thêm Xu ngẫu nhiên"); break;
            case "hunter":
                lore.add("§8[Set 3 mảnh]: +10% Tỉ lệ chí mạng");
                lore.add("§8[Set 4 mảnh]: Đòn bạo kích tăng 15% Tốc chạy trong 3 giây"); break;
            case "magical":
                lore.add("§8[Set 3 mảnh]: +25% Tốc độ hồi Năng lượng");
                lore.add("§8[Set 4 mảnh]: Kỹ năng tiêu hao giảm 20% Năng lượng"); break;
            case "giant":
                lore.add("§8[Set 3 mảnh]: +20% Máu tối đa");
                lore.add("§8[Set 4 mảnh]: Đứng yên 3s sẽ tự động hồi 2% Máu/s"); break;
            case "armored":
                lore.add("§8[Set 3 mảnh]: +20% Tỉ lệ Giáp và Kháng Phép");
                lore.add("§8[Set 4 mảnh]: Chặn 1 đòn sát thương chí mạng mỗi 30 giây"); break;
            case "scavenger":
                lore.add("§8[Set 3 mảnh]: +25% Tỉ lệ rớt đồ");
                lore.add("§8[Set 4 mảnh]: Có 10% tỉ lệ x2 số lượng vật phẩm nhặt được"); break;
            case "swift":
                lore.add("§8[Set 3 mảnh]: +15% Kháng hiệu ứng");
                lore.add("§8[Set 4 mảnh]: Miễn nhiễm hoàn toàn với Trói Chân và Làm Chậm"); break;
            case "shattering":
                lore.add("§8[Set 3 mảnh]: +15% Xuyên Giáp và Xuyên Kháng Phép");
                lore.add("§8[Set 4 mảnh]: Gây thêm 10% Sát thương chuẩn"); break;
        }
    }

    private static List<String> getPreferredStats(String lineage) {
        switch (lineage.toLowerCase()) {
            case "vampiric": return Arrays.asList("Tỉ lệ hồi phục", "Hộ Thể Tuần Hoàn", "Sát thương lên Boss", "Tỉ lệ Giáp");
            case "experienced": return Arrays.asList("Tốc độ di chuyển", "Giảm hồi chiêu", "Hồi Năng lượng");
            case "hunter": return Arrays.asList("Tốc độ đánh", "Sát thương chuẩn", "Sát thương lên Boss", "Tỉ lệ xuyên giáp");
            case "magical": return Arrays.asList("Giảm tiêu hao Năng lượng", "Hồi Năng lượng cố định", "Hồi Năng lượng", "Tỉ lệ xuyên kháng phép");
            case "giant": return Arrays.asList("Tỉ lệ đỡ đòn", "Kháng hiệu ứng", "Hộ Thể Tuần Hoàn", "Tỉ lệ Kháng phép");
            case "armored": return Arrays.asList("Tỉ lệ đỡ đòn", "Phản sát thương", "Hộ Thể Tuần Hoàn", "Kháng hiệu ứng");
            case "scavenger": return Arrays.asList("Tỉ lệ né tránh", "Nhận Kinh nghiệm", "Tốc độ di chuyển", "Hồi Năng lượng");
            case "swift": return Arrays.asList("Tỉ lệ né tránh", "Tốc độ đánh", "Giảm hồi chiêu", "Tỉ lệ chí mạng");
            case "shattering": return Arrays.asList("Sát thương chuẩn", "Sát thương lên Boss", "Tỉ lệ chí mạng", "Tỉ lệ xuyên giáp", "Sát thương chí mạng");
            default: return new ArrayList<>();
        }
    }

    private static int getSubStatsCount(int tierIndex, Random rand) {
        int[][] chances = {
                {60, 30, 9, 1, 0},   // 0: Common (Max 4, tỉ lệ ra 4 cực thấp 1%)
                {40, 40, 15, 5, 0},  // 1: Uncommon
                {20, 45, 25, 10, 0}, // 2: Rare
                {5,  35, 40, 20, 0}, // 3: Epic
                {0,  25, 45, 30, 0}, // 4: Mythical
                {0,  15, 45, 35, 5}, // 5: Heroic (Bắt đầu có tỉ lệ ra 5 dòng)
                {0,  5,  40, 45, 10},// 6: Legendary
                {0,  0,  20, 60, 20} // 7: Divine (Chắc chắn >= 3 dòng, 20% ra 5 dòng)
        };
        int[] weights = chances[Math.min(7, Math.max(0, tierIndex))];
        int roll = rand.nextInt(100);
        int current = 0;
        for (int i = 0; i < weights.length; i++) {
            current += weights[i];
            if (roll < current) return i + 1;
        }
        return 1;
    }
}