package vn.minecraft.rPGCore.brewing;

import vn.minecraft.rPGCore.managers.ItemManager.ItemData;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BrewingEngine {

    public PotionData evaluate(List<ItemData> ingredients, Player alchemist) {
        if (ingredients == null || ingredients.isEmpty()) {
            PotionData fail = new PotionData(UUID.randomUUID().toString());
            fail.isRuined = true;
            return fail;
        }

        boolean isAlchemist = false;
        int alchLevel = 0;

        if (alchemist != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(alchemist);
            if (data != null && data.getJobId().equals("ALCHEMIST")) {
                isAlchemist = true;
                alchLevel = data.getLevel("ALCHEMIST");
            }
        }

        FlavorStats flavors = new FlavorStats(ingredients);
        PotionData result = new PotionData(UUID.randomUUID().toString());

        double efficiencyMultiplier = 1.0;

        // [FIXED]: Chuyển hình phạt vào khâu Nấu Thuốc
        if (!isAlchemist) {
            efficiencyMultiplier *= 0.5;
            result.flavorTags.add("§8[Hàng Nghiệp Dư] §c- Giảm 50% dược tính");
        }

        // Phạt khi bỏ quá 3 nhánh thảo dược cùng loại
        for (int count : flavors.itemCounts.values()) {
            if (count > 3) {
                efficiencyMultiplier *= Math.pow(0.5, count - 3);
            }
        }

        double harshPoints = flavors.getHarshFlavors();
        double sweetPoints = flavors.sweet;
        double finalToxicity = Math.max(0, harshPoints - (sweetPoints * 1.5));

        result.toxicityPoints = finalToxicity * 10.0;

        if (result.toxicityPoints > 30) {
            efficiencyMultiplier *= 0.8;
            result.flavorTags.add("§c[Quá Khó Uống] §7- Bị giảm dược tính");
        }

        double successRate = 40.0 + alchLevel + (flavors.tasteless * 20.0) - (finalToxicity * 15.0);
        successRate = Math.max(5.0, Math.min(100.0, successRate));

        if (Math.random() * 100.0 > successRate) {
            result.isRuined = true;
            result.name = "§8Dược Phẩm Phế Phẩm";
            result.flavorTags.add("§c[Kinh Tởm]");
            return result;
        }

        java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?");

        for (ItemData item : ingredients) {
            String id = item.id.toLowerCase();
            String effect = item.effect != null ? item.effect.toLowerCase() : "";

            double value = 0.0;
            java.util.regex.Matcher m = numberPattern.matcher(effect);
            if (m.find()) value = Double.parseDouble(m.group());

            double finalVal = value * efficiencyMultiplier;

            if (effect.contains("tỉ lệ hồi phục")) result.customStats.put("Tỉ lệ hồi phục", result.customStats.getOrDefault("Tỉ lệ hồi phục", 0.0) + finalVal);
            else if (effect.contains("tỉ lệ hồi năng lượng")) result.customStats.put("Hồi Năng lượng", result.customStats.getOrDefault("Hồi Năng lượng", 0.0) + finalVal);
            else if (effect.contains("tốc độ di chuyển")) result.customStats.put("Tốc độ di chuyển", result.customStats.getOrDefault("Tốc độ di chuyển", 0.0) + finalVal);
            else if (effect.contains("sát thương") && !effect.contains("phép thuật") && !effect.contains("chí mạng")) result.customStats.put("Sát thương (%)", result.customStats.getOrDefault("Sát thương (%)", 0.0) + finalVal);
            else if (effect.contains("% giáp")) result.customStats.put("Tỉ lệ Giáp", result.customStats.getOrDefault("Tỉ lệ Giáp", 0.0) + finalVal);
            else if (effect.contains("tỉ lệ đỡ đòn")) result.customStats.put("Tỉ lệ đỡ đòn", result.customStats.getOrDefault("Tỉ lệ đỡ đòn", 0.0) + finalVal);
            else if (effect.contains("kháng hiệu ứng")) result.customStats.put("Kháng hiệu ứng", result.customStats.getOrDefault("Kháng hiệu ứng", 0.0) + finalVal);
            else if (effect.contains("xuyên giáp")) result.customStats.put("Tỉ lệ xuyên giáp", result.customStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + finalVal);
            else if (effect.contains("kháng phép") && !effect.contains("xuyên")) result.customStats.put("Tỉ lệ Kháng phép", result.customStats.getOrDefault("Tỉ lệ Kháng phép", 0.0) + finalVal);
            else if (effect.contains("né tránh")) result.customStats.put("Tỉ lệ né tránh", result.customStats.getOrDefault("Tỉ lệ né tránh", 0.0) + finalVal);
            else if (effect.contains("chí mạng")) result.customStats.put("Tỉ lệ chí mạng", result.customStats.getOrDefault("Tỉ lệ chí mạng", 0.0) + finalVal);
            else if (effect.contains("hút máu")) result.customStats.put("Tỉ lệ hút máu", result.customStats.getOrDefault("Tỉ lệ hút máu", 0.0) + finalVal);
            else if (effect.contains("máu tối đa")) result.customStats.put("Máu tối đa", result.customStats.getOrDefault("Máu tối đa", 0.0) + finalVal);
            else if (effect.contains("tốc độ đánh")) result.customStats.put("Tốc độ đánh", result.customStats.getOrDefault("Tốc độ đánh", 0.0) + finalVal);
            else if (effect.contains("xuyên kháng phép")) result.customStats.put("Tỉ lệ xuyên kháng phép", result.customStats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + finalVal);

            else if (effect.contains("hồi máu tức thì")) result.customStats.put("Hồi máu tức thì (%)", result.customStats.getOrDefault("Hồi máu tức thì (%)", 0.0) + finalVal);
            else if (effect.contains("hồi năng lượng tức thì")) result.customStats.put("Hồi năng lượng tức thì (%)", result.customStats.getOrDefault("Hồi năng lượng tức thì (%)", 0.0) + finalVal);

            if (effect.contains("bất tử")) result.specialEffects.add("BẤT_TỬ");
            if (effect.contains("thanh tẩy siêu cấp")) result.specialEffects.add("THANH_TẨY");
            if (effect.contains("độc tử thần")) result.specialEffects.add("EXECUTE_POISON");
            if (effect.contains("hồi sinh tại chỗ")) result.specialEffects.add("RESURRECTION");

            if (effect.contains("nhảy cao")) result.potionEffects.add("JUMP");
            if (effect.contains("kháng lửa")) result.potionEffects.add("FIRE_RESISTANCE");
            if (effect.contains("tàng hình")) result.potionEffects.add("INVISIBILITY");
            if (effect.contains("thở dưới nước")) result.potionEffects.add("WATER_BREATHING");

            if (id.contains("mana_shroom") || id.contains("mana_root")) result.isManaPotion = true;
        }

        result.durationMinutes += (int) (flavors.salty * 2);
        if (efficiencyMultiplier < 1.0) result.durationMinutes = Math.max(1, (int)(result.durationMinutes * efficiencyMultiplier));

        if (alchLevel > 0) {
            result.name = "§6Thần Dược Cao Cấp";
            if (finalToxicity == 0 && harshPoints > 0) {
                result.flavorTags.add("§a[Cân Bằng Hoàn Hảo]");
                result.durationMinutes += 2;
            }
        } else {
            result.name = "§aThuốc Sắc Thảo Dược";
        }

        return result;
    }
}