package vn.minecraft.rPGCore.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BaitManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final Random rand = new Random();

    public BaitManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public NamespacedKey getKey(String stat, String biome) {
        return new NamespacedKey(plugin, stat + "_" + biome);
    }

    private double getBaseMultiplier(int count) {
        if (count <= 3) return count;
        return 3.0 + ((count - 3) * 0.5);
    }

    private String getItemBiome(String id) {
        if (id.contains("sandworm") || id.contains("squid") || id.contains("shell") || id.contains("kelp") || id.contains("coral") || id.contains("mermaid")) return "OCEAN";
        if (id.contains("leech") || id.contains("loach") || id.contains("coin") || id.contains("moss") || id.contains("freshwater") || id.contains("demon")) return "RIVER";
        if (id.contains("blindworm") || id.contains("crab") || id.contains("stalactite") || id.contains("mold") || id.contains("glowing_oil") || id.contains("venom")) return "CAVE";
        if (id.contains("swamp") || id.contains("frog") || id.contains("lilypad") || id.contains("gas")) return "SWAMP";
        return "ALL";
    }

    private String getBiomeName(String biome) {
        switch (biome) {
            case "OCEAN": return "Biển";
            case "RIVER": return "Sông/Hồ";
            case "CAVE": return "Hang Ngầm";
            case "SWAMP": return "Đầm Lầy";
            default: return "Mọi nơi";
        }
    }

    public ItemStack craftCustomBait(List<String> baseIds, List<String> buffIds) {
        if (baseIds.size() < 3) return null;

        Map<String, Integer> baseCounts = new HashMap<>();
        for (String id : baseIds) baseCounts.put(id, baseCounts.getOrDefault(id, 0) + 1);

        Map<String, Double> biteTime = new HashMap<>();
        Map<String, Double> bigFish = new HashMap<>();
        Map<String, Double> trashMod = new HashMap<>();
        Map<String, Double> treasure = new HashMap<>();
        Map<String, Double> secondChance = new HashMap<>();
        Map<String, Double> autoCatch = new HashMap<>();
        Map<String, Integer> tolerance = new HashMap<>();

        boolean gMonster = false, gX3 = false, gTreasure = false, isInstant = false;

        // TÍNH TOÁN BASE ITEMS (CÓ GIẢM DẦN HIỆU SUẤT)
        for (Map.Entry<String, Integer> entry : baseCounts.entrySet()) {
            String id = entry.getKey();
            String b = getItemBiome(id);
            double multi = getBaseMultiplier(entry.getValue());

            switch (id) {
                case "bait_base_earthworm": biteTime.put(b, biteTime.getOrDefault(b, 0.0) + 5.0 * multi); break;
                case "bait_base_minnow": bigFish.put(b, bigFish.getOrDefault(b, 0.0) + 5.0 * multi); break;
                case "bait_base_shiny_pebble": treasure.put(b, treasure.getOrDefault(b, 0.0) + 2.0 * multi); break;
                case "bait_base_sludge": trashMod.put(b, trashMod.getOrDefault(b, 0.0) + 10.0 * multi); break;
                case "bait_base_corn": trashMod.put(b, trashMod.getOrDefault(b, 0.0) - 5.0 * multi); break;
                case "bait_base_sticky_sap": tolerance.put(b, tolerance.getOrDefault(b, 0) + (int)(50 * multi)); break;

                case "bait_base_sandworm": case "bait_base_leech": case "bait_base_blindworm": case "bait_base_swamp_snail":
                    biteTime.put(b, biteTime.getOrDefault(b, 0.0) + 10.0 * multi); break;
                case "bait_base_squid_meat": case "bait_base_loach": case "bait_base_blind_crab": case "bait_base_small_frog":
                    bigFish.put(b, bigFish.getOrDefault(b, 0.0) + 10.0 * multi); break;
                case "bait_base_broken_shell": case "bait_base_rusty_coin": case "bait_base_stalactite":
                    treasure.put(b, treasure.getOrDefault(b, 0.0) + 5.0 * multi); break;
                case "bait_base_dead_kelp": case "bait_base_moss":
                    trashMod.put(b, trashMod.getOrDefault(b, 0.0) + 20.0 * multi); break;
                case "bait_base_mold":
                    trashMod.put(b, trashMod.getOrDefault(b, 0.0) - 10.0 * multi); break;
            }
        }

        // TÍNH TOÁN BUFF ITEMS (KHÔNG BỊ GIẢM)
        for (String id : buffIds) {
            String b = getItemBiome(id);
            switch (id) {
                case "bait_buff_pure_water": trashMod.put(b, trashMod.getOrDefault(b, 0.0) - 30.0); break;
                case "bait_buff_sonar": bigFish.put(b, bigFish.getOrDefault(b, 0.0) + 15.0); break;
                case "bait_buff_magnet": treasure.put(b, treasure.getOrDefault(b, 0.0) + 10.0); break;
                case "bait_buff_metronome": tolerance.put(b, tolerance.getOrDefault(b, 0) + 150); break;
                case "bait_buff_phoenix": secondChance.put(b, secondChance.getOrDefault(b, 0.0) + 50.0); break;
                case "bait_buff_lazy_charm": autoCatch.put(b, autoCatch.getOrDefault(b, 0.0) + 20.0); break;
                case "bait_buff_wind_glyph": biteTime.put(b, biteTime.getOrDefault(b, 0.0) + 20.0); break;
                case "bait_buff_ancient_bone": tolerance.put(b, tolerance.getOrDefault(b, 0) + 300); break;

                case "bait_buff_monster_blood": case "bait_buff_river_demon_eye": gMonster = true; break;
                case "bait_buff_golden_lure": gX3 = true; break;
                case "bait_buff_pirate_coin": gTreasure = true; break;
                case "bait_buff_lightning": isInstant = true; break;

                case "bait_buff_glowing_oil": case "bait_buff_mutant_lilypad": bigFish.put(b, bigFish.getOrDefault(b, 0.0) + 30.0); break;
                case "bait_buff_coral_powder": treasure.put(b, treasure.getOrDefault(b, 0.0) + 20.0); break;
                case "bait_buff_freshwater_crystal": case "bait_buff_spider_venom": biteTime.put(b, biteTime.getOrDefault(b, 0.0) + 40.0); break;
                case "bait_buff_mermaid_tear": secondChance.put(b, secondChance.getOrDefault(b, 0.0) + 100.0); break;
                case "bait_buff_swamp_gas": autoCatch.put(b, autoCatch.getOrDefault(b, 0.0) + 50.0); break;
            }
        }

        int totalBaseCount = baseIds.size();
        int amount = 1;
        if (totalBaseCount >= 9) amount = 3 + rand.nextInt(3);
        else if (totalBaseCount >= 6) amount = 1 + rand.nextInt(3);

        ItemStack result = itemManager.buildItem("bait_custom", amount);
        if (result == null) return null;

        ItemMeta meta = result.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add("§8[ Phân loại: Vật phẩm - Mồi Câu Đặc Chế ]");
        lore.add("");
        lore.add("§fHiệu quả Đặc biệt:");

        if (isInstant) {
            lore.add("§f  ➥ §dCá cắn câu ngay lập tức");
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bait_instant"), PersistentDataType.BYTE, (byte)1);
        }

        // Hàm tiện ích để nạp Lore và NBT
        addDynamicLore(meta, lore, biteTime, "bait_bite_time", "Thời gian chờ", "-", "%", "§a");
        addDynamicLore(meta, lore, bigFish, "bait_big_fish", "Tỉ lệ Thủy Quái", "+", "%", "§c");
        addDynamicLore(meta, lore, treasure, "bait_treasure", "Tỉ lệ Kho Báu", "+", "%", "§6");
        addDynamicLore(meta, lore, autoCatch, "bait_auto_catch", "Tỉ lệ Bỏ qua Minigame", "+", "%", "§b");
        addDynamicLore(meta, lore, secondChance, "bait_second_chance", "Cơ hội hồi sinh dây", "+", "%", "§a");

        for (String b : trashMod.keySet()) {
            double v = trashMod.get(b);
            meta.getPersistentDataContainer().set(getKey("bait_trash", b), PersistentDataType.DOUBLE, v);
            String sign = v > 0 ? "+" : "";
            String color = v > 0 ? "§7" : "§a";
            lore.add("§f  ➥ " + color + sign + String.format("%.1f", v) + "% Tỉ lệ câu Rác §7(" + getBiomeName(b) + ")");
        }

        for (String b : tolerance.keySet()) {
            int v = tolerance.get(b);
            meta.getPersistentDataContainer().set(getKey("bait_tolerance", b), PersistentDataType.INTEGER, v);
            lore.add("§f  ➥ §e+" + v + "ms Dung sai nhịp điệu §7(" + getBiomeName(b) + ")");
        }

        if (gMonster) { lore.add("§f  ➥ §4[TỐI THƯỢNG] §cGặp Thủy Quái"); meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bait_guarantee_monster"), PersistentDataType.BYTE, (byte)1); }
        if (gX3) { lore.add("§f  ➥ §4[TỐI THƯỢNG] §eThành quả x3"); meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bait_guarantee_x3"), PersistentDataType.BYTE, (byte)1); }
        if (gTreasure) { lore.add("§f  ➥ §4[TỐI THƯỢNG] §6Nhận được Rương"); meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bait_guarantee_treasure"), PersistentDataType.BYTE, (byte)1); }

        lore.add("§8§m                                ");
        lore.add("§7Được pha chế tinh xảo từ " + totalBaseCount + " nguyên liệu nền");
        lore.add("§7và các phụ gia đặc biệt.");

        meta.setLore(lore);
        result.setItemMeta(meta);
        return result;
    }

    private void addDynamicLore(ItemMeta meta, List<String> lore, Map<String, Double> statMap, String keyBase, String name, String sign, String unit, String color) {
        for (String b : statMap.keySet()) {
            double v = statMap.get(b);
            meta.getPersistentDataContainer().set(getKey(keyBase, b), PersistentDataType.DOUBLE, v);
            lore.add("§f  ➥ " + color + sign + String.format("%.1f", v) + unit + " " + name + " §7(" + getBiomeName(b) + ")");
        }
    }
}