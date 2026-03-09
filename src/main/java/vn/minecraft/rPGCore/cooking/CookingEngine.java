package vn.minecraft.rPGCore.cooking;

import vn.minecraft.rPGCore.managers.IngredientManager.IngredientData;
import java.util.List;

public class CookingEngine {

    public FoodItemData evaluate(List<IngredientData> ingredients) {
        FoodItemData result = new FoodItemData();
        NutritionStats stats = new NutritionStats(ingredients);

        double corePoints = stats.getCorePoints();

        // ==========================================
        // 1. KIỂM DUYỆT ĐỘC TỐ & RÁC (RUINED CHECK)
        // ==========================================
        double detoxPower = ((stats.chili + stats.garlic) / 0.5) * 0.25;
        double remainingToxin = Math.max(0, stats.monster - detoxPower);

        if (corePoints == 0 || (remainingToxin > corePoints * 0.3) || (stats.getTotalSpice() > corePoints)) {
            result.isRuined = true;
            result.freshnessMinutes = 10;
            result.flavorTags.add("§c[Món Ăn Hỏng] §7- Không thể nuốt trôi!");
            return result; // Trả về luôn món ăn hỏng, bỏ qua các bước buff
        }

        if (stats.monster > 0 && remainingToxin == 0) result.flavorTags.add("§2[Khử Độc]");

        // ==========================================
        // 2. TÍNH TOÁN ĐỘ NGẤY (BOREDOM)
        // ==========================================
        double baseBoredom = (stats.meat / 0.25) * 5
                + (stats.fish / 0.25) * 5
                + (stats.fat / 0.5) * 25;

        if (stats.starch > 0) baseBoredom *= 1.5; // Tinh bột x1.5 tổng ngấy

        // Rau thanh lọc ngấy
        boolean vegNerfsDuration = false;
        if (stats.veg >= 3.0) {
            baseBoredom -= 100;
            result.regenRate += 25;
            vegNerfsDuration = true;
        } else if (stats.veg >= 2.0) {
            baseBoredom -= 45;
            result.regenRate += 15;
        } else if (stats.veg >= 1.0) {
            baseBoredom -= 20;
        }

        result.boredomPoints = Math.max(0, Math.min(100, (int) baseBoredom));

        // ==========================================
        // 3. TÍNH TOÁN THỜI GIAN NO (DURATION)
        // ==========================================
        double baseDurationSec = (stats.meat / 0.25) * 25
                + (stats.fish / 0.25) * 15
                + (stats.starch / 0.5) * 40;

        double timeMultiplier = 1.0;
        if (stats.salt > 0 && stats.pepper > 0) timeMultiplier += 0.3; // Combo 30%
        else if (stats.salt > 0 || stats.pepper > 0) timeMultiplier += 0.1; // Lẻ 10%

        // ==========================================
        // 4. HỆ SỐ CHẤT BÉO & ĐƯỜNG (FAT & SUGAR)
        // ==========================================
        double fatMultiplier = 1.0;
        if (stats.fat > 0) {
            if (stats.fat <= corePoints) {
                fatMultiplier += (stats.fat / 0.5) * 0.15; // Mỗi 0.5 điểm + 15%
                result.flavorTags.add("§e[Béo Ngậy]");
            } else {
                fatMultiplier = 1.05; // Quá liều
                if (stats.sour > 0) {
                    result.flavorTags.add("§e[Thanh Vị] §7- Vị chua khử ngấy!");
                } else {
                    result.hasFatDebuff = true; // Sẽ xử lý -20% Speed ở Listener
                    result.flavorTags.add("§e[Quá Ngấy]");
                }
            }
        }

        double sugarMultiplier = 1.0 + (Math.floor(stats.sugar) * 0.1); // Mỗi 1.0 Đường = +10% hiệu quả Buff

        // Áp dụng Thời gian
        double finalDurationSec = baseDurationSec * timeMultiplier * fatMultiplier;
        if (vegNerfsDuration) finalDurationSec *= 0.5;
        result.durationMinutes = Math.max(1, (int) (finalDurationSec / 60));

        // ==========================================
        // 5. TÍNH TOÁN BUFF NỘI TẠI (STATS)
        // ==========================================
        double buffMultiplier = fatMultiplier * sugarMultiplier;

        // Buff Tinh Bột
        if (stats.starch >= 1.0) result.maxHpBonus = 20 * buffMultiplier; // Ví dụ +20 HP

        // Buff Cá
        if (stats.fish >= 3.5) { result.bonusDmg = 30; result.cdr = 20; }
        else if (stats.fish >= 2.0) { result.bonusDmg = 15; result.cdr = 10; }
        else if (stats.fish >= 1.0) { result.bonusDmg = 15; }

        // Buff Gia Vị & Chua
        if (stats.chili > 0) result.moveSpeed += 10;
        if (stats.garlic > 0) result.atkSpeed += 10;
        if (stats.sour > 0) result.armorPen += 15;

        // Áp dụng Hệ số cho Buff
        result.bonusDmg *= buffMultiplier;
        result.cdr *= buffMultiplier;
        result.moveSpeed *= buffMultiplier;
        result.atkSpeed *= buffMultiplier;
        result.armorPen *= buffMultiplier;
        result.regenRate *= buffMultiplier;

        // Xử lý Ma Thuật
        if (stats.magic > corePoints) {
            result.isMagicReversed = true;
            result.flavorTags.add("§5[Đảo Ngược Lời Nguyền]");
            // Nghịch đảo toàn bộ buff
            result.bonusDmg *= -1; result.cdr *= -1; result.moveSpeed *= -1;
            result.atkSpeed *= -1; result.armorPen *= -1; result.regenRate *= -1;
        }

        // ==========================================
        // 6. PHÂN LOẠI MÓN ĂN & TÊN GỌI
        // ==========================================
        if (stats.fish > stats.meat && stats.fish >= 1.0) {
            result.name = "§b§lHải Sản Hầm Thượng Hạng";
            result.material = org.bukkit.Material.RABBIT_STEW;
            result.freshnessMinutes = 45;
        } else if (stats.meat >= 1.0) {
            result.name = "§c§lSúp Thịt Nguyên Tảng";
            result.material = org.bukkit.Material.RABBIT_STEW;
            result.freshnessMinutes = 60;
        } else if (stats.veg >= 1.0) {
            result.name = "§a§lSalad Súp Thanh Lọc";
            result.material = org.bukkit.Material.BEETROOT_SOUP;
            result.freshnessMinutes = 15;
        } else if (stats.starch >= 1.0) {
            result.name = "§e§lSúp Khoai Nghiền";
            result.material = org.bukkit.Material.MUSHROOM_STEW;
            result.freshnessMinutes = 30;
        } else {
            result.name = "§f§lSúp Thập Cẩm";
            result.freshnessMinutes = 20;
        }

        if (stats.getTotalSpice() > 1.5) result.flavorTags.add("§6[Đậm Vị] §7- Có thể giải Tuyệt Thực!");

        return result;
    }
}