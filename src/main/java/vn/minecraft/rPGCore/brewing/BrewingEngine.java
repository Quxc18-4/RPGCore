package vn.minecraft.rPGCore.brewing;

import vn.minecraft.rPGCore.managers.ItemManager.ItemData;
import org.bukkit.entity.Player;

import java.util.*;

public class BrewingEngine {

    // ==========================================
    // KHỞI TẠO 10 CÔNG THỨC THẤT TRUYỀN
    // ==========================================
    public static class SecretRecipe {
        public String recipeId;
        public String potionName;
        public List<String> requiredItems;
        public String specialEffect;
        public Map<String, Double> customStats = new HashMap<>();
        public int r, g, b;

        public SecretRecipe(String id, String name, int r, int g, int b, String effect, String... items) {
            this.recipeId = id; this.potionName = name; this.r = r; this.g = g; this.b = b;
            this.specialEffect = effect; this.requiredItems = Arrays.asList(items);
        }
    }

    public static final List<SecretRecipe> SECRET_RECIPES = new ArrayList<>();
    static {
        // LƯU Ý: Bạn có thể tự thay đổi các ID ("water_pure", "herb_blood"...) cho khớp với items.yml của bạn!
        SECRET_RECIPES.add(new SecretRecipe("RECIPE_IMMORTAL", "§e§lThần Dược Bất Tử", 255, 215, 0, "IMMORTAL", "water_pure", "herb_phoenix_tear", "herb_dragon_root"));
        SECRET_RECIPES.add(new SecretRecipe("RECIPE_RESURRECT", "§d§lGiọt Lệ Niết Bàn", 255, 105, 180, "RESURRECTION", "water_magic", "herb_angel_feather", "herb_soul_flower"));
        SECRET_RECIPES.add(new SecretRecipe("RECIPE_CLEANSE", "§b§lThánh Thủy Tinh Khiết", 0, 255, 255, "CLEANSE", "water_pure", "herb_white_lotus", "herb_moon_grass"));
        SECRET_RECIPES.add(new SecretRecipe("RECIPE_EXECUTE", "§5§lĐộc Tử Thần", 75, 0, 130, "EXECUTE_POISON", "water_swamp", "herb_death_cap", "herb_widow_bite"));

        SecretRecipe heal100 = new SecretRecipe("RECIPE_HEAL_100", "§a§lThần Dược Hồi Sinh Cơ", 50, 255, 50, null, "water_pure", "herb_golden_apple", "herb_blood_grass");
        heal100.customStats.put("Hồi máu tức thì (%)", 100.0); SECRET_RECIPES.add(heal100);

        SecretRecipe mana100 = new SecretRecipe("RECIPE_MANA_100", "§3§lBình Ma Lực Tối Thượng", 0, 191, 255, null, "water_magic", "herb_mana_shroom", "herb_star_dust");
        mana100.customStats.put("Hồi năng lượng tức thì (%)", 100.0); SECRET_RECIPES.add(mana100);

        SecretRecipe speed = new SecretRecipe("RECIPE_SPEED", "§f§lCuồng Phong Dược", 255, 255, 255, null, "water_river", "herb_wind_leaf", "herb_lightning_root");
        speed.customStats.put("Tốc độ di chuyển", 50.0); SECRET_RECIPES.add(speed);

        SecretRecipe shield = new SecretRecipe("RECIPE_SHIELD", "§8§lThiết Giáp Tinh Trạch", 105, 105, 105, null, "water_sea", "herb_iron_bark", "herb_turtle_shell");
        shield.customStats.put("Tỉ lệ Giáp", 40.0); SECRET_RECIPES.add(shield);

        SecretRecipe berserk = new SecretRecipe("RECIPE_BERSERK", "§4§lHuyết Sát Dược", 139, 0, 0, null, "water_swamp", "herb_demon_horn", "herb_blood_grass");
        berserk.customStats.put("Sát thương (%)", 30.0); berserk.customStats.put("Tỉ lệ chí mạng", 20.0); SECRET_RECIPES.add(berserk);

        SecretRecipe luck = new SecretRecipe("RECIPE_LUCK", "§6§lPhúc Lộc Dược", 255, 204, 0, null, "water_pure", "herb_clover", "herb_gold_dust");
        luck.customStats.put("Tỉ lệ né tránh", 30.0); SECRET_RECIPES.add(luck);
    }

