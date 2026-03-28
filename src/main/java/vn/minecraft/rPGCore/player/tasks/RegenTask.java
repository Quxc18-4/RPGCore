package vn.minecraft.rPGCore.player.tasks;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.listeners.FoodListener;
import vn.minecraft.rPGCore.managers.FoodBuffManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;

import java.util.Map;
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

            rpgPlayer.updateMaxStats();
            rpgPlayer.updateMoveSpeed();

            boolean hasFullnessBuff = FoodListener.fullnessTimers.containsKey(uuid) &&
                    FoodListener.fullnessTimers.get(uuid) > System.currentTimeMillis();

            int foodLevel = player.getFoodLevel();
            int playerLevel = player.getLevel();
            boolean inCombat = rpgPlayer.isInCombat();

            double maxHp = rpgPlayer.getTotalMaxHealth();
            double maxRes = rpgPlayer.getTotalMaxResource();
            double currentRes = rpgPlayer.getCurrentResource();

            // ==========================================
            // 1. CƠ CHẾ TỤT THANH ĐÓI (ĐÃ NERF MẠNH)
            // ==========================================
            if (!hasFullnessBuff) {
                if (foodLevel > 10) {
                    player.setExhaustion(player.getExhaustion() + 0.05f); // Giảm từ 0.2 xuống 0.05
                }

                boolean isHealingNeeded = (player.getHealth() < maxHp) || (currentRes < maxRes);
                if (isHealingNeeded && foodLevel > 0) {
                    player.setExhaustion(player.getExhaustion() + 0.2f); // Giảm từ 1.4 xuống 0.2 (Rất chậm)
                }
            }

            if (foodLevel == 0) {
                player.damage(maxHp * 0.05);
            }

            // ==========================================
            // 3. CÔNG THỨC HỒI MÁU & MANA CƠ BẢN
            // ==========================================
            double hpRegen = 0.0;
            double resourceRegen = 0.0;
            double baseHpRegen = 20.0 + (2.0 * playerLevel);

            if (foodLevel >= 18) {
                hpRegen = baseHpRegen + (maxHp * (inCombat ? 0.005 : 0.015));
                resourceRegen = maxRes * 0.08;
            }
            else if (foodLevel >= 12) {
                hpRegen = baseHpRegen;
                resourceRegen = maxRes * 0.05;
            }
            else if (foodLevel >= 4) {
                hpRegen = baseHpRegen * 0.5;
                resourceRegen = maxRes * 0.02;
            }

            // ==========================================
            // 4. [MỚI]: KÍCH HOẠT CHỈ SỐ HỒI PHỤC TỪ TRANG BỊ & NỘI TẠI
            // ==========================================
            Map<String, Double> stats = rpgPlayer.getTotalStatsMap();
            double bonusHpRegenPct = stats.getOrDefault("Tỉ lệ hồi phục", 0.0);
            double flatManaRegen = stats.getOrDefault("Hồi Năng lượng", 0.0);

            // Khuếch đại máu và cộng thẳng Năng lượng
            hpRegen *= (1.0 + (bonusHpRegenPct / 100.0));
            resourceRegen += flatManaRegen;

            // ==========================================
            // 5. HÌNH PHẠT GIAO TRANH
            // ==========================================
            if (inCombat) {
                resourceRegen *= 0.5;
            }

            // ==========================================
            // 6. FOOD BUFF
            // ==========================================
            double foodRegenBonus = RPGCore.getInstance().getFoodBuffManager().getBonus(uuid, FoodBuffManager.StatType.REGEN);
            if (foodRegenBonus != 0) {
                double multiplier = 1.0 + (foodRegenBonus / 100.0);
                hpRegen *= multiplier;
                resourceRegen *= multiplier;
            }

            // ==========================================
            // 6.5. CƠ CHẾ ĐỘC QUYỀN CỦA NỘ KHÍ (RAGE)
            // ==========================================
            if (rpgPlayer.getCurrentClass() != null && rpgPlayer.getCurrentClass().getResourceType() == vn.minecraft.rPGCore.classes.ResourceType.RAGE) {
                // Khóa van: Không cho tự hồi Nộ từ đồ ăn hay dòng chỉ số "Hồi Năng lượng"
                resourceRegen = 0.0;

                // [YÊU CẦU MỚI]: Hồi 1 nộ/giây TRONG combat, không bị tụt ngoài combat
                if (inCombat && currentRes < maxRes) {
                    resourceRegen = 1.0;
                }
            }

            // ==========================================
            // 7. CẬP NHẬT UI
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