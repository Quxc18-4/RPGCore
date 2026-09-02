package vn.minecraft.rPGCore.MobScalingSystem.enums;

public enum MobRole {
    NONE(1.0, 1.0, 1.0, 1.0),
    TANKER(1.5, 0.6, 1.5, 1.2),

    // [ĐÃ SỬA]: Fighter máu trâu, giáp dày, nhưng Kháng phép (MR) bị neft mạnh (0.7)
    FIGHTER(1.2, 1.1, 1.1, 0.7),

    MAGE(0.7, 1.5, 0.5, 2.0),
    ASSASSIN(0.8, 2.0, 0.3, 0.3),
    MARKSMAN(0.7, 1.8, 0.4, 0.4), // Xạ thủ (Bắn cung, ném xa)

    // [MỚI]: Các hệ chuyên biệt bác yêu cầu
    SCOUT(0.8, 0.8, 0.6, 0.6),    // Trinh sát: Máu giấy, DMG trung bình, di chuyển lanh lẹ
    STEALER(0.85, 0.9, 0.5, 0.5), // Kẻ trộm: Mỏng manh, đánh lén
    SUPPORTER(1.1, 0.5, 1.0, 1.5),// Hỗ trợ: DMG bé tí, Kháng phép cao để sinh tồn buff máu
    BUFFER(1.2, 0.4, 1.2, 1.2),   // Buff: Trâu bò để đứng vững buff team, DMG phế
    DEBUFF(0.9, 0.7, 0.8, 1.8),   // Debuff: Kháng phép cao, máu trung bình

    // Tiên phong (Vanguard): Giáp vật lý SIÊU DÀY (1.8), xông pha tuyến đầu, nhưng bị Phép thuật nướng chín (0.4)
    VANGUARD(1.4, 0.9, 1.8, 0.4);

    public final double hp, dmg, armor, mr;
    MobRole(double hp, double dmg, double armor, double mr) {
        this.hp = hp; this.dmg = dmg; this.armor = armor; this.mr = mr;
    }
}