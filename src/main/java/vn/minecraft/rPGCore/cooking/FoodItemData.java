package vn.minecraft.rPGCore.cooking;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class FoodItemData {
    public String name = "§8Món Ăn Kì Lạ";
    public Material material = Material.MUSHROOM_STEW;
    public int freshnessMinutes = 30;
    public int durationMinutes = 0;
    public int boredomPoints = 0; // Độ Ngấy (0 - 100)

    // Hệ thống Stats nội tại (RPG)
    public double bonusDmg = 0;
    public double cdr = 0;
    public double moveSpeed = 0;
    public double atkSpeed = 0;
    public double armorPen = 0;
    public double regenRate = 0;
    public double maxHpBonus = 0;

    // 3 CHỈ SỐ MỚI TỪ NHÓM MA THUẬT
    public double lifeSteal = 0;
    public double critRate = 0;
    public double dropRate = 0;

    // Lưu trữ Hiệu ứng Vanilla (Format: TÊN_HIỆU_ỨNG:CẤP_ĐỘ)
    public List<String> potionEffects = new ArrayList<>();

    // Các Cờ (Flags) trạng thái
    public boolean isRuined = false;
    public boolean hasFatDebuff = false;
    public boolean isMagicReversed = false;

    public List<String> flavorTags = new ArrayList<>();
}