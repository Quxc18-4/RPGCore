package vn.minecraft.rPGCore.cooking;

import vn.minecraft.rPGCore.managers.ItemManager.IngredientData;
import java.util.List;
import java.util.Map;

public class CookingEngine {

    public FoodItemData evaluate(List<IngredientData> ingredients) {
        FoodItemData result = new FoodItemData();
        NutritionStats stats = new NutritionStats(ingredients);

        double corePoints = stats.getCorePoints();

        // 1. KIỂM DUYỆT RÁC & ĐỘC TỐ
        double detoxPower = ((stats.chili + stats.garlic) / 0.5) * 0.25;
        double remainingToxin = Math.max(0, stats.monster - detoxPower);

        if (corePoints == 0 || (remainingToxin > corePoints * 0.3) || (stats.getTotalSpice() > corePoints)) {
            result.isRuined = true;
            result.freshnessMinutes = 10;
            result.flavorTags.add("§c[Món Ăn Hỏng] §7- Không thể nuốt trôi!");
            return result;
        }
        if (stats.monster > 0 && remainingToxin == 0) result.flavorTags.add("§2[Khử Độc]");

        // 2. TÍNH TOÁN ĐỘ NGẤY & THỜI GIAN
        double baseBoredom = (stats.meat / 0.25) * 5 + (stats.fish / 0.25) * 5 + (stats.fat / 0.5) * 25;
        if (stats.starch > 0) baseBoredom *= 1.5;

        boolean vegNerfsDuration = false;
        if (stats.veg >= 3.0) { baseBoredom -= 100; result.regenRate += 25; vegNerfsDuration = true; }
        else if (stats.veg >= 2.0) { baseBoredom -= 45; result.regenRate += 15; }
        else if (stats.veg >= 1.0) { baseBoredom -= 20; }

        result.boredomPoints = Math.max(0, Math.min(100, (int) baseBoredom));

        double baseDurationSec = (stats.meat / 0.25) * 25 + (stats.fish / 0.25) * 15 + (stats.starch / 0.5) * 40;
        double timeMultiplier = 1.0;
        if (stats.salt > 0 && stats.pepper > 0) timeMultiplier += 0.3;
        else if (stats.salt > 0 || stats.pepper > 0) timeMultiplier += 0.1;

        double fatMultiplier = 1.0;
        if (stats.fat > 0) {
            if (stats.fat <= corePoints) {
                fatMultiplier += (stats.fat / 0.5) * 0.15;
                result.flavorTags.add("§e[Béo Ngậy]");
            } else {
                fatMultiplier = 1.05;
                if (stats.sour > 0) result.flavorTags.add("§e[Thanh Vị] §7- Vị chua khử ngấy!");
                else { result.hasFatDebuff = true; result.flavorTags.add("§e[Quá Ngấy]"); }
            }
        }

        double sugarMultiplier = 1.0 + (Math.floor(stats.sugar) * 0.1);

        double finalDurationSec = baseDurationSec * timeMultiplier * fatMultiplier;
        if (vegNerfsDuration) finalDurationSec *= 0.5;
        result.durationMinutes = Math.max(1, (int) (finalDurationSec / 60));

        // 3. STATS NỘI TẠI (TINH BỘT, CÁ, GIA VỊ)
        double buffMultiplier = fatMultiplier * sugarMultiplier;
        if (stats.starch >= 1.0) result.maxHpBonus = 20 * buffMultiplier;
        if (stats.fish >= 3.5) { result.bonusDmg = 30; result.cdr = 20; }
        else if (stats.fish >= 2.0) { result.bonusDmg = 15; result.cdr = 10; }
        else if (stats.fish >= 1.0) { result.bonusDmg = 15; }

        // Gia vị đặc biệt (Tỏi, Ớt chỉ tính mốc chẵn 1.0 như yêu cầu)
        if (stats.chili >= 1.0) result.moveSpeed += 10 * Math.floor(stats.chili);
        if (stats.garlic >= 1.0) result.atkSpeed += 10 * Math.floor(stats.garlic);
        if (stats.sour >= 0.5) result.armorPen += 15 * (stats.sour / 0.5);

        result.bonusDmg *= buffMultiplier; result.cdr *= buffMultiplier; result.moveSpeed *= buffMultiplier;
        result.atkSpeed *= buffMultiplier; result.armorPen *= buffMultiplier; result.regenRate *= buffMultiplier;

        // 4. ================= XỬ LÝ MA THUẬT =================
        boolean isReversed = stats.magic > corePoints;
        if (isReversed) {
            result.isMagicReversed = true;
            result.flavorTags.add("§5[Đảo Ngược Lời Nguyền]");
            result.bonusDmg *= -1; result.cdr *= -1; result.moveSpeed *= -1;
            result.atkSpeed *= -1; result.armorPen *= -1; result.regenRate *= -1;
        }

        for (Map.Entry<String, Integer> entry : stats.itemCounts.entrySet()) {
            String id = entry.getKey();
            int count = entry.getValue();
            if (!id.startsWith("magic_")) continue;

            // Công thức Cấp số nhân: 1=1, 2=2, 3=4, 4=8...
            double magicMult = Math.pow(2, count - 1) * sugarMultiplier;

            switch (id) {
                case "magic_golden_carrot":
                    if (!isReversed) result.potionEffects.add("NIGHT_VISION:0");
                    else result.potionEffects.add("BLINDNESS:0");
                    break;
                case "magic_blaze_powder":
                    if (!isReversed) result.potionEffects.add("FIRE_RESISTANCE:0");
                    else {
                        result.flavorTags.add("§c[Bỏng Rát] §7- Dễ bốc cháy");
                        result.potionEffects.add("HUNGER:1");
                    }
                    break;
                case "magic_fermented_eye":
                    double regen = 20.0 * magicMult;
                    if (!isReversed) result.regenRate += regen;
                    else result.potionEffects.add("POISON:" + (int) Math.min(4, magicMult/2)); // Bị độc càng nặng
                    break;
                case "magic_pufferfish":
                    if (!isReversed) result.potionEffects.add("WATER_BREATHING:0");
                    else {
                        result.potionEffects.add("BLINDNESS:0");
                        result.potionEffects.add("SLOWNESS:1");
                        result.potionEffects.add("POISON:1");
                    }
                    break;
                case "magic_spider_eye":
                    // Luôn luôn bị độc (Ám sát)
                    result.potionEffects.add("POISON:" + (isReversed ? 2 : 1));
                    break;
                case "magic_bat_wing":
                    double ls = 10.0 * magicMult;
                    result.lifeSteal += isReversed ? -ls : ls;
                    break;
                case "magic_glowstone":
                    double crit = 10.0 * magicMult;
                    double drop = 15.0 * magicMult;
                    if (!isReversed) {
                        result.critRate += crit;
                        result.dropRate += drop;
                    } else {
                        result.critRate -= crit;
                        result.dropRate -= drop;
                        result.potionEffects.add("GLOWING:0");
                    }
                    break;
            }
        }

        // 5. ĐẶT TÊN MÓN ĂN
        if (stats.fish > stats.meat && stats.fish >= 1.0) { result.name = "§b§lHải Sản Hầm Thượng Hạng"; result.material = org.bukkit.Material.RABBIT_STEW; result.freshnessMinutes = 45; }
        else if (stats.meat >= 1.0) { result.name = "§c§lSúp Thịt Nguyên Tảng"; result.material = org.bukkit.Material.RABBIT_STEW; result.freshnessMinutes = 60; }
        else if (stats.veg >= 1.0) { result.name = "§a§lSalad Súp Thanh Lọc"; result.material = org.bukkit.Material.BEETROOT_SOUP; result.freshnessMinutes = 15; }
        else if (stats.starch >= 1.0) { result.name = "§e§lSúp Khoai Nghiền"; result.material = org.bukkit.Material.MUSHROOM_STEW; result.freshnessMinutes = 30; }
        else { result.name = "§f§lSúp Thập Cẩm"; result.freshnessMinutes = 20; }

        if (stats.getTotalSpice() > 1.5) result.flavorTags.add("§6[Đậm Vị] §7- Có thể giải Tuyệt Thực!");

        return result;
    }
}