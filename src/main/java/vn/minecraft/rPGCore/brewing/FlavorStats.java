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

            // 1. Phân tích "Ngũ Vị" từ phần mô tả Effect (Dành cho Thảo Dược)
            // [SỬA LỖI CRASH]: Bọc kiểm tra null. Nếu effect là null thì gán chuỗi rỗng ""
            String effectDesc = data.effect != null ? data.effect.toLowerCase() : "";

            // Mỗi nhánh thảo dược cung cấp 1 điểm vị tương ứng
            if (effectDesc.contains("vị đắng")) bitter += 1.0;
            else if (effectDesc.contains("vị ngọt")) sweet += 1.0;
            else if (effectDesc.contains("vị chua")) sour += 1.0;
            else if (effectDesc.contains("vị cay")) spicy += 1.0;
            else if (effectDesc.contains("vị mặn")) salty += 1.0;
            else if (effectDesc.contains("vô vị")) tasteless += 1.0;

            // 2. [CẬP NHẬT MỚI]: Đọc cứng ID của các loại Nhóm Gia Vị (SPICE)
            // Khắc phục việc gia vị bị thiếu từ khóa "Vị Ngọt/Vị Mặn"
            if (data.category == vn.minecraft.rPGCore.managers.ItemManager.Category.SPICE) {
                switch (id) {
                    case "spice_sugar":
                    case "spice_honey":
                        // Đường và Mật ong là chuyên gia điều vị -> Cung cấp tận +2.0 Vị Ngọt
                        sweet += 1.0;
                        break;
                    case "spice_salt":
                        salty += 1.0;
                        break;
                    case "spice_sour":
                        sour += 1.0;
                        break;
                    case "spice_chili":
                    case "spice_pepper":
                        spicy += 1.0;
                        break;
                    case "spice_garlic":
                        spicy += 0.5; // Tỏi ít cay hơn
                        break;
                    case "spice_all_in_one":
                        // Gia vị tổng hợp buff đều các chỉ số (Giống lore của bạn)
                        sweet += 0.5;
                        salty += 0.5;
                        sour += 0.5;
                        spicy += 0.5;
                        bitter += 0.5;
                        break;
                }
            }
        }
    }

    // Tính tổng "Vị lạ" (Những vị gắt gây khó uống)
    public double getHarshFlavors() {
        return bitter + sour + spicy;
    }
}