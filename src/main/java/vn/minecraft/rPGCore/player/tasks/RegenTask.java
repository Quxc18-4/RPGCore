package vn.minecraft.rPGCore.player.tasks;

import org.bukkit.scheduler.BukkitRunnable;
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
        // Quét toàn bộ người chơi đang online trong hệ thống
        for (UUID uuid : playerManager.getAllPlayerUUIDs()) {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
            if (rpgPlayer == null) continue;

            double currentRes = rpgPlayer.getCurrentResource();
            double maxRes = rpgPlayer.getCurrentClass().getMaxResource();
            double regenRate = rpgPlayer.getCurrentClass().getRegenRate();

            // Nếu chưa đầy thì mới hồi phục
            if (currentRes < maxRes) {
                // Cộng thêm lượng hồi phục (không vượt quá max)
                rpgPlayer.setCurrentResource(Math.min(currentRes + regenRate, maxRes));

                // Cập nhật lên màn hình (BossBar)
                bossBarManager.updateBars(rpgPlayer.getBukkitPlayer(), rpgPlayer);
            }
        }
    }
}