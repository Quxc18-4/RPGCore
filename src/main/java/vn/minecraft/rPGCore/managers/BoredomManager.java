package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoredomManager {

    // Lưu trữ % Điểm Tuyệt Thực (0 - 100)
    private final Map<UUID, Double> anorexiaPoints = new HashMap<>();

    // Lưu trữ số lần Cố nhồi nhét (Đạt 3 lần là bị Cấm ăn)
    private final Map<UUID, Integer> strikes = new HashMap<>();

    // Danh sách người chơi đang bị Cấm ăn
    private final Map<UUID, Boolean> bannedPlayers = new HashMap<>();

    public BoredomManager(JavaPlugin plugin) {
        startPassiveDigestion(plugin);
    }

    // Vòng lặp tự động giảm điểm mỗi 5 phút (6000 ticks)
    private void startPassiveDigestion(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (!anorexiaPoints.containsKey(uuid)) continue;

                    double current = anorexiaPoints.get(uuid);
                    boolean isBanned = bannedPlayers.getOrDefault(uuid, false);

                    // Mặc định: Giảm 3% nếu đang bị cấm, 1% nếu bình thường
                    double reduction = isBanned ? 3.0 : 1.0;

                    // [KỸ NĂNG TỐI THƯỢNG]: DẠ DÀY THÉP
                    vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                    if (data != null && data.hasUnlockedSkill("CHEF", "CHEF_IRON_STOMACH") && isBanned) {
                        reduction = 6.0; // Trừ lẹ gấp đôi -> Giảm 50% thời gian tuyệt thực
                    }

                    double newPoints = Math.max(0, current - reduction);
                    anorexiaPoints.put(uuid, newPoints);

                    if (isBanned && newPoints <= 20.0) {
                        unban(uuid);
                        player.sendMessage("§aCơ thể bạn đã tiêu hóa xong. Cảm giác thèm ăn đã quay trở lại!");
                    }
                }
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
    }

    public boolean isBanned(UUID uuid) {
        return bannedPlayers.getOrDefault(uuid, false);
    }

    public void unban(UUID uuid) {
        bannedPlayers.put(uuid, false);
        strikes.put(uuid, 0); // Reset số lần nhồi nhét
    }

    public void addStrikeAndPoints(UUID uuid) {
        double currentPoints = anorexiaPoints.getOrDefault(uuid, 0.0);
        anorexiaPoints.put(uuid, Math.min(100.0, currentPoints + 5.0)); // Cộng 5% điểm

        int currentStrikes = strikes.getOrDefault(uuid, 0) + 1;
        strikes.put(uuid, currentStrikes);

        if (currentStrikes >= 3) {
            bannedPlayers.put(uuid, true);
        }
    }

    public void reduceAnorexia(UUID uuid, double amount) {
        double current = anorexiaPoints.getOrDefault(uuid, 0.0);
        anorexiaPoints.put(uuid, Math.max(0.0, current - amount));
    }

    public double getPoints(UUID uuid) {
        return anorexiaPoints.getOrDefault(uuid, 0.0);
    }
}