    public double getSuccessRate(List<ItemData> ingredients, Player alchemist, boolean isEnhancing) {
        if (ingredients.isEmpty()) return 0;

        FlavorStats flavors = new FlavorStats(ingredients);
        double finalToxicity = Math.max(0, flavors.getHarshFlavors() - (flavors.sweet * 1.5));

        // Base 60% gốc
        double successRate = 60.0 + (flavors.tasteless * 20.0) - (finalToxicity * 15.0);

        // [NHÚNG LOGIC]: Lấy tỉ lệ cộng thêm từ Nghề Dược Sư (Bao gồm Nội tại & Ultimate)
        if (alchemist != null) {
            successRate += vn.minecraft.rPGCore.jobs.types.AlchemistJob.getSuccessRateBonus(alchemist);
        }

        int activeIngredients = ingredients.size();
        if (activeIngredients > 2) {
            successRate -= (activeIngredients - 2) * 5.0;
        }

        if (isEnhancing) successRate -= 20.0;

        // Tác động của Dung môi lên tỉ lệ thành công
        for (ItemData ing : ingredients) {
            if ("catalyst_purity".equals(ing.id)) successRate += 15.0;
            if ("catalyst_chaos".equals(ing.id)) successRate -= 20.0;
        }

        return Math.max(5.0, Math.min(100.0, successRate));
    }

