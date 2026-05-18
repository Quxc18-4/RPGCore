package vn.minecraft.rPGCore.cooking;

import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.managers.ItemManager.ItemData;
import java.util.List;
import java.util.Map;

public class CookingEngine {

    public FoodItemData evaluate(List<ItemData> ingredients, org.bukkit.entity.Player cook) {
        FoodItemData result = new FoodItemData();
        NutritionStats stats = new NutritionStats(ingredients);

        double cookMultiplier = 1.0;
        int chefLevel = 0;

        // KIỂM TRA CÁC KỸ NĂNG TỐI THƯỢNG
        boolean hasIronStomach = false;
        boolean hasMagicHands = false;

        if (cook != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(cook);
            if (data != null && data.getJobId().equals("CHEF")) {
                chefLevel = data.getLevel("CHEF");
                hasIronStomach = data.hasUnlockedSkill("CHEF", "CHEF_IRON_STOMACH");
                hasMagicHands = data.hasUnlockedSkill("CHEF", "CHEF_MAGIC_HANDS");

                double bonusChance = (chefLevel / 50.0) * 40.0;

                for (ItemData item : ingredients) {
                    if (item.category == ItemManager.Category.SPICE || item.category == ItemManager.Category.FAT) continue;
                    if (Math.random() * 100 <= bonusChance) {
                        if (item.category == ItemManager.Category.MAGIC) {
                            if (Math.random() * 100 <= (bonusChance / 2.0)) stats.magic += 0.25;
                        } else {
                            if (item.category == ItemManager.Category.MEAT) stats.meat += 0.5;
                            else if (item.category == ItemManager.Category.FISH) stats.fish += 0.5;
                            else if (item.category == ItemManager.Category.VEG) stats.veg += 0.5;
                            else if (item.category == ItemManager.Category.STARCH) stats.starch += 0.5;
                            else if (item.category == ItemManager.Category.MONSTER) stats.monster += 0.5;
                        }
                    }
                }
                cookMultiplier = 1.0 + ((chefLevel / 50.0) * 0.30);
            }
        }

        boolean hasPreservative = ingredients.stream().anyMatch(i -> i.id.equals("misc_preservative_powder"));
        double corePoints = stats.getCorePoints();

        // 1. KIỂM DUYỆT RÁC & ĐỘC TỐ
        double detoxPower = ((stats.chili + stats.garlic) / 0.5) * 0.25;
        double remainingToxin = Math.max(0, stats.monster - detoxPower);

        if (!hasPreservative && (corePoints == 0 || (remainingToxin > corePoints * 0.3) || (stats.getTotalSpice() > corePoints))) {
            result.isRuined = true; result.freshnessMinutes = 10;
            result.flavorTags.add("§c[Món Ăn Hỏng] §7- Không thể nuốt trôi!"); return result;
        }
        if (stats.monster > 0 && remainingToxin == 0) result.flavorTags.add("§2[Khử Độc]");

        // 2. TÍNH TOÁN ĐỘ NGẤY & THỜI GIAN
        double baseBoredom = (stats.meat / 0.25) * 5 + (stats.fish / 0.25) * 5 + (stats.fat / 0.5) * 25;
        if (stats.starch > 0) baseBoredom *= 1.5;

        boolean vegNerfsDuration = false;
        if (stats.veg >= 3.0) { baseBoredom -= 100; result.regenRate += 25; vegNerfsDuration = true; }
        else if (stats.veg >= 2.0) { baseBoredom -= 45; result.regenRate += 15; }
        else if (stats.veg >= 1.0) { baseBoredom -= 20; }

        // [KỸ NĂNG TỐI THƯỢNG]: Dạ Dày Thép (-50% Tỉ lệ nhận điểm ngấy)
        if (hasIronStomach) baseBoredom *= 0.5;
        result.boredomPoints = Math.max(0, Math.min(100, (int) baseBoredom));

        double baseDurationSec = (stats.meat / 0.25) * 25 + (stats.fish / 0.25) * 15 + (stats.starch / 0.5) * 40;
        double timeMultiplier = 1.0;
        if (stats.salt > 0 && stats.pepper > 0) timeMultiplier += 0.3;
        else if (stats.salt > 0 || stats.pepper > 0) timeMultiplier += 0.1;

        double fatMultiplier = 1.0;
        if (stats.fat > 0) {
            if (stats.fat <= corePoints) { fatMultiplier += (stats.fat / 0.5) * 0.15; result.flavorTags.add("§e[Béo Ngậy]"); }
            else {
                fatMultiplier = 1.05;
                if (stats.sour > 0) result.flavorTags.add("§e[Thanh Vị] §7- Vị chua khử ngấy!");
                else { result.hasFatDebuff = true; result.flavorTags.add("§e[Quá Ngấy]"); }
            }
        }

        double sugarMultiplier = 1.0 + (Math.floor(stats.sugar) * 0.1);

        // [KỸ NĂNG TỐI THƯỢNG]: Bàn Tay Ma Thuật (+20% mọi Buff tương đương 2 Đường)
        if (hasMagicHands) sugarMultiplier += 0.2;

        double finalDurationSec = baseDurationSec * timeMultiplier * fatMultiplier;
        if (vegNerfsDuration) finalDurationSec *= 0.5;

        // [KỸ NĂNG TỐI THƯỢNG]: Bàn Tay Ma Thuật (x2 Thời gian No căng)
        if (hasMagicHands) finalDurationSec *= 2.0;
        result.durationMinutes = Math.max(1, (int) (finalDurationSec / 60));

        // 3. STATS NỘI TẠI
        double buffMultiplier = fatMultiplier * sugarMultiplier;
        if (stats.starch >= 1.0) result.maxHpBonus = 200 * buffMultiplier;
        if (stats.fish >= 3.5) { result.bonusDmg = 30; result.cdr = 20; }
        else if (stats.fish >= 2.0) { result.bonusDmg = 15; result.cdr = 10; }
        else if (stats.fish >= 1.0) { result.bonusDmg = 15; }

        if (stats.chili >= 1.0) result.moveSpeed += 10 * Math.floor(stats.chili);
        if (stats.garlic >= 1.0) result.atkSpeed += 10 * Math.floor(stats.garlic);
        if (stats.sour >= 0.5) result.armorPen += 15 * (stats.sour / 0.5);

        result.bonusDmg *= buffMultiplier; result.cdr *= buffMultiplier; result.moveSpeed *= buffMultiplier;
        result.atkSpeed *= buffMultiplier; result.armorPen *= buffMultiplier; result.regenRate *= buffMultiplier;

        // 4. XỬ LÝ MA THUẬT (Giữ nguyên)
        boolean isReversed = stats.magic > corePoints;
        if (isReversed) {
            result.isMagicReversed = true; result.flavorTags.add("§5[Đảo Ngược Lời Nguyền]");
            result.bonusDmg *= -1; result.cdr *= -1; result.moveSpeed *= -1;
            result.atkSpeed *= -1; result.armorPen *= -1; result.regenRate *= -1;
        }

        for (Map.Entry<String, Integer> entry : stats.itemCounts.entrySet()) {
            String id = entry.getKey(); int count = entry.getValue();
            if (!id.startsWith("magic_")) continue;
            double magicMult = count * sugarMultiplier;

            switch (id) {
                // --- NHÓM 1: ĐỒ RỚT TỪ QUÁI VẬT ---
                case "magic_blaze_powder": if (!isReversed) result.potionEffects.add("FIRE_RESISTANCE:0"); else { result.flavorTags.add("§c[Bỏng Rát]"); result.potionEffects.add("HUNGER:1"); } break;
                case "magic_fermented_eye": double regen = 10.0 * magicMult; if (!isReversed) result.regenRate += regen; else result.potionEffects.add("POISON:" + (int) Math.min(4, magicMult/2)); break;
                case "magic_pufferfish": if (!isReversed) result.potionEffects.add("WATER_BREATHING:0"); else { result.potionEffects.add("BLINDNESS:0"); result.potionEffects.add("SLOWNESS:1"); result.potionEffects.add("POISON:1"); } break;
                case "magic_spider_eye": result.potionEffects.add("POISON:" + (isReversed ? 2 : 1)); break;
                case "magic_toxic_blood": if (!isReversed) { result.potionEffects.add("POISON:" + (int) Math.min(4, magicMult)); result.potionEffects.add("WITHER:0"); result.flavorTags.add("§c[Kịch Độc]"); } else { result.bonusDmg += 10.0 * magicMult; result.flavorTags.add("§4[Sôi Máu]"); } break;
                case "magic_bat_wing": result.lifeSteal += isReversed ? -(10.0 * magicMult) : (10.0 * magicMult); break;
                case "magic_goblin_ear": double dropBonus = 10.0 * magicMult; if (!isReversed) result.dropRate += dropBonus; else { result.dropRate -= dropBonus; result.potionEffects.add("UNLUCK:0"); result.flavorTags.add("§c[Đen Đủi]"); } break;
                case "magic_glowstone": double crit = 10.0 * magicMult; if (!isReversed) result.critRate += crit; else { result.critRate -= crit; result.potionEffects.add("GLOWING:0"); } break;

                // --- NHÓM 2: NÔNG SẢN CỦA NÔNG DÂN ---
                case "magic_golden_carrot":
                    if (!isReversed) result.potionEffects.add("NIGHT_VISION:0"); else result.potionEffects.add("BLINDNESS:0");
                    break;
                case "magic_flame_beetroot":
                    if (!isReversed) { result.potionEffects.add("FIRE_RESISTANCE:0"); result.moveSpeed += 15.0 * magicMult; }
                    else { result.flavorTags.add("§c[Bỏng Rát]"); result.potionEffects.add("HUNGER:1"); result.moveSpeed -= 15.0 * magicMult; }
                    break;
                case "magic_golden_melon":
                    if (!isReversed) result.maxHpBonus += 50.0 * magicMult; else result.maxHpBonus -= 50.0 * magicMult;
                    break;
                case "magic_glowing_mushroom":
                    if (!isReversed) { result.critRate += 5.0 * magicMult; result.potionEffects.add("GLOWING:0"); }
                    else { result.critRate -= 5.0 * magicMult; result.potionEffects.add("BLINDNESS:0"); }
                    break;
                case "magic_star_wheat":
                    if (!isReversed) result.dropRate += 10.0 * magicMult;
                    else { result.dropRate -= 10.0 * magicMult; result.potionEffects.add("UNLUCK:0"); }
                    break;
                case "magic_soul_sweet_berry":
                    if (!isReversed) result.lifeSteal += 5.0 * magicMult; else result.lifeSteal -= 5.0 * magicMult;
                    break;
                case "magic_crystal_kelp":
                    if (!isReversed) { result.armorPen += 10.0 * magicMult; result.potionEffects.add("WATER_BREATHING:0"); }
                    else { result.armorPen -= 10.0 * magicMult; result.potionEffects.add("POISON:0"); }
                    break;
                case "magic_dragon_cocoa":
                    if (!isReversed) result.bonusDmg += 10.0 * magicMult; else result.bonusDmg -= 10.0 * magicMult;
                    break;
                case "magic_mana_potato":
                    if (!isReversed) result.cdr += 5.0 * magicMult; else result.cdr -= 5.0 * magicMult;
                    break;
                case "magic_chaos_glow_berry":
                    if (!isReversed) result.atkSpeed += 10.0 * magicMult; else result.atkSpeed -= 10.0 * magicMult;
                    break;
            }
        }

        // 5. ĐẶT TÊN MÓN ĂN
        if (stats.fish > stats.meat && stats.fish >= 1.0) { result.name = "§b§lHải Sản Hầm Thượng Hạng"; result.material = org.bukkit.Material.RABBIT_STEW; result.freshnessMinutes = 45; }
        else if (stats.meat >= 1.0) { result.name = "§c§lSúp Thịt Nguyên Tảng"; result.material = org.bukkit.Material.RABBIT_STEW; result.freshnessMinutes = 60; }
        else if (stats.veg >= 1.0) { result.name = "§a§lSalad Súp Thanh Lọc"; result.material = org.bukkit.Material.BEETROOT_SOUP; result.freshnessMinutes = 15; }
        else if (stats.starch >= 1.0) { result.name = "§e§lSúp Khoai Nghiền"; result.material = org.bukkit.Material.MUSHROOM_STEW; result.freshnessMinutes = 30; }
        else { result.name = "§f§lSúp Thập Cẩm"; result.freshnessMinutes = 20; }

        if (stats.getTotalSpice() > 1.5) result.flavorTags.add("§6[Đậm Vị]");

        // 6. XỬ LÝ CƯỜNG HÓA VÀ GIẤU ĐỘC (CHEF)
        result.bonusDmg *= cookMultiplier; result.cdr *= cookMultiplier; result.moveSpeed *= cookMultiplier;
        result.atkSpeed *= cookMultiplier; result.armorPen *= cookMultiplier; result.regenRate *= cookMultiplier;
        result.maxHpBonus *= cookMultiplier; result.lifeSteal *= cookMultiplier; result.critRate *= cookMultiplier; result.dropRate *= cookMultiplier;

        if (chefLevel > 0) {
            boolean hasPoison = result.potionEffects.stream().anyMatch(e -> e.contains("POISON") || e.contains("WITHER"));
            if (hasPoison && Math.random() * 100 <= chefLevel * 1.5) {
                result.isHiddenPoison = true;
                result.flavorTags.removeIf(tag -> tag.contains("[Kịch Độc]") || tag.contains("[Đảo Ngược Lời Nguyền]"));
                if (result.name.contains("Hỏng") || result.name.contains("Kì Lạ")) result.name = "§a§lSúp Hầm Thơm Ngon";
            }
        }

        // [KỸ NĂNG TỐI THƯỢNG]: Dạ Dày Thép (+50% thời gian tươi)
        if (hasIronStomach) result.freshnessMinutes = (int) (result.freshnessMinutes * 1.5);

        if (hasPreservative) {
            result.freshnessMinutes = 999999;
            result.flavorTags.add("§d[Bảo Quản Vĩnh Cửu] §7- Không bao giờ thiu");
        }

        return result;
    }
}