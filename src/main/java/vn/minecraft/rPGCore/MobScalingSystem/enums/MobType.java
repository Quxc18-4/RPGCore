package vn.minecraft.rPGCore.MobScalingSystem.enums;

public enum MobType {
    HUMANOID(1.0, 1.0, 1.0, 1.0),
    WEAK_HUMANOID(0.8, 0.8, 0.8, 0.8),
    BEAST(1.1, 1.1, 0.9, 0.8),
    UNDEAD(1.2, 0.9, 1.0, 0.5), // Xác sống sợ sát thương phép (Lửa/Ánh sáng)
    INSECT(0.8, 1.2, 0.5, 1.0),

    // [MỚI BỔ SUNG]
    ARTHROPOD(0.8, 1.1, 1.2, 0.8), // Chân đốt (Nhện, Bọ cạp): Giáp cứng
    SLIME(1.25, 0.7, 0.5, 1.5),     // Slime: Siêu nhiều máu, kháng phép tốt, thủ vật lý yếu
    ELEMENTAL(0.8, 1.4, 0.5, 2.0), // Sinh vật Nguyên tố

    DEMON(1.2, 1.3, 1.0, 1.5),
    SPIRIT(0.5, 1.0, 0.2, 2.5),
    GIANT(2.5, 1.5, 1.2, 0.6),
    LIVESTOCK(0.5, 0.2, 0.5, 0.5);

    public final double hp, dmg, armor, mr;
    MobType(double hp, double dmg, double armor, double mr) {
        this.hp = hp; this.dmg = dmg; this.armor = armor; this.mr = mr;
    }
}