package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class JobResetTask extends BukkitRunnable {

    @Override
    public void run() {
        // Quét qua tất cả những người đang Online
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Lệnh check cực nhẹ, chỉ check biến Time, mất 0.0001ms mỗi người
            JobQuestManager.checkAndResetQuests(player);
        }
    }
}