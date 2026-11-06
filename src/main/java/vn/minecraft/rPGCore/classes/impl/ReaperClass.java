package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

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

        // [LIMIT BREAK]: Mở khóa Cap riêng cho Reaper
        this.resourceRegenCap = 600.0;
    }

    // ==========================================
    // 1. CHỈ SỐ TĨNH (PASSIVE STATS) & CỜ HỆ THỐNG
    // Đã hoàn thiện logic: Hồi phục dư -> Máu Tối Đa
    // ==========================================
    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        double healthRegen = stats.getOrDefault("Tỉ lệ hồi máu", 100.0);

        if (healthRegen > 200.0) {
            double excessRegen = Math.min(healthRegen, this.resourceRegenCap) - 200.0;
            double bonusMaxHpPercent = Math.floor(excessRegen / 10.0) * 1.0;
            bonusMaxHpPercent = Math.min(bonusMaxHpPercent, 40.0);
            stats.put("% Máu Tối Đa", stats.getOrDefault("% Máu Tối Đa", 0.0) + bonusMaxHpPercent);
        }

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

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        // Reaper là phái cận chiến (dùng Lưỡi hái Hoe). Mặc định cho phép chém tay.
        // Có thể chặn chém tay (return false) nếu đang trong trạng thái Vận Chiêu (Channeling).
        return super.performMeleeAttack(attacker, rpgAttacker, target, weapon);
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {
        // Nơi xử lý Đòn đánh tay đặc biệt (Giống Mảnh Sấm Sét của Warrior).
        // Ý tưởng tương lai: Đánh tay tự trừ 2% Máu để nổ thêm Sát thương Chuẩn dựa trên % Máu đã mất.
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

    @Override
    public double getDynamicLifeSteal(Player attacker, RPGPlayer rpgPlayer) {
        // Chỉ số Hút máu cộng thêm ngay khoảnh khắc chém trúng (Thay đổi liên tục).
        // Ý tưởng tương lai: Trả về lượng hút máu tương ứng với % máu đã mất của Reaper.
        // Ví dụ: Mất 50% máu -> Trả về 20.0 (cộng thêm 20% lifesteal).
        return super.getDynamicLifeSteal(attacker, rpgPlayer);
    }

    @Override
    public void applyDynamicCombatStats(Player attacker, LivingEntity target, Map<String, Double> combatStats) {
        super.applyDynamicCombatStats(attacker, target, combatStats);
        // Sửa đổi chỉ số linh hoạt lúc tính sát thương.
        // Ý tưởng tương lai: Nếu quái (target) đang bị chảy máu, cộng thêm 20.0 Tỉ lệ Xuyên Giáp cho đòn đánh này.
    }
}