package vn.minecraft.rPGCore.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatManager {

    // Class lưu trữ toàn bộ chỉ số đã được xử lý (Phục vụ cho tính toán Combat)
    public static class PlayerStats {
        public double minPhysicalDamage = 0.0;
        public double maxPhysicalDamage = 0.0;
        public double minMagicalDamage = 0.0;
        public double maxMagicalDamage = 0.0;

        public double maxHealth = 0.0;
        public double armor = 0.0;

        // Base Stats (Chỉ số cơ bản của nhân vật)
        public double critRate = 5.0;      // Mặc định 5%
        public double critDamage = 50.0;   // Mặc định 50% (Gây 150% sát thương)

        public double armorPen = 0.0;
        public double lifeSteal = 0.0;
        public double dodgeRate = 0.0;
        public double blockRate = 0.0;
        public double regenRate = 0.0;
        public double attackSpeed = 0.0;
        public double cooldownReduction = 0.0;
        public double moveSpeed = 0.0;

        public Map<String, Integer> setCounts = new HashMap<>();
    }

    // Hàm lấy Object PlayerStats (Dùng cho class ArcherListener / Chiến đấu)
    public static PlayerStats getEquipmentStats(Player player) {
        PlayerStats stats = new PlayerStats();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        ItemStack[] allEquips = new ItemStack[5];
        System.arraycopy(armor, 0, allEquips, 0, 4);
        allEquips[4] = mainHand;

        for (ItemStack item : allEquips) {
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;
            boolean isWeapon = (item == mainHand);

            for (String line : item.getItemMeta().getLore()) {
                String clean = ChatColor.stripColor(line).trim();
                if (clean.isEmpty()) continue;

                if (!isWeapon && clean.startsWith("✦ Thuộc tính Dòng dõi (")) {
                    String lineage = clean.substring(clean.indexOf("(") + 1, clean.indexOf(")")).toLowerCase();
                    stats.setCounts.put(lineage, stats.setCounts.getOrDefault(lineage, 0) + 1);
                    continue;
                }

                if (clean.startsWith("- ")) {
                    String[] parts = clean.substring(2).split(":");
                    if (parts.length == 2) {
                        String statName = parts[0].trim();
                        // Lọc bỏ dấu +, %, chữ "Giới hạn"
                        String valStr = parts[1].split("\\(")[0].replaceAll("[+%]", "").trim();

                        try {
                            if (valStr.contains("~")) {
                                String[] range = valStr.split("~");
                                double min = Double.parseDouble(range[0].trim());
                                double max = Double.parseDouble(range[1].trim());
                                if (statName.contains("vật lý")) {
                                    stats.minPhysicalDamage += min;
                                    stats.maxPhysicalDamage += max;
                                } else if (statName.contains("phép thuật")) {
                                    stats.minMagicalDamage += min;
                                    stats.maxMagicalDamage += max;
                                }
                            } else {
                                double val = Double.parseDouble(valStr);
                                switch (statName) {
                                    case "Máu tối đa": stats.maxHealth += val; break;
                                    case "Giáp": stats.armor += val; break;
                                    case "Tỉ lệ chí mạng": stats.critRate += val; break;
                                    case "Sát thương chí mạng": stats.critDamage += val; break;
                                    case "Tỉ lệ xuyên giáp": stats.armorPen += val; break;
                                    case "Tỉ lệ hút máu": stats.lifeSteal += val; break;
                                    case "Tỉ lệ né tránh": stats.dodgeRate += val; break;
                                    case "Tỉ lệ đỡ đòn": stats.blockRate += val; break;
                                    case "Tỉ lệ hồi phục": stats.regenRate += val; break;
                                    case "Tốc độ đánh": stats.attackSpeed += val; break;
                                    case "Giảm hồi chiêu": stats.cooldownReduction += val; break;
                                    case "Tốc độ di chuyển": stats.moveSpeed += val; break;
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        applySetBonuses(stats);

        // Giới hạn chỉ số (Hard Caps)
        if (stats.critRate > 100.0) stats.critRate = 100.0;
        if (stats.armorPen > 80.0) stats.armorPen = 80.0;
        if (stats.cooldownReduction > 40.0) stats.cooldownReduction = 40.0;
        if (stats.lifeSteal > 50.0) stats.lifeSteal = 50.0;
        if (stats.attackSpeed > 100.0) stats.attackSpeed = 100.0;
        if (stats.dodgeRate > 50.0) stats.dodgeRate = 50.0;
        if (stats.blockRate > 50.0) stats.blockRate = 50.0;

        return stats;
    }

    private static void applySetBonuses(PlayerStats stats) {
        int vampiric = stats.setCounts.getOrDefault("vampiric", 0);
        int hunter = stats.setCounts.getOrDefault("hunter", 0);
        int giant = stats.setCounts.getOrDefault("giant", 0);
        int armored = stats.setCounts.getOrDefault("armored", 0);
        int swift = stats.setCounts.getOrDefault("swift", 0);
        int shattering = stats.setCounts.getOrDefault("shattering", 0);

        if (vampiric >= 3) stats.lifeSteal += 12.0;
        if (hunter >= 3) stats.critRate += 8.0;
        if (hunter >= 4) stats.critDamage += 40.0;
        if (giant >= 3) stats.maxHealth *= 1.25;
        if (armored >= 3) stats.armor *= 1.25;
        if (swift >= 3) stats.moveSpeed += 15.0;
        if (shattering >= 3) stats.armorPen += 15.0;
    }

    // Hàm chuyển đổi sang Map (Giữ nguyên để KHÔNG LÀM LỖI lệnh /profile stats)
    public static Map<String, Double> getPlayerStats(Player player) {
        PlayerStats stats = getEquipmentStats(player);
        Map<String, Double> map = new LinkedHashMap<>(); // Dùng LinkedHashMap để in theo thứ tự cố định

        if (stats.minPhysicalDamage > 0) {
            map.put("Sát thương vật lý (Nhỏ nhất)", stats.minPhysicalDamage);
            map.put("Sát thương vật lý (Lớn nhất)", stats.maxPhysicalDamage);
        }
        if (stats.minMagicalDamage > 0) {
            map.put("Sát thương phép thuật (Nhỏ nhất)", stats.minMagicalDamage);
            map.put("Sát thương phép thuật (Lớn nhất)", stats.maxMagicalDamage);
        }

        if (stats.maxHealth > 0) map.put("Máu tối đa", stats.maxHealth);
        if (stats.armor > 0) map.put("Giáp", stats.armor);

        map.put("Tỉ lệ chí mạng", stats.critRate);
        map.put("Sát thương chí mạng", stats.critDamage);

        if (stats.armorPen > 0) map.put("Tỉ lệ xuyên giáp", stats.armorPen);
        if (stats.lifeSteal > 0) map.put("Tỉ lệ hút máu", stats.lifeSteal);
        if (stats.dodgeRate > 0) map.put("Tỉ lệ né tránh", stats.dodgeRate);
        if (stats.blockRate > 0) map.put("Tỉ lệ đỡ đòn", stats.blockRate);
        if (stats.regenRate > 0) map.put("Tỉ lệ hồi phục", stats.regenRate);
        if (stats.attackSpeed > 0) map.put("Tốc độ đánh", stats.attackSpeed);
        if (stats.cooldownReduction > 0) map.put("Giảm hồi chiêu", stats.cooldownReduction);
        if (stats.moveSpeed > 0) map.put("Tốc độ di chuyển", stats.moveSpeed);

        return map;
    }
}