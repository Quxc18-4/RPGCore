package vn.minecraft.rPGCore.MobScalingSystem.managers;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.MobScalingSystem.enums.*;

public class MobStatManager {

    // [ĐÃ SỬA]: Thêm 4 tham số Bonus Modifier ở cuối cùng
    public static void injectDynamicStats(LivingEntity mob, int level, MobType type, MobStage stage, MobRole role, MobRank rank,
                                          double bonusHp, double bonusDmg, double bonusArmor, double bonusMr) {

// ==========================================
        // 1. CHỈ SỐ GỐC THEO CẤP (ĐÃ CÂN BẰNG LẠI)
        // ==========================================
        // Máu (HP): Đổi từ hàm bậc 3 (^3.0) xuống bậc 2 (^2.0) để Máu không bị lạm phát ở late game
        double baseHp = 50 + (level * 20) + (Math.pow(level, 2.0) * 1.5);

        // Sát thương (DMG): Làm mượt lại một chút để quái không One-hit người chơi
        double baseDmg = 20 + (level * 12) + (Math.pow(level, 1.8) * 0.5);

        // Giáp & Kháng Phép: Đổi từ (^2.2) xuống (^1.5) để vũ khí của Player không bị "lỗi thời" quá nhanh
        double baseArmor = (level - 1) * 2.0 + (Math.pow(level - 1, 1.2) * 0.8);
        double baseMr = (level - 1) * 1.5 + (Math.pow(level - 1, 1.2) * 0.8);

        // ==========================================
        // 2. CHỒNG LỚP 4 TRỤ CỘT ENUM
        // ==========================================
        double finalHp = baseHp * type.hp * stage.hp * role.hp * rank.hp;
        double finalDmg = baseDmg * type.dmg * stage.dmg * role.dmg * rank.dmg;
        double finalArmor = baseArmor * type.armor * stage.armor * role.armor;
        double finalMr = baseMr * type.mr * stage.mr * role.mr;

        // ==========================================
        // 3. ÉP CHỈ SỐ BONUS ĐẶC BIỆT TỪ CONFIG
        // Ví dụ: bonusHp = 0.1 -> Tăng 10%. bonusHp = -0.1 -> Giảm 10%.
        // ==========================================
        finalHp *= (1.0 + bonusHp);
        finalDmg *= (1.0 + bonusDmg);
        finalArmor *= (1.0 + bonusArmor);
        finalMr *= (1.0 + bonusMr);

        // ==========================================
        // 4. BƠM VÀO THỰC THỂ (Vanilla)
        // ==========================================
        AttributeInstance hpAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(Math.max(1.0, finalHp));
            mob.setHealth(Math.max(1.0, finalHp));
        }

