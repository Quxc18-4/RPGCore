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
        // 1. CHỈ SỐ GỐC THEO CẤP
        // ==========================================
        double baseHp = 50 + (level * 20) + (Math.pow(level, 3.0) * 0.1);
        double baseDmg = 60 + (level * 15) + (Math.pow(level, 2.2) * 0.1);
        double baseArmor = (level - 1) * 1.35 + (Math.pow(level - 1, 2.2) * 0.1);
        double baseMr = (level - 1) * 1.5 + (Math.pow(level - 1, 2.2) * 0.1);

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
        double mobsToKill = 50.0 + (level * 20.0) + (Math.pow(level, 2) * 1.5);
        double baseMobExp = reqExpForThisLevel / mobsToKill;

        // ==========================================
        // [ĐÃ SỬA]: HỆ THỐNG ĐIỂM NGUY HIỂM TOÀN DIỆN (THREAT SCORE)
        // Đánh giá dựa trên cả Máu, Sát Thương, Giáp và Kháng Phép (Trọng số 40-40-20)
        // ==========================================
        double hpScore = type.hp * stage.hp * role.hp * (1.0 + bonusHp);
        double dmgScore = type.dmg * stage.dmg * role.dmg * (1.0 + bonusDmg);
        double defScore = ((type.armor * stage.armor * role.armor * (1.0 + bonusArmor)) +
                (type.mr * stage.mr * role.mr * (1.0 + bonusMr))) / 2.0;

        // Cân bằng tỷ trọng: Sát thương và Máu quyết định 80% độ khó, Phòng thủ quyết định 20%
        double threatScore = (hpScore * 0.4) + (dmgScore * 0.4) + (defScore * 0.2);

        // ==========================================
        // [ĐÃ SỬA]: HỆ SỐ EXP THEO CẤP BẬC (RANK) BÁC YÊU CẦU
        // ==========================================
        double rankExpBonus = 1.0;
        switch (rank) {
            case ELITE:
                rankExpBonus = 3.0;
                break;
            case MINIBOSS:
                rankExpBonus = 5.0;
                break;
            case REGION_BOSS:
                rankExpBonus = 20.0;
                break;
            case WORLD_BOSS:
                rankExpBonus = 100.0; // Boss thế giới auto x100 EXP
                break;
            default:
                rankExpBonus = 1.0;
                break;
        }

        // Tích hợp toàn bộ: Base EXP * Độ nguy hiểm của Vị trí * Thưởng Cấp bậc
        double finalMobExp = baseMobExp * threatScore * rankExpBonus;

        mob.setMetadata("RPG_MOB_EXP", new FixedMetadataValue(RPGCore.getInstance(), finalMobExp));

        // ==========================================
        // 7. [MỚI]: HỆ THỐNG TỰ ĐỘNG PHÂN LOẠI SÁT THƯƠNG (PHYSICAL vs MAGICAL)
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