    public PotionData evaluate(List<ItemData> ingredients, Player alchemist, PotionData basePotion) {
        PotionData result = new PotionData(UUID.randomUUID().toString());
        if (ingredients == null || ingredients.isEmpty()) { result.isRuined = true; return result; }

        boolean isAlchemist = false;
        int alchLevel = 0;

        if (alchemist != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(alchemist);
            if (data != null && data.getJobId().equals("ALCHEMIST")) {
                isAlchemist = true;
                alchLevel = data.getLevel("ALCHEMIST");
            }
        }

        // ---------------------------------------------------------
        // [GIAI ĐOẠN 3]: KIỂM TRA XEM CÓ KHỚP VỚI CÔNG THỨC ẨN NÀO KHÔNG
        // ---------------------------------------------------------
        List<String> inputIds = new ArrayList<>();
        for (ItemData ing : ingredients) inputIds.add(ing.id);

        SecretRecipe matchedRecipe = null;
        for (SecretRecipe recipe : SECRET_RECIPES) {
            if (inputIds.size() == recipe.requiredItems.size() && inputIds.containsAll(recipe.requiredItems)) {
                matchedRecipe = recipe;
                break;
            }
        }

        if (matchedRecipe != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(alchemist);
            // Kiểm tra xem người chơi đã cắn sách học công thức này chưa?
            if (isAlchemist && data.hasUnlockedSkill("ALCH_RECIPE", matchedRecipe.recipeId)) {
                result.name = matchedRecipe.potionName;
                result.colorR = matchedRecipe.r; result.colorG = matchedRecipe.g; result.colorB = matchedRecipe.b;
                result.durationMinutes = 30; // Thuốc thần mặc định 30 phút
                result.toxicityPoints = 0;   // Thuốc thần không có độc
                if (matchedRecipe.specialEffect != null) result.specialEffects.add(matchedRecipe.specialEffect);
                result.customStats.putAll(matchedRecipe.customStats);
                result.flavorTags.add("§d[Thần Dược Thất Truyền]");
                return result; // Trả về luôn Thần dược, bỏ qua các bước tính toán lằng nhằng phía dưới!
            } else {
                // Xếp đúng đồ nhưng chưa học sách? Cho nổ vạc luôn!
                result.isRuined = true;
                result.name = "§8Hỗn Hợp Bất Ổn";
                result.flavorTags.add("§c[Sai Cấu Trúc Ma Thuật] §7- Bạn chưa đủ kiến thức để tinh chế công thức này.");
                return result;
            }
        }

        if (basePotion != null) {
            result.isEnhanced = true;
            result.durationMinutes = basePotion.durationMinutes;
            result.toxicityPoints = basePotion.toxicityPoints;
            result.isManaPotion = basePotion.isManaPotion;
            result.customStats.putAll(basePotion.customStats);
            result.specialEffects.addAll(basePotion.specialEffects);
            result.potionEffects.addAll(basePotion.potionEffects);
            result.flavorTags.addAll(basePotion.flavorTags);
        }

        FlavorStats flavors = new FlavorStats(ingredients);

        // [NHÚNG LOGIC]: Người thường 0.5. Alchemist tự động scale lên 1.0 -> 1.5. Có bạo kích Ult sẽ lên 2.0
        double efficiencyMultiplier = 0.5;
        if (alchemist != null) {
            boolean[] isCritEnhanced = new boolean[]{false};
            efficiencyMultiplier = vn.minecraft.rPGCore.jobs.types.AlchemistJob.getStatMultiplier(alchemist, isCritEnhanced);
            if (isCritEnhanced[0]) {
                result.isEnhanced = true; // Đánh dấu đột biến từ Ultimate 1
            }
        }

        for (int count : flavors.itemCounts.values()) {
            if (count > 3) efficiencyMultiplier *= Math.pow(0.5, count - 3);
        }

        double harshPoints = flavors.getHarshFlavors();
        double sweetPoints = flavors.sweet;
        double finalToxicity = Math.max(0, harshPoints - (sweetPoints * 1.5));

        result.toxicityPoints += (finalToxicity * 10.0);

        if (finalToxicity > 3.0) {
            efficiencyMultiplier *= 0.8;
            if (!result.flavorTags.contains("§c[Quá Khó Uống]")) result.flavorTags.add("§c[Quá Khó Uống] §7- Bị giảm dược tính");
        }

        double successRate = getSuccessRate(ingredients, alchemist, basePotion != null);
        if (Math.random() * 100.0 > successRate) {
            result.isRuined = true;
            result.name = "§8Dược Phẩm Phế Thải";
            result.flavorTags.add("§c[Kinh Tởm]");
            return result;
        }

        // PHA MÀU
        if (flavors.spicy >= Math.max(flavors.sweet, flavors.bitter)) { result.colorR = 255; result.colorG = 50; result.colorB = 50; }
        else if (flavors.sweet > flavors.bitter) { result.colorR = 255; result.colorG = 150; result.colorB = 200; }
        else if (flavors.bitter > flavors.salty) { result.colorR = 50; result.colorG = 200; result.colorB = 50; }
        else if (flavors.salty > flavors.sour) { result.colorR = 200; result.colorG = 255; result.colorB = 255; }
        else { result.colorR = 255; result.colorG = 255; result.colorB = 50; }

        // [CẬP NHẬT GIAI ĐOẠN 3]: Tích hợp Dung môi xúc tác
        boolean hasPurityCatalyst = false;
        boolean hasChaosCatalyst = false;

        java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?");

        // Quét Dung Môi trước để lấy hệ số
        for (ItemData item : ingredients) {
            if ("catalyst_purity".equals(item.id)) hasPurityCatalyst = true;
            if ("catalyst_chaos".equals(item.id)) hasChaosCatalyst = true;
        }

        if (hasPurityCatalyst) efficiencyMultiplier *= 0.90; // Phạt 10% Stats
        if (hasChaosCatalyst) efficiencyMultiplier *= 1.25;  // Thưởng 25% Stats

        // Biến tạm để cộng dồn cấp độ hiệu ứng Vanilla
        Map<String, Integer> effectLevels = new HashMap<>();

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
            else if (effect.contains("sát thương") && !effect.contains("phép thuật") && !effect.contains("chí mạng")) result.customStats.put("Sát thương", result.customStats.getOrDefault("Sát thương", 0.0) + finalVal);
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

            if (effect.contains("bất tử") && !result.specialEffects.contains("IMMORTAL")) result.specialEffects.add("IMMORTAL");
            if (effect.contains("thanh tẩy siêu cấp") && !result.specialEffects.contains("CLEANSE")) result.specialEffects.add("CLEANSE");
            if (effect.contains("độc tử thần") && !result.specialEffects.contains("EXECUTE_POISON")) result.specialEffects.add("EXECUTE_POISON");
            if (effect.contains("hồi sinh tại chỗ") && !result.specialEffects.contains("RESURRECTION")) result.specialEffects.add("RESURRECTION");

            // [FIX CỘNG DỒN]: Đếm số lượng để tăng Cấp (Amplifier)
            if (effect.contains("nhảy cao")) effectLevels.put("JUMP", effectLevels.getOrDefault("JUMP", 0) + 1);
            if (effect.contains("kháng lửa")) effectLevels.put("FIRE_RESISTANCE", effectLevels.getOrDefault("FIRE_RESISTANCE", 0) + 1);
            if (effect.contains("tàng hình")) effectLevels.put("INVISIBILITY", effectLevels.getOrDefault("INVISIBILITY", 0) + 1);
            if (effect.contains("thở dưới nước")) effectLevels.put("WATER_BREATHING", effectLevels.getOrDefault("WATER_BREATHING", 0) + 1);

            if (id.contains("mana_shroom") || id.contains("mana_root")) result.isManaPotion = true;
        }