        AttributeInstance dmgAttr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.setBaseValue(Math.max(0.0, finalDmg));
        }

        AttributeInstance armorAttr = mob.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(finalArmor);
        }

        // ==========================================
        // 5. LƯU METADATA CHO CÁC HỆ THỐNG KHÁC ĐỌC
        // ==========================================
        mob.setMetadata("RPG_Magic_Resist", new FixedMetadataValue(RPGCore.getInstance(), finalMr));
        mob.setMetadata("RPG_MOB_DAMAGE", new FixedMetadataValue(RPGCore.getInstance(), finalDmg));
        mob.setMetadata("MOB_TYPE", new FixedMetadataValue(RPGCore.getInstance(), type.name()));
        mob.setMetadata("MOB_LEVEL", new FixedMetadataValue(RPGCore.getInstance(), level));

        if (rank == MobRank.MINIBOSS || rank == MobRank.REGION_BOSS || rank == MobRank.WORLD_BOSS) {
            mob.setMetadata("isBoss", new FixedMetadataValue(RPGCore.getInstance(), true));
        }

        // ==========================================
        // 6. [BỔ SUNG]: TÍNH TOÁN EXP CÀY CUỐC ĐỈNH CAO
        // ==========================================
        double expBaseCurve = 500 + (level * 200) + (Math.pow(level, 3.0) * 1.5);
        double stageMultiplier = 1.0;

        if (level >= 16 && level < 22) stageMultiplier = 1.5;
        else if (level >= 22 && level < 32) stageMultiplier = 3.0;
        else if (level >= 32 && level < 50) stageMultiplier = 6.0;
        else if (level >= 50 && level < 75) stageMultiplier = 15.0;
        else if (level >= 75 && level < 101) stageMultiplier = 30.0;
        else if (level >= 101 && level < 150) stageMultiplier = 50.0;
        else if (level >= 150) stageMultiplier = 80.0;

        double reqExpForThisLevel = expBaseCurve * stageMultiplier;

        // ==========================================
        // ĐƯỜNG CONG TƯƠNG QUAN: MỐC GIẾT QUÁI ĐỒNG BỘ VỚI STAGE MULTIPLIER
        // ==========================================
        double mobsToKill;
        if (level < 16) {
            mobsToKill = 25.0 + (level * 8.0);          // Cấp 1 - 15: Khoảng 33 đến 145 con để lên cấp
        } else if (level < 22) {
            mobsToKill = 155.0 + ((level - 16) * 15.0);  // Cấp 16 - 21: Khoảng 155 đến 230 con
        } else if (level < 32) {
            mobsToKill = 250.0 + ((level - 22) * 18.0); // Cấp 22 - 31: Khoảng 250 đến 412 con
        } else if (level < 50) {
            mobsToKill = 450.0 + ((level - 32) * 25.0);  // Cấp 32 - 49: Khoảng 450 đến ~875 con (Cực kỳ vừa vặn mốc 300-400 con ở tầm cấp 40 như bác muốn!)
        } else if (level < 75) {
            mobsToKill = 900.0 + ((level - 50) * 35.0); // Cấp 50 - 74: Khoảng 900 đến ~1740 con
        } else if (level < 101) {
            mobsToKill = 2000.0 + ((level - 75) * 45.0); // Cấp 75 - 100: Khoảng 2000 đến ~3125 con
        } else {
            mobsToKill = 3500.0 + ((level - 101) * 75.0); // Cấp 101 trở lên:  Khoảng 3500 đến ~10925 con
        }

        double baseMobExp = reqExpForThisLevel / mobsToKill;

        // ==========================================
        // [ĐÃ SỬA]: HỆ THỐNG ĐIỂM NGUY HIỂM TOÀN DIỆN (THREAT SCORE)
        // ==========================================
        double hpScore = type.hp * stage.hp * role.hp * (1.0 + bonusHp);
        double dmgScore = type.dmg * stage.dmg * role.dmg * (1.0 + bonusDmg);
        double defScore = ((type.armor * stage.armor * role.armor * (1.0 + bonusArmor)) +
                (type.mr * stage.mr * role.mr * (1.0 + bonusMr))) / 2.0;

        double threatScore = (hpScore * 0.4) + (dmgScore * 0.4) + (defScore * 0.2);

        // ==========================================
        // [ĐÃ SỬA]: HỆ SỐ EXP THEO CẤP BẬC (RANK)
        // ==========================================
        double rankExpBonus = 1.0;
        switch (rank) {
            case ELITE:
                // Tinh anh: Trâu gấp 3-4 lần quái thường, sát thương to hơn.
                rankExpBonus = 15.0;
                break;
            case MINIBOSS:
                // Miniboss: Mất 3-4 phút để giết (gấp 20-30 lần thời gian đánh quái).
                // Cho x45 lần EXP để người chơi thấy "Đáng đồng tiền bát gạo"!
                rankExpBonus = 45.0;
                break;
            case REGION_BOSS:
                // Boss Khu vực: Cần tổ đội 2-3 người đánh, mất 5-10 phút.
                rankExpBonus = 150.0;
                break;
            case WORLD_BOSS:
                // Boss Thế giới: Cả server xúm vào đập.
                rankExpBonus = 500.0;
                break;
            default:
                rankExpBonus = 1.0;
                break;
        }

        // Tích hợp toàn bộ: Base EXP * Độ nguy hiểm của Vị trí * Thưởng Cấp bậc
        double finalMobExp = baseMobExp * threatScore * rankExpBonus;
        mob.setMetadata("RPG_MOB_EXP", new FixedMetadataValue(RPGCore.getInstance(), finalMobExp));

        // ==========================================
        // 7. [MỚI]: HỆ THỐNG TỰ ĐỘNG PHÂN LOẠI SÁT THƯƠNG
        // ==========================================
        boolean dealsMagicDamage = false;

        // Bọn này sinh ra là thực thể ma thuật thuần túy (Type)
        if (type == MobType.ELEMENTAL || type == MobType.SPIRIT) {
            dealsMagicDamage = true;
        }
        // Hoặc bọn này có học phép thuật / đánh xa bằng phép (Role)
        else if (role == MobRole.MAGE || role == MobRole.SUPPORTER || role == MobRole.BUFFER || role == MobRole.DEBUFF) {
            dealsMagicDamage = true;
        }

        // Gắn cờ ma thuật để RPGClass của Player tự động đem Kháng Phép ra đỡ!
        if (dealsMagicDamage) {
            mob.setMetadata("Magic_Attack", new FixedMetadataValue(RPGCore.getInstance(), true));
        }
    }
}