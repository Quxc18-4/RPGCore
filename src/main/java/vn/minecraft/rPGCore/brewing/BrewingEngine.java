package vn.minecraft.rPGCore.brewing;

import vn.minecraft.rPGCore.managers.ItemManager.ItemData;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

public class BrewingEngine {

    // [MỚI]: Tính tỉ lệ thành công hiển thị lên giao diện
    public double getSuccessRate(List<ItemData> ingredients, Player alchemist, boolean isEnhancing) {
        if (ingredients.isEmpty()) return 0;
        int alchLevel = 0;
        if (alchemist != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(alchemist);
            if (data != null && data.getJobId().equals("ALCHEMIST")) alchLevel = data.getLevel("ALCHEMIST");
        }

        FlavorStats flavors = new FlavorStats(ingredients);
        double harshPoints = flavors.getHarshFlavors();
        double finalToxicity = Math.max(0, harshPoints - (flavors.sweet * 1.5));

        double successRate = 40.0 + alchLevel + (flavors.tasteless * 20.0) - (finalToxicity * 15.0);
        if (isEnhancing) successRate -= 20.0; // Cường hóa sẽ khó nổ vạc hơn

        return Math.max(5.0, Math.min(100.0, successRate));
    }

    // [MỚI]: Nhận thêm BasePotion để cường hóa
    public PotionData evaluate(List<ItemData> ingredients, Player alchemist, PotionData basePotion) {
        PotionData result = new PotionData(UUID.randomUUID().toString());

        if (ingredients == null || ingredients.isEmpty()) {
            result.isRuined = true; return result;
        }

        boolean isAlchemist = false;
        if (alchemist != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(alchemist);
            if (data != null && data.getJobId().equals("ALCHEMIST")) isAlchemist = true;
        }

        // 1. KẾ THỪA CHỈ SỐ TỪ THUỐC CŨ (NẾU ĐANG CƯỜNG HÓA)
        if (basePotion != null) {
            result.isEnhanced = true;
            result.name = "§d§lThần Dược Cường Hóa";
            result.durationMinutes = basePotion.durationMinutes;
            result.toxicityPoints = basePotion.toxicityPoints;
            result.isManaPotion = basePotion.isManaPotion;
            result.customStats.putAll(basePotion.customStats);
            result.specialEffects.addAll(basePotion.specialEffects);
            result.potionEffects.addAll(basePotion.potionEffects);
            result.flavorTags.addAll(basePotion.flavorTags);
        }

        FlavorStats flavors = new FlavorStats(ingredients);
        double efficiencyMultiplier = isAlchemist ? 1.0 : 0.5;

        for (int count : flavors.itemCounts.values()) {
            if (count > 3) efficiencyMultiplier *= Math.pow(0.5, count - 3);
        }

        double harshPoints = flavors.getHarshFlavors();
        double sweetPoints = flavors.sweet;
        double finalToxicity = Math.max(0, harshPoints - (sweetPoints * 1.5));

        result.toxicityPoints += (finalToxicity * 10.0);

        if (finalToxicity > 3.0) {
            efficiencyMultiplier *= 0.8;
            if (!result.flavorTags.contains("§c[Quá Khó Uống]")) result.flavorTags.add("§c[Quá Khó Uống]");
        }

        // Tính nổ vạc
        double successRate = getSuccessRate(ingredients, alchemist, basePotion != null);
        if (Math.random() * 100.0 > successRate) {
            result.isRuined = true;
            result.name = "§8Dược Phẩm Phế Thải";
            result.flavorTags.add("§c[Kinh Tởm]");
            return result;
        }

        // 2. PHA MÀU (Dựa trên vị áp đảo nhất)
        if (flavors.spicy >= Math.max(flavors.sweet, flavors.bitter)) { result.colorR = 255; result.colorG = 50; result.colorB = 50; } // Đỏ
        else if (flavors.sweet > flavors.bitter) { result.colorR = 255; result.colorG = 150; result.colorB = 200; } // Hồng
        else if (flavors.bitter > flavors.salty) { result.colorR = 50; result.colorG = 200; result.colorB = 50; } // Xanh lá
        else if (flavors.salty > flavors.sour) { result.colorR = 200; result.colorG = 255; result.colorB = 255; } // Trắng xanh
        else { result.colorR = 255; result.colorG = 255; result.colorB = 50; } // Vàng chua

        // 3. QUÉT THẢO DƯỢC
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

            else if (effect.contains("hồi máu tức thì")) result.customStats.put("Hồi máu tức thì", result.customStats.getOrDefault("Hồi máu tức thì", 0.0) + finalVal);
            else if (effect.contains("hồi năng lượng tức thì")) result.customStats.put("Hồi năng lượng tức thì", result.customStats.getOrDefault("Hồi năng lượng tức thì", 0.0) + finalVal);

            if (effect.contains("bất tử") && !result.specialEffects.contains("BẤT_TỬ")) result.specialEffects.add("BẤT_TỬ");
            if (effect.contains("thanh tẩy siêu cấp") && !result.specialEffects.contains("THANH_TẨY")) result.specialEffects.add("THANH_TẨY");
            if (effect.contains("độc tử thần") && !result.specialEffects.contains("EXECUTE_POISON")) result.specialEffects.add("EXECUTE_POISON");
            if (effect.contains("hồi sinh tại chỗ") && !result.specialEffects.contains("RESURRECTION")) result.specialEffects.add("RESURRECTION");

            if (effect.contains("nhảy cao") && !result.potionEffects.contains("JUMP")) result.potionEffects.add("JUMP");
            if (effect.contains("kháng lửa") && !result.potionEffects.contains("FIRE_RESISTANCE")) result.potionEffects.add("FIRE_RESISTANCE");
            if (effect.contains("tàng hình") && !result.potionEffects.contains("INVISIBILITY")) result.potionEffects.add("INVISIBILITY");
            if (effect.contains("thở dưới nước") && !result.potionEffects.contains("WATER_BREATHING")) result.potionEffects.add("WATER_BREATHING");

            if (id.contains("mana_shroom") || id.contains("mana_root")) result.isManaPotion = true;
        }

        result.durationMinutes += (int) (flavors.salty * 2);

        if (basePotion == null && isAlchemist && finalToxicity == 0 && harshPoints > 0) {
            result.flavorTags.add("§a[Cân Bằng Hoàn Hảo]");
        }

        return result;
    }
}