        // Nạp các hiệu ứng cộng dồn vào List (Dạng: "JUMP:2")
        for (Map.Entry<String, Integer> entry : effectLevels.entrySet()) {
            result.potionEffects.add(entry.getKey() + ":" + entry.getValue());
        }

        result.durationMinutes += (int) (flavors.salty * 2);
        if (efficiencyMultiplier < 1.0) result.durationMinutes = Math.max(1, (int)(result.durationMinutes * efficiencyMultiplier));

        // ==========================================
        // THUẬT TOÁN ĐỊNH DANH TÊN THUỐC
        // ==========================================
        boolean hasHeal = result.customStats.containsKey("Hồi máu tức thì (%)") || result.specialEffects.contains("HEAL_INSTANT");
        boolean hasMana = result.customStats.containsKey("Hồi năng lượng tức thì (%)") || result.specialEffects.contains("RESTORE_MANA");
        boolean hasVanilla = !result.potionEffects.isEmpty();
        boolean hasStats = false;

        for (String key : result.customStats.keySet()) {
            if (!key.contains("tức thì")) hasStats = true;
        }

        int types = 0;
        if (hasHeal) types++;
        if (hasMana) types++;
        if (hasVanilla) types++;
        if (hasStats) types++;

        String baseName = "Thuốc Sắc Thảo Dược";
        if (types > 1) baseName = "Thuốc Hỗn Hợp";
        else if (hasHeal) baseName = "Thuốc Hồi Phục";
        else if (hasMana) baseName = "Bình Ma Lực";
        else if (hasStats) baseName = "Thuốc Tăng Cường";
        else if (hasVanilla) baseName = "Thuốc Đa Dụng";

        // [NHÚNG LOGIC]: Chỉnh tên màu sắc theo Level Dược sư & Bạo kích
        if (basePotion != null) {
            result.name = "§d§l" + baseName + " Cường Hóa";
        } else if (result.isEnhanced && vn.minecraft.rPGCore.jobs.types.AlchemistJob.hasUltimate(alchemist, "ALCH_ULT_1")) {
            result.name = "§5§l" + baseName + " Đại Tài (200%)";
        } else if (isAlchemist && alchLevel >= 40) {
            result.name = "§6" + baseName + " Cô Đặc";
        } else if (isAlchemist && alchLevel >= 20) {
            result.name = "§e" + baseName + " Tinh Khiết";
        } else if (!isAlchemist) {
            result.name = "§f" + baseName + " Pha Loãng";
        } else {
            result.name = "§a" + baseName;
        }

        if (basePotion == null && isAlchemist && finalToxicity == 0 && harshPoints > 0) {
            result.flavorTags.add("§a[Cân Bằng Hoàn Hảo]");
        }

        return result;
    }
}