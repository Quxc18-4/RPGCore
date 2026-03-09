package vn.minecraft.rPGCore.cooking;

import vn.minecraft.rPGCore.managers.IngredientManager.IngredientData;
import java.util.List;

public class NutritionStats {
    public double meat = 0, fish = 0, veg = 0, starch = 0;
    public double fat = 0, monster = 0, magic = 0;
    public double salt = 0, sugar = 0, chili = 0, garlic = 0, pepper = 0, sour = 0;

    public NutritionStats(List<IngredientData> ingredients) {
        for (IngredientData data : ingredients) {
            String id = data.id.toLowerCase();
            switch (data.category) {
                case MEAT:
                    if (id.contains("fish") || id.contains("seafood")) fish += data.points;
                    else meat += data.points;
                    break;
                case VEG: veg += data.points; break;
                case STARCH: starch += data.points; break;
                case FAT: fat += data.points; break;
                case MONSTER: monster += data.points; break;
                case MAGIC: magic += data.points; break;
                case SPICE:
                    if (id.contains("salt")) salt += data.points;
                    else if (id.contains("sugar")) sugar += data.points;
                    else if (id.contains("chili")) chili += data.points;
                    else if (id.contains("garlic")) garlic += data.points;
                    else if (id.contains("pepper")) pepper += data.points;
                    else if (id.contains("sour") || id.contains("lemon") || id.contains("vinegar")) sour += data.points;
                    break;
                default: break;
            }
        }
    }

    public double getCorePoints() { return meat + fish + veg + starch; }
    public double getTotalSpice() { return salt + sugar + chili + garlic + pepper + sour; }
}