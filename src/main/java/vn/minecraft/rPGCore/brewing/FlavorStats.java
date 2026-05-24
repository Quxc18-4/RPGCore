package vn.minecraft.rPGCore.brewing;

import vn.minecraft.rPGCore.managers.ItemManager.ItemData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlavorStats {
    public double bitter = 0;   // Đắng (Sát thương, Hút máu...)
    public double sweet = 0;    // Ngọt (Hồi phục, Điều vị khử độc)
    public double sour = 0;     // Chua (Tốc độ di chuyển, Debuff)
    public double spicy = 0;    // Cay (Bạo kích, Xuyên giáp, Lửa)
    public double salty = 0;    // Mặn (Phòng thủ, Sinh tồn)
    public double tasteless = 0;// Vô vị (Dung môi, Chất kết dính)

    public Map<String, Integer> itemCounts = new HashMap<>();

    public FlavorStats(List<ItemData> ingredients) {
        for (ItemData data : ingredients) {
            String id = data.id.toLowerCase();
            itemCounts.put(id, itemCounts.getOrDefault(id, 0) + 1);

            // Phân tích "Ngũ Vị" từ phần mô tả Effect (Ví dụ: "Vị Đắng | Pha chế: Hồi máu...")
            String effectDesc = data.effect.toLowerCase();

            // Mỗi nhánh thảo dược cung cấp 1 điểm vị tương ứng
            if (effectDesc.contains("vị đắng")) bitter += 1.0;
            else if (effectDesc.contains("vị ngọt")) sweet += 1.0;
            else if (effectDesc.contains("vị chua")) sour += 1.0;
            else if (effectDesc.contains("vị cay")) spicy += 1.0;
            else if (effectDesc.contains("vị mặn")) salty += 1.0;
            else if (effectDesc.contains("vô vị")) tasteless += 1.0;
        }
    }

    // Tính tổng "Vị lạ" (Những vị gắt gây khó uống)
    public double getHarshFlavors() {
        return bitter + sour + spicy;
    }
}