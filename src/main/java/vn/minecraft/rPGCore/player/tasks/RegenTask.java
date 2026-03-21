package vn.minecraft.rPGCore.player.tasks;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.listeners.FoodListener;
import vn.minecraft.rPGCore.managers.FoodBuffManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;

import java.util.UUID;

public class RegenTask extends BukkitRunnable {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;

    public RegenTask(PlayerManager playerManager, BossBarManager bossBarManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
    }

    @Override
    public void run() {
        for (UUID uuid : playerManager.getAllPlayerUUIDs()) {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
            if (rpgPlayer == null) continue;

            Player player = rpgPlayer.getBukkitPlayer();
            if (player == null || !player.isOnline() || player.isDead()) continue;

            // ==========================================
            // [LIÊN KẾT FOOD BUFF]: REFRESH MÁU & TỐC CHẠY
            // ==========================================
            // Gọi 2 hàm này mỗi giây để đảm bảo nếu Buff Thức Ăn hết hạn, Máu và Tốc chạy sẽ tự động trả về bình thường
            rpgPlayer.updateMaxStats();
            rpgPlayer.updateMoveSpeed();

            // Kiểm tra xem người chơi có đang sở hữu Buff "No Căng" từ đồ ăn không
            boolean hasFullnessBuff = FoodListener.fullnessTimers.containsKey(uuid) &&
                    FoodListener.fullnessTimers.get(uuid) > System.currentTimeMillis();

            int foodLevel = player.getFoodLevel(); // 0 - 20
            int playerLevel = player.getLevel();
            boolean inCombat = rpgPlayer.isInCombat();

            // Sử dụng getTotalMaxHealth để tính cả Level + Class + Đồ ăn
            double maxHp = rpgPlayer.getTotalMaxHealth();
            double maxRes = rpgPlayer.getTotalMaxResource();
            double currentRes = rpgPlayer.getCurrentResource();

            // ==========================================
            // 1. CƠ CHẾ TỤT THANH ĐÓI (EXHAUSTION)
            // ==========================================
            // [SỬA LỖI]: Chỉ tụt đói nếu KHÔNG có buff "No Căng"
            if (!hasFullnessBuff) {
                // Đứng im vẫn tụt nếu no trên mức 10
                if (foodLevel > 10) {
                    player.setExhaustion(player.getExhaustion() + 0.2f);
                }

                // Tiêu hao năng lượng khi cơ thể đang chữa lành
                boolean isHealingNeeded = (player.getHealth() < maxHp) || (currentRes < maxRes);
                if (isHealingNeeded && foodLevel > 0) {
                    player.setExhaustion(player.getExhaustion() + 1.4f);
                }
            }

            // ==========================================
            // 2. CƠ CHẾ SÁT THƯƠNG DO ĐÓI (STARVATION)
            // ==========================================
            if (foodLevel == 0) {
                // Scale sát thương chết đói: Mỗi giây mất 5% máu tối đa
                double starveDamage = maxHp * 0.05;
                player.damage(starveDamage);
            }

            // ==========================================
            // 3. CÔNG THỨC HỒI MÁU & MANA/STAMINA (ĐÃ NERF)
            // ==========================================
            double hpRegen = 0.0;
            double resourceRegen = 0.0;

            // Nerf cơ bản: Giảm chỉ số cộng thẳng
            double baseHpRegen = 20.0 + (2.0 * playerLevel);

            if (foodLevel >= 18) { // 9 - 10 đùi gà
                // Nerf: Giảm % hồi máu (Từ 3% xuống 1.5% ngoài combat, 1% xuống 0.5% trong combat)
                double hpPercent = inCombat ? 0.005 : 0.015;
                hpRegen = baseHpRegen + (maxHp * hpPercent);
                // Nerf: Giảm tài nguyên hồi (Từ 12% xuống 8%)
                resourceRegen = maxRes * 0.08;
            }
            else if (foodLevel >= 12) { // 6 - <9 đùi gà
                hpRegen = baseHpRegen;
                resourceRegen = maxRes * 0.05;
            }
            else if (foodLevel >= 4) { // 2 - <6 đùi gà
                hpRegen = baseHpRegen * 0.5;
                resourceRegen = maxRes * 0.02;
            }
            // foodLevel < 4: Không hồi phục gì cả

            // ==========================================
            // 4. HÌNH PHẠT GIAO TRANH (COMBAT PENALTY)
            // ==========================================
            if (inCombat) {
                // Giảm 50% hiệu quả hồi phục Mana/Stamina khi đang chiến đấu
                resourceRegen *= 0.5;
            }

            // ==========================================
            // 5. [LIÊN KẾT FOOD BUFF]: TỈ LỆ HỒI PHỤC (REGEN)
            // ==========================================
            double foodRegenBonus = RPGCore.getInstance().getFoodBuffManager().getBonus(uuid, FoodBuffManager.StatType.REGEN);
            if (foodRegenBonus != 0) {
                // Ví dụ: Salad Rau Củ buff +25% Tỉ lệ Hồi Phục -> multiplier = 1.25
                double multiplier = 1.0 + (foodRegenBonus / 100.0);
                hpRegen *= multiplier;
                resourceRegen *= multiplier;
            }

            // ==========================================
            // 6. ÁP DỤNG HỒI PHỤC VÀ CẬP NHẬT UI
            // ==========================================
            if (resourceRegen > 0 && currentRes < maxRes) {
                rpgPlayer.setCurrentResource(currentRes + resourceRegen);
            }

            if (hpRegen > 0 && player.getHealth() < maxHp) {
                rpgPlayer.heal(hpRegen);
            }

            bossBarManager.updateBars(player, rpgPlayer);
        }
    }
}