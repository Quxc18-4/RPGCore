package vn.minecraft.rPGCore.brewing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotionData {
    public String potionUuid;           // ID duy nhất để lưu file YML
    public String name = "§8Dược Phẩm Kì Lạ";
    public int durationMinutes = 10;     // Thời gian cơ bản là 3 phút
    public double toxicityPoints = 0;   // Điểm Độc tính (Sốc thuốc) khi uống
    // Thêm dòng này vào dưới List<String> specialEffects
    public List<String> potionEffects = new ArrayList<>();

    // Hệ thống Stats nội tại (RPG)
    public Map<String, Double> customStats = new HashMap<>();
    // Các stats đặc biệt: "Bất tử", "Thanh tẩy", "Hồi sinh"...
    public List<String> specialEffects = new ArrayList<>();

    // Các Cờ (Flags) trạng thái
    public boolean isRuined = false;       // Nấu hỏng (Gây Nausea/Poison)
    public boolean isManaPotion = false;   // Đánh dấu để check Trúng Độc Mana

    // Nhãn Hương Vị (Để hiện Lore)
    public List<String> flavorTags = new ArrayList<>();

    public PotionData(String uuid) {
        this.potionUuid = uuid;
    }
}