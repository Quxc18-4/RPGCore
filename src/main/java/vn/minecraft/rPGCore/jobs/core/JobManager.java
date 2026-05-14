package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JobManager {
    private static final Map<String, RPGJob> registeredJobs = new HashMap<>();
    private static final Map<UUID, PlayerJobData> playerData = new HashMap<>();

    public static void registerJob(RPGJob job) {
        registeredJobs.put(job.getId().toUpperCase(), job);
    }

    public static Collection<RPGJob> getAllJobs() {
        return registeredJobs.values();
    }

    // Đã thay đổi: Gọi loadPlayerData nếu chưa có
    public static PlayerJobData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerData.containsKey(uuid)) {
            loadPlayerData(uuid);
        }
        return playerData.get(uuid);
    }

    public static RPGJob getPlayerJob(Player player) {
        PlayerJobData data = getPlayerData(player);
        return registeredJobs.get(data.getJobId().toUpperCase());
    }

    public static int getPlayerJobLevel(Player player) {
        return getPlayerData(player).getLevel();
    }

    // ==========================================
    // CÔNG THỨC TÍNH EXP YÊU CẦU CỦA NGHỀ
    // ==========================================
    public static double getRequiredJobExp(int currentLevel) {
        // Công thức: Cấp 1: 550, Cấp 10: 5500, Cấp 50: 125500
        return (Math.pow(currentLevel, 2) * 50) + 500;
    }

    // ==========================================
    // XỬ LÝ CỘNG EXP VÀ TỰ ĐỘNG THĂNG CẤP
    // ==========================================
    public static void addExp(Player player, double amount) {
        PlayerJobData data = getPlayerData(player);
        RPGJob job = getPlayerJob(player);

        if (job == null || data.getLevel() >= job.getMaxLevel()) return;

        // 1. Cộng EXP vào Data
        double currentExp = data.getExp() + amount;
        data.setExp(job.getId(), currentExp);
        player.sendMessage("§a[Nghề Phụ] §fBạn vừa nhận được §e" + amount + " EXP §fnghề §b" + job.getName() + "§f!");

        // 2. Logic kiểm tra Level Up [ĐÃ BỔ SUNG]
        boolean leveledUp = false;
        int currentLevel = data.getLevel();
        double requiredExp = getRequiredJobExp(currentLevel);

        while (currentExp >= requiredExp && currentLevel < job.getMaxLevel()) {
            currentExp -= requiredExp;
            currentLevel++;
            requiredExp = getRequiredJobExp(currentLevel);
            leveledUp = true;
        }

        // 3. Nếu có thăng cấp -> Cập nhật Data và báo hỷ
        if (leveledUp) {
            data.setLevel(job.getId(), currentLevel);
            data.setExp(job.getId(), currentExp);

            player.sendMessage("");
            player.sendMessage("§e✨ §lCHÚC MỪNG! §fKỹ năng nghề §b" + job.getName() + " §fcủa bạn đã thăng lên §aCấp " + currentLevel + "§f!");
            player.sendMessage("§7(Sử dụng lệnh /job menu để xem các chỉ số mới)");
            player.sendMessage("");

            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    // ==========================================
    // [MỚI]: HỆ THỐNG LƯU TRỮ (SAVE / LOAD YML)
    // ==========================================
    public static void loadPlayerData(UUID uuid) {
        File file = new File(vn.minecraft.rPGCore.RPGCore.getInstance().getDataFolder() + "/job_data", uuid.toString() + ".yml");
        if (!file.exists()) {
            playerData.put(uuid, new PlayerJobData());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerJobData data = new PlayerJobData();

        data.setJobId(config.getString("currentJob", "NONE"));
        data.addBlacksmithScraps(config.getInt("scraps", 0));

        if (config.contains("levels")) {
            for (String key : config.getConfigurationSection("levels").getKeys(false))
                data.setLevel(key, config.getInt("levels." + key));
        }
        if (config.contains("exps")) {
            for (String key : config.getConfigurationSection("exps").getKeys(false))
                data.setExp(key, config.getDouble("exps." + key));
        }
        if (config.contains("questProgress")) {
            for (String key : config.getConfigurationSection("questProgress").getKeys(false))
                data.setQuestProgress(key, config.getInt("questProgress." + key));
        }
        if (config.contains("questClaimed")) {
            for (String key : config.getConfigurationSection("questClaimed").getKeys(false))
                data.setQuestClaimed(key, config.getBoolean("questClaimed." + key));
        }

        data.setLastDailyReset(config.getLong("lastDailyReset", System.currentTimeMillis()));
        data.setLastWeeklyReset(config.getLong("lastWeeklyReset", System.currentTimeMillis()));

        playerData.put(uuid, data);
    }

    public static void savePlayerData(UUID uuid) {
        PlayerJobData data = playerData.get(uuid);
        if (data == null) return;

        File dir = new File(vn.minecraft.rPGCore.RPGCore.getInstance().getDataFolder(), "job_data");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("currentJob", data.getJobId());
        config.set("scraps", data.getBlacksmithScraps());

        for (Map.Entry<String, Integer> entry : data.getJobLevelsMap().entrySet()) config.set("levels." + entry.getKey(), entry.getValue());
        for (Map.Entry<String, Double> entry : data.getJobExpsMap().entrySet()) config.set("exps." + entry.getKey(), entry.getValue());
        for (Map.Entry<String, Integer> entry : data.getQuestProgressMap().entrySet()) config.set("questProgress." + entry.getKey(), entry.getValue());
        for (Map.Entry<String, Boolean> entry : data.getQuestClaimedMap().entrySet()) config.set("questClaimed." + entry.getKey(), entry.getValue());

        config.set("lastDailyReset", data.getLastDailyReset());
        config.set("lastWeeklyReset", data.getLastWeeklyReset());

        try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveAll() {
        for (UUID uuid : playerData.keySet()) {
            savePlayerData(uuid);
        }
    }

    // ==========================================
    // [MỚI]: TỰ ĐỘNG LƯU KHI NGƯỜI CHƠI THOÁT GAME
    // ==========================================
    public static class JobDataListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            loadPlayerData(event.getPlayer().getUniqueId());
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            UUID uuid = event.getPlayer().getUniqueId();
            savePlayerData(uuid);
            playerData.remove(uuid); // Xóa khỏi RAM để chống rò rỉ bộ nhớ
        }
    }

    /* =========================================
     * HOOK API DÀNH CHO CÁC CLASS BÊN NGOÀI
     * ========================================= */
    public static double getForgeBonus(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getForgeSuccessBonus(getPlayerJobLevel(player)) : 0.0;
    }
    public static double getLineageBonus(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getLineageStarBonus(getPlayerJobLevel(player)) : 0.0;
    }
    public static double getFreshnessBuff(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getFreshnessMultiplier(getPlayerJobLevel(player)) : 1.0;
    }
}