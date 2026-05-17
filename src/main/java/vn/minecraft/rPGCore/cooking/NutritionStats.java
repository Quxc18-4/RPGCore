package vn.minecraft.rPGCore.cooking;

import vn.minecraft.rPGCore.managers.ItemManager.ItemData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NutritionStats {
    public double meat = 0, fish = 0, veg = 0, starch = 0;
    public double fat = 0, monster = 0, magic = 0;
    public double salt = 0, sugar = 0, chili = 0, garlic = 0, pepper = 0, sour = 0;

    public Map<String, Integer> itemCounts = new HashMap<>();

    public NutritionStats(List<ItemData> ingredients) {
        for (ItemData data : ingredients) {
            String id = data.id.toLowerCase();

            itemCounts.put(id, itemCounts.getOrDefault(id, 0) + 1);

            if (id.equals("veg_potato")) {
                veg += 0.5;
                starch += 0.5;
                continue;
            }

            if (id.equals("meat_wolf_bone")) {
                meat += data.points;
                sugar += 0.25;
                continue;
            }

            // [MỚI]: Gia vị tổng hợp (All in one)
            if (id.equals("spice_all_in_one")) {
                salt += 0.5; sugar += 0.5; chili += 0.5; garlic += 0.5; pepper += 0.5; sour += 0.5;
                continue;
            }

            switch (data.category) {
                case MEAT: meat += data.points; break;
                case FISH: fish += data.points; break;
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