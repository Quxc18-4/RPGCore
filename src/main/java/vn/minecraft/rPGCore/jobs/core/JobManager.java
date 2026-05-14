package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JobManager {
    private static final Map<String, RPGJob> registeredJobs = new HashMap<>();
    private static final Map<UUID, PlayerJobData> playerData = new HashMap<>();

    public static void registerJob(RPGJob job) {
        registeredJobs.put(job.getId().toUpperCase(), job);
    }

    public static RPGJob getPlayerJob(Player player) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        return (data != null) ? registeredJobs.get(data.getJobId().toUpperCase()) : null;
    }

    public static int getPlayerJobLevel(Player player) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        return (data != null) ? data.getLevel() : 0;
    }

    // Xử lý cộng EXP cho người chơi
    public static void addExp(Player player, double amount) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        if (data == null) return;

        RPGJob job = getPlayerJob(player);
        if (job == null || data.getLevel() >= job.getMaxLevel()) return;

        data.addExp(amount);
        player.sendMessage("§a[Nghề Phụ] §fBạn vừa nhận được §e" + amount + " EXP §fnghề §b" + job.getName() + "§f!");

        // Cần thêm logic check level up tại đây (Sẽ triển khai ở Phase 4)
    }

    // Mở GUI chọn/xem thông tin nghề (Lệnh /job)
    public static void openJobGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§l§8MENU NGHỀ PHỤ");
        // Logic loop qua registeredJobs và setItem vào inv
        // (Chi tiết render Item sẽ sử dụng job.getIcon() và job.getDescription())
        player.openInventory(inv);
    }

    public static java.util.Collection<RPGJob> getAllJobs() {
        return registeredJobs.values();
    }

    public static PlayerJobData getPlayerData(Player player) {
        playerData.putIfAbsent(player.getUniqueId(), new PlayerJobData());
        return playerData.get(player.getUniqueId());
    }

    /* =========================================
     * HOOK API DÀNH CHO CÁC CLASS BÊN NGOÀI
     * ========================================= */

    // Hook cho BlacksmithGUI.java
    public static double getForgeBonus(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getForgeSuccessBonus(getPlayerJobLevel(player)) : 0.0;
    }

    public static double getLineageBonus(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getLineageStarBonus(getPlayerJobLevel(player)) : 0.0;
    }

    // Hook cho CookingManager.java
    public static double getFreshnessBuff(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getFreshnessMultiplier(getPlayerJobLevel(player)) : 1.0;
    }

    // ... Sẽ bổ sung thêm các Hook khác tương tự
}