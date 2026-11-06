package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.List;
import java.util.Map;

public class ReaperClass extends RPGClass {

    // Từ khóa Metadata để kết nối với hệ thống Core của Server
    public static final String META_REAPER_FAST_POOL = "REAPER_FAST_POOL";
    public static final String META_REAPER_SHIELD = "REAPER_OVERHEAL_SHIELD";

    public ReaperClass(String id, String displayName, Material icon, List<String> description,
                       double maxHealth, double maxResource, ResourceType resourceType,
                       double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
        // [CẬP NHẬT 1]: Mở khóa Cap Tỉ lệ Hồi phục riêng cho Reaper
        this.hpRegenCap = 600.0;
        // Trong Constructor của ReaperClass
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata("Reaper_Bloodlust")) return 0.05; // Tăng 5% sát thương cuối
                return 0.0;
            }
        });
    }

    // ==========================================
    // 1. CHỈ SỐ TĨNH (PASSIVE STATS) & CỜ HỆ THỐNG
    // Đã hoàn thiện logic: Hồi phục dư -> Máu Tối Đa
    // ==========================================
    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        // [CẬP NHẬT 2]: Khởi đầu 25% Hồi phục thay vì 20%
        stats.put("Tỉ lệ hồi phục", stats.getOrDefault("Tỉ lệ hồi phục", 0.0) + 25.0);

        // Hiển thị % Máu Tối Đa lên UI cho người chơi xem (Đồng bộ với Global)
        double actualHpBonusPercent = getGlobalMaxHpMultiplier(rpgPlayer) * 100.0;
        stats.put("% Máu Tối Đa", stats.getOrDefault("% Máu Tối Đa", 0.0) + actualHpBonusPercent);

        Player player = rpgPlayer.getBukkitPlayer();
        if (player != null && player.isOnline()) {
            if (!player.hasMetadata(META_REAPER_FAST_POOL)) {
                player.setMetadata(META_REAPER_FAST_POOL, new FixedMetadataValue(RPGCore.getInstance(), true));
            }
            if (!player.hasMetadata(META_REAPER_SHIELD)) {
                player.setMetadata(META_REAPER_SHIELD, new FixedMetadataValue(RPGCore.getInstance(), true));
            }
        }
        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // ==========================================
    // [HOOK TƯƠNG LAI] 2. THAO TÁC VŨ KHÍ & ĐÁNH THƯỜNG
    // ==========================================

    // Sửa trong ReaperClass.java
    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        // [SỬA LỖI 1 DMG]: Hứng đòn đánh Kỹ năng ngay từ bước khởi đầu (Before Combat)
        if (attacker.hasMetadata("Reaper_BS_OnHit")) {
            int stageBuff = attacker.getMetadata("Reaper_BS_OnHit").get(0).asInt();
            int skillLevel = rpgAttacker.getSkillData().getSkillLevel("reaper_blood_strike");

            // Xóa Buff lập tức
            attacker.removeMetadata("Reaper_BS_OnHit", vn.minecraft.rPGCore.RPGCore.getInstance());
            attacker.removeMetadata("Active_Weapon_Enhancement", vn.minecraft.rPGCore.RPGCore.getInstance());

            // Chạy sát thương Kỹ năng
            ISkill bsSkill = vn.minecraft.rPGCore.RPGCore.getInstance().getSkillManager().getSkill("reaper_blood_strike");
            if (bsSkill instanceof vn.minecraft.rPGCore.skills.reaper.BloodStrikeSkill) {
                ((vn.minecraft.rPGCore.skills.reaper.BloodStrikeSkill) bsSkill).processOnHit(attacker, rpgAttacker, target, skillLevel, stageBuff);
            }

            // TRẢ VỀ FALSE ĐỂ HỦY BỎ HOÀN TOÀN ĐÒN ĐÁNH TAY VANILLA (CHỐNG LỖI +1 DMG)
            return false;
        }

        // Nếu không có Buff, chém tay bình thường
        return super.performMeleeAttack(attacker, rpgAttacker, target, weapon);
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {
        // Xóa đoạn hứng kỹ năng ở đây, chỉ giữ lại super
        return super.onMeleeHit(attacker, rpgAttacker, target, rawDamage);
    }

    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        super.onAttackLanded(attacker, target);
        // Bắt sự kiện sau khi chém TRÚNG mục tiêu.
        // Ý tưởng tương lai: Đánh trúng sẽ tích 1 'Huyết Ấn' lên quái. Đủ 5 Ấn quái phát nổ hồi lại máu cho Reaper.
    }

    // ==========================================
    // [HOOK TƯƠNG LAI] 3. SỰ KIỆN CHỊU ĐÒN & TƯƠNG TÁC PHÍM
    // ==========================================

    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, LivingEntity attacker, double incomingDamage) {
        // Nơi can thiệp sát thương trước khi bị trừ vào máu.
        // Ý tưởng tương lai: Máu càng thấp (dưới 30%), incomingDamage càng bị giảm bớt (Kháng đòn tử vong).
        return super.onTakeDamage(victim, rpgVictim, attacker, incomingDamage);
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // Bắt sự kiện bấm phím F (Đổi tay cầm).
        // Ý tưởng tương lai: Bấm F bật mode "Huyết Quỷ" (Rút máu liên tục nhưng x2 Sát Thương và đổi Particle Đỏ).
        return super.onSwapItem(player, rpgPlayer, weapon);
    }

    // ==========================================
    // [HOOK TƯƠNG LAI] 4. ĐIỀU CHỈNH CHỈ SỐ ĐỘNG TRONG COMBAT
    // ==========================================

    // Tăng 20% Hút máu khi đang trong 12s Cuồng Huyết
    @Override
    public double getDynamicLifeSteal(Player attacker, RPGPlayer rpgPlayer) {
        double extraLs = super.getDynamicLifeSteal(attacker, rpgPlayer);
        // Kiểm tra xem đồng hồ 12s còn chạy không
        if (attacker.hasMetadata("Reaper_Bloodlust_End")) {
            extraLs += 20.0;
        }
        return extraLs;
    }

    @Override
    public void applyDynamicCombatStats(Player attacker, LivingEntity target, Map<String, Double> combatStats) {
        super.applyDynamicCombatStats(attacker, target, combatStats);
        // Sửa đổi chỉ số linh hoạt lúc tính sát thương.
        // Ý tưởng tương lai: Nếu quái (target) đang bị chảy máu, cộng thêm 20.0 Tỉ lệ Xuyên Giáp cho đòn đánh này.
    }
    // Thêm hàm này vào ReaperClass.java
    @Override
    public double getGlobalMaxHpMultiplier(RPGPlayer rpgPlayer) {
        double vitRegen = rpgPlayer.getAttributeData().getAttribute("VIT") * 0.1;
        double gearRegen = rpgPlayer.getStats() != null ? rpgPlayer.getStats().regenRate : 0.0;

        // [CẬP NHẬT 3]: Base là 25.0
        double rawRegen = vitRegen + gearRegen + 25.0;

        double dynamicBonus = 0.0;
        if (rawRegen > 200.0) {
            // [CẬP NHẬT 4]: Sử dụng đúng hpRegenCap để chặn trần phần dư
            double excessRegen = Math.min(rawRegen, this.hpRegenCap) - 200.0;
            dynamicBonus = Math.floor(excessRegen / 10.0) / 100.0;
            dynamicBonus = Math.min(dynamicBonus, 0.40);
        }
        return 0.10 + dynamicBonus;
    }
    // ==========================================
    // NỘI TẠI 4: DYNAMIC REGEN (Máu càng thấp hồi càng nhanh)
    // ==========================================
    @Override
    public double getDynamicHpRegenRate(Player player, RPGPlayer rpgPlayer) {
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double currentHp = player.getHealth();

        if (maxHp <= 0 || currentHp >= maxHp) return 0.0;

        // Tính % máu đã mất
        double missingHpPercent = ((maxHp - currentHp) / maxHp) * 100.0;

        // Mỗi 1% máu mất = +1% Hồi phục (Tối đa 99%)
        return Math.min(Math.floor(missingHpPercent), 99.0);
    }
}