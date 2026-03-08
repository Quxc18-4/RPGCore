package vn.minecraft.rPGCore.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class StatManager {

    // Hàm quyét toàn bộ trang bị đang mặc và trả về Tổng chỉ số
    public static Map<String, Double> getPlayerStats(Player player) {
        Map<String, Double> stats = new HashMap<>();
        Map<String, Integer> setCounts = new HashMap<>();

        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Gộp chung Giáp và Vũ khí vào 1 mảng để quét
        ItemStack[] allEquips = new ItemStack[5];
        System.arraycopy(armor, 0, allEquips, 0, 4);
        allEquips[4] = mainHand;

        for (ItemStack item : allEquips) {
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;

            boolean isWeapon = (item == mainHand);

            for (String line : item.getItemMeta().getLore()) {
                // Xóa mã màu để dễ đọc text
                String clean = ChatColor.stripColor(line).trim();
                if (clean.isEmpty()) continue;

                // 1. Nhận diện Set Dòng dõi để đếm (Chỉ tính Giáp)
                if (!isWeapon && clean.startsWith("✦ Thuộc tính Dòng dõi (")) {
                    String lineage = clean.substring(clean.indexOf("(") + 1, clean.indexOf(")")).toLowerCase();
                    setCounts.put(lineage, setCounts.getOrDefault(lineage, 0) + 1);
                    continue;
                }

                // 2. Tự động Nhận diện và Cộng dồn MỌI CHỈ SỐ
                if (clean.startsWith("- ")) {
                    String[] parts = clean.substring(2).split(":");
                    if (parts.length == 2) {
                        String statName = parts[0].trim();
                        // Loại bỏ dấu + và dấu % để lấy số nguyên chất
                        String valStr = parts[1].replaceAll("[+%]", "").trim();

                        // Nếu là khoảng sát thương vũ khí (VD: 100.0 ~ 150.0)
                        if (valStr.contains("~")) {
                            String[] range = valStr.split("~");
                            try {
                                double min = Double.parseDouble(range[0].trim());
                                double max = Double.parseDouble(range[1].trim());
                                stats.put(statName + " (Nhỏ nhất)", stats.getOrDefault(statName + " (Nhỏ nhất)", 0.0) + min);
                                stats.put(statName + " (Lớn nhất)", stats.getOrDefault(statName + " (Lớn nhất)", 0.0) + max);
                            } catch (NumberFormatException ignored) {}
                        } else {
                            // Cộng dồn các chỉ số bình thường (Máu, Giáp, Chí mạng, v.v)
                            try {
                                double value = Double.parseDouble(valStr);
                                stats.put(statName, stats.getOrDefault(statName, 0.0) + value);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }

        // 3. XỬ LÝ SET BONUS (Cộng dồn thêm chỉ số nếu mặc đủ bộ)
        if (setCounts.getOrDefault("giant", 0) >= 3) {
            double currentHp = stats.getOrDefault("Máu tối đa", 0.0);
            stats.put("Máu tối đa", currentHp * 1.25); // Set 3 Giant: +25% Máu
        }
        if (setCounts.getOrDefault("armored", 0) >= 3) {
            double currentArmor = stats.getOrDefault("Giáp", 0.0);
            stats.put("Giáp", currentArmor * 1.25); // Set 3 Armored: +25% Giáp
        }
        if (setCounts.getOrDefault("hunter", 0) >= 3) {
            stats.put("Tỉ lệ chí mạng", stats.getOrDefault("Tỉ lệ chí mạng", 0.0) + 8.0);
        }
        // Todo: Thêm tiếp các Set Bonus khác tại đây sau này...

        // 4. KIỂM TRA GIỚI HẠN (HARD CAPS)
        if (stats.getOrDefault("Tỉ lệ chí mạng", 0.0) > 100.0) stats.put("Tỉ lệ chí mạng", 100.0);
        if (stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) > 80.0) stats.put("Tỉ lệ xuyên giáp", 80.0);
        if (stats.getOrDefault("Giảm hồi chiêu", 0.0) > 40.0) stats.put("Giảm hồi chiêu", 40.0);
        if (stats.getOrDefault("Tỉ lệ hút máu", 0.0) > 50.0) stats.put("Tỉ lệ hút máu", 50.0);
        if (stats.getOrDefault("Tốc độ đánh", 0.0) > 100.0) stats.put("Tốc độ đánh", 100.0);

        return stats;
    }
}