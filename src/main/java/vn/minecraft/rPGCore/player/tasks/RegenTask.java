package vn.minecraft.rPGCore.player.tasks;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.listeners.FoodListener;
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

            // Kiểm tra xem người chơi có đang sở hữu Buff "No Căng" từ đồ ăn không
            boolean hasFullnessBuff = FoodListener.fullnessTimers.containsKey(uuid) &&
                    FoodListener.fullnessTimers.get(uuid) > System.currentTimeMillis();

            int foodLevel = player.getFoodLevel(); // 0 - 20
            int playerLevel = player.getLevel();
            boolean inCombat = rpgPlayer.isInCombat();

            // Sử dụng getTotalMaxHealth để tính cả Level + Class
            double maxHp = rpgPlayer.getTotalMaxHealth();
            double maxRes = rpgPlayer.getTotalMaxResource();
            double currentRes = rpgPlayer.getCurrentResource();

            // ==========================================
            // 1. CƠ CHẾ TỤT THANH ĐÓI (EXHAUSTION)
            // ==========================================
            // Đứng im vẫn tụt nếu no trên mức 10
            if (foodLevel > 10) {
                player.setExhaustion(player.getExhaustion() + 0.2f);
            }

            // Tiêu hao năng lượng khi cơ thể đang chữa lành
            boolean isHealingNeeded = (player.getHealth() < maxHp) || (currentRes < maxRes);
            if (isHealingNeeded && foodLevel > 0) {
                player.setExhaustion(player.getExhaustion() + 1.4f);
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
            // 3. CÔNG THỨC HỒI MÁU & MANA/STAMINA
            // ==========================================
            double hpRegen = 0.0;
            double resourceRegen = 0.0;
            double baseHpRegen = 50.0 + (5.0 * playerLevel);

            if (foodLevel >= 18) { // 9 - 10 đùi gà
                double hpPercent = inCombat ? 0.01 : 0.03;
                hpRegen = baseHpRegen + (maxHp * hpPercent);
                resourceRegen = maxRes * 0.12;
            }
            else if (foodLevel >= 12) { // 6 - <9 đùi gà
                hpRegen = baseHpRegen;
                resourceRegen = maxRes * 0.08;
            }
            else if (foodLevel >= 4) { // 2 - <6 đùi gà
                hpRegen = baseHpRegen * 0.7;
                resourceRegen = maxRes * 0.05;
            }
            // foodLevel < 4: Không hồi phục gì cả

            // ==========================================
            // 4. HÌNH PHẠT GIAO TRANH (COMBAT PENALTY)
            // ==========================================
            if (inCombat) {
                // Giảm 50% hiệu quả hồi phục Mana/Stamina khi đang chiến đấu
                resourceRegen *= 0.5;
                // Máu đã được tính tier % riêng ở trên nên không cần giảm thêm 50% ở đây
            }

            // ==========================================
            // 5. ÁP DỤNG HỒI PHỤC VÀ CẬP NHẬT UI
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