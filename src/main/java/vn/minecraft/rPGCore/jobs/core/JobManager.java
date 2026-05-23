package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.*;

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
    // XỬ LÝ CỘNG EXP VÀ TỰ ĐỘNG THĂNG CẤP / NHẬN JP
    // ==========================================
    public static void addExp(Player player, double amount) {
        PlayerJobData data = getPlayerData(player);
        RPGJob job = getPlayerJob(player);

        if (job == null) return; // Không có dòng chặn maxLevel nữa

        int currentLevel = data.getLevel();
        int maxLevel = job.getMaxLevel();

        // 1. Cộng EXP vào Data
        double currentExp = data.getExp() + amount;
        data.setExp(job.getId(), currentExp);
        player.sendMessage("§a[Nghề Phụ] §fBạn vừa nhận được §e" + amount + " EXP §fnghề §b" + job.getName() + "§f!");

        // 2. Logic kiểm tra Level Up hoặc Đổi JP
        boolean leveledUp = false;
        int jpGained = 0;
        double requiredExp = getRequiredJobExp(currentLevel);

        while (currentExp >= requiredExp) {
            currentExp -= requiredExp;

            if (currentLevel < maxLevel) {
                // Chưa max -> Thăng cấp bình thường
                currentLevel++;
                leveledUp = true;
                requiredExp = getRequiredJobExp(currentLevel);
            } else {
                // Đã max cấp 50 -> Quy đổi thành Điểm Kỹ Năng (JP)
                jpGained++;
                requiredExp = getRequiredJobExp(maxLevel); // Mốc EXP yêu cầu giữ nguyên ở cấp 50
            }
        }

        // 3. Cập nhật lại số EXP còn dư
        data.setExp(job.getId(), currentExp);

        // 4. Thông báo Thăng Cấp
        if (leveledUp) {
            data.setLevel(job.getId(), currentLevel);
            player.sendMessage("");
            player.sendMessage("§e✨ §lCHÚC MỪNG! §fKỹ năng nghề §b" + job.getName() + " §fcủa bạn đã thăng lên §aCấp " + currentLevel + "§f!");
            player.sendMessage("§7(Sử dụng lệnh /job menu để xem các chỉ số mới)");
            player.sendMessage("");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // 5. Thông báo Nhận Điểm Kỹ Năng (JP)
        if (jpGained > 0) {
            data.addJobPoints(job.getId(), jpGained);
            player.sendMessage("");
            player.sendMessage("§d✨ §lĐỘT PHÁ TỐI THƯỢNG! §fBạn vừa nhận được §d+" + jpGained + " Điểm Kỹ Năng (JP) §fcho nghề §b" + job.getName() + "§f!");
            player.sendMessage("§7(Hãy mở Cây Kỹ Năng để học các nội tại ẩn)");
            player.sendMessage("");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
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

        if (config.contains("levels")) for (String key : config.getConfigurationSection("levels").getKeys(false)) data.setLevel(key, config.getInt("levels." + key));
        if (config.contains("exps")) for (String key : config.getConfigurationSection("exps").getKeys(false)) data.setExp(key, config.getDouble("exps." + key));
        if (config.contains("questProgress")) for (String key : config.getConfigurationSection("questProgress").getKeys(false)) data.setQuestProgress(key, config.getInt("questProgress." + key));
        if (config.contains("questClaimed")) for (String key : config.getConfigurationSection("questClaimed").getKeys(false)) data.setQuestClaimed(key, config.getBoolean("questClaimed." + key));

        // [MỚI]: LOAD ĐIỂM JP VÀ KỸ NĂNG
        if (config.contains("jobPoints")) for (String key : config.getConfigurationSection("jobPoints").getKeys(false)) data.setJobPoints(key, config.getInt("jobPoints." + key));
        if (config.contains("unlockedSkills")) {
            for (String key : config.getConfigurationSection("unlockedSkills").getKeys(false)) {
                for (String skillId : config.getStringList("unlockedSkills." + key)) data.unlockSkill(key, skillId);
            }
        }

        // LOAD LỊCH SỬ MUA HÀNG SHOP
        if (config.contains("shopPurchases")) {
            for (String key : config.getConfigurationSection("shopPurchases").getKeys(false)) {
                data.getShopMonthlyPurchases().put(key, config.getInt("shopPurchases." + key));
            }
        }
        data.setLastMonthlyShopReset(config.getLong("lastMonthlyShopReset", System.currentTimeMillis()));

        data.setActiveDailyQuests(config.getStringList("activeDaily"));
        data.setActiveWeeklyQuests(config.getStringList("activeWeekly"));
        data.setDailyMilestone(config.getInt("dailyMilestone", 0));

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

        // [MỚI]: SAVE ĐIỂM JP VÀ KỸ NĂNG
        for (Map.Entry<String, Integer> entry : data.getJobPointsMap().entrySet()) config.set("jobPoints." + entry.getKey(), entry.getValue());
        for (Map.Entry<String, List<String>> entry : data.getUnlockedSkillsMap().entrySet()) config.set("unlockedSkills." + entry.getKey(), entry.getValue());

        // SAVE LỊCH SỬ MUA HÀNG SHOP
        for (Map.Entry<String, Integer> entry : data.getShopMonthlyPurchases().entrySet()) {
            config.set("shopPurchases." + entry.getKey(), entry.getValue());
        }

        config.set("lastMonthlyShopReset", data.getLastMonthlyShopReset());
        config.set("activeDaily", data.getActiveDailyQuests());
        config.set("activeWeekly", data.getActiveWeeklyQuests());
        config.set("dailyMilestone", data.getDailyMilestone());

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
            Player player = event.getPlayer();
            loadPlayerData(player.getUniqueId());

            // Tự động kiểm tra và bốc nhiệm vụ nếu là ngày mới hoặc người chơi chưa có quest
            Bukkit.getScheduler().runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), () -> {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.checkAndResetQuests(player);
            }, 20L); // Delay 1 giây (20 Ticks) cho an toàn lúc đăng nhập
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
    public static double getForgeBonus(Player player, int itemLevel) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getForgeSuccessBonus(getPlayerJobLevel(player), itemLevel) : 0.0;
    }
    public static double getLineageBonus(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getLineageStarBonus(getPlayerJobLevel(player)) : 0.0;
    }
    public static double getFreshnessBuff(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getFreshnessMultiplier(getPlayerJobLevel(player)) : 1.0;
    }

    // ==========================================
    // HÀM TÍNH TOÁN EXP GIAO DỊCH CHO THƯƠNG NHÂN
    // ==========================================
    public static void processMerchantExp(Player player, long amount) {
        PlayerJobData data = getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        int level = data.getLevel("MERCHANT");
        // Scale: Cấp 1 được 0.1 EXP/Xu -> Cấp 50 được 0.2 EXP/Xu
        double expPerCoin = 0.1 + (level * 0.002);
        double totalExp = amount * expPerCoin;

        // Gọi hàm addExp để tự động cộng và check thăng cấp
        addExp(player, totalExp);

        // Ghi log báo cho người chơi
        player.sendMessage("§e§l[Thương Nhân] §fNhận được §d" + String.format("%.1f", totalExp) + " EXP §fnghề từ giao dịch này.");
    }
}