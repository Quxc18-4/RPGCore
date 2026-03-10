package vn.minecraft.rPGCore.cooking;

import vn.minecraft.rPGCore.managers.IngredientManager.IngredientData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NutritionStats {
    public double meat = 0, fish = 0, veg = 0, starch = 0;
    public double fat = 0, monster = 0, magic = 0;
    public double salt = 0, sugar = 0, chili = 0, garlic = 0, pepper = 0, sour = 0;

    // BỘ ĐẾM RIÊNG BIỆT CHO CÁC VẬT PHẨM ĐẶC THÙ (MAGIC)
    public Map<String, Integer> itemCounts = new HashMap<>();

    public NutritionStats(List<IngredientData> ingredients) {
        for (IngredientData data : ingredients) {
            String id = data.id.toLowerCase();

            // Ghi nhận số lượng từng ID
            itemCounts.put(id, itemCounts.getOrDefault(id, 0) + 1);

            // Xử lý ngoại lệ Khoai tây: Vừa là Rau, vừa là Tinh bột
            if (id.equals("veg_potato")) {
                veg += 0.5;
                starch += 0.5;
                continue; // Bỏ qua switch bên dưới
            }

            switch (data.category) {
                case MEAT: meat += data.points; break;
                case FISH: fish += data.points; break; // Fish tách riêng
                case VEG: veg += data.points; break;
                case STARCH: starch += data.points; break;
                case FAT: fat += data.points; break;
                case MONSTER: monster += data.points; break;
                case MAGIC: magic += data.points; break;
                case SPICE:
                    if (id.contains("salt")) salt += data.points;
                    else if (id.contains("sugar") || id.contains("honey")) sugar += data.points;
                    else if (id.contains("chili")) chili += data.points;
                    else if (id.contains("garlic")) garlic += data.points;
                    else if (id.contains("pepper")) pepper += data.points;
                    else if (id.contains("sour")) sour += data.points;
                    break;
                default: break;
            }
        }
    }

    public double getCorePoints() { return meat + fish + veg + starch; }
    public double getTotalSpice() { return salt + sugar + chili + garlic + pepper + sour; }
}