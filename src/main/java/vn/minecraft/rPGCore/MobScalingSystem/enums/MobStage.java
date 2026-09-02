package vn.minecraft.rPGCore.MobScalingSystem.enums;

public enum MobStage {
    // 3 Cấp độ đầu (Làm lại cho mượt)
    STAGE_1(1.0, 1.0, 1.0, 1.0),       // Côn trùng, lính lác cơ bản
    STAGE_2(1.3, 1.2, 1.1, 1.1),       // Tiến hóa nhẹ
    STAGE_3(1.7, 1.5, 1.2, 1.2),       // Trưởng thành

    // Mở rộng giai đoạn Giữa (Mid-tier)
    STAGE_4(2.3, 1.9, 1.4, 1.4),       // Biến dị (Mutated)
    STAGE_5(3.1, 2.4, 1.6, 1.6),       // Tinh anh (Elite Form)
    STAGE_6(4.2, 3.0, 1.8, 1.8),       // Tướng lĩnh (Commander)

    // Mở rộng giai đoạn Cuối (High-tier & God-tier)
    STAGE_7(5.8, 3.8, 2.1, 2.1),       // Lãnh chúa (Lord)
    STAGE_8(8.0, 4.8, 2.4, 2.4),       // Vua / Ma vương (King/Demon King)
    STAGE_9(11.5, 6.2, 2.8, 2.8),      // Thảm họa (Disaster/Catastrophe)
    STAGE_10(16.0, 8.0, 3.5, 3.5);     // Thần thoại (Mythic/God)

    public final double hp, dmg, armor, mr;
    MobStage(double hp, double dmg, double armor, double mr) {
        this.hp = hp; this.dmg = dmg; this.armor = armor; this.mr = mr;
    }
}