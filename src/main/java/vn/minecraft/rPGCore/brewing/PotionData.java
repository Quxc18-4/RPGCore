package vn.minecraft.rPGCore.brewing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotionData {
    public String potionUuid;           // ID duy nhất để lưu file YML
    public String name = "§8Dược Phẩm Kì Lạ";
    public int durationMinutes = 10;
    public double toxicityPoints = 0;
    public List<String> potionEffects = new ArrayList<>();

    // Hệ thống Stats nội tại (RPG)
    public Map<String, Double> customStats = new HashMap<>();
    // Các stats đặc biệt: "Bất tử", "Thanh tẩy", "Hồi sinh"...
    public List<String> specialEffects = new ArrayList<>();

    public boolean isRuined = false;
    public boolean isManaPotion = false;

    // [MỚI]: Cơ chế cường hóa và Màu sắc RGB
    public boolean isEnhanced = false;
    public int colorR = 50, colorG = 50, colorB = 200; // Mặc định xanh dương

    public List<String> flavorTags = new ArrayList<>();

    public PotionData(String uuid) {
        this.potionUuid = uuid;
    }
}