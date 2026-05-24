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

import static java.lang.Math.round;

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
        player.sendMessage("§a[Nghề Phụ] §fBạn vừa nhận được §e" + String.format("%.1f", amount) + " EXP §fnghề §b" + job.getName() + "§f!");

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

        // LOAD DỮ LIỆU TÍCH LŨY TIỀN THƯƠNG NHÂN
        data.setMerchantAccumulatedCoins(config.getLong("merchantAccumulatedCoins", 0L));

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

        // SAVE DỮ LIỆU TÍCH LŨY TIỀN THƯƠNG NHÂN
        config.set("merchantAccumulatedCoins", data.getMerchantAccumulatedCoins());

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

    // =========================================================================
    // HÀM TÍNH TOÁN EXP & CHỨNG NHẬN GIAO DỊCH (TÍCH LŨY QUA PLAYERJOBDATA)
    // =========================================================================
    public static void processMerchantExp(Player player, long amount) {
        PlayerJobData data = getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        int level = data.getLevel("MERCHANT");

        // 1. TÍNH TOÁN EXP NGHỀ NGHIỆP
        double expPerCoin = 0.1 + (level * 0.002);
        double totalExp = amount * expPerCoin;
        addExp(player, totalExp);

        // 2. LOGIC RỚT CHỨNG NHẬN TÍCH LŨY DÒNG TIỀN
        long currentAcc = data.getMerchantAccumulatedCoins();
        long totalCoins = currentAcc + amount;

        // Đổi ra số lượng đơn vị Vàng luân chuyển (1 Vàng = 4096 Đồng)
        long goldUnits = totalCoins / 4096;
        long leftoverCoins = totalCoins % 4096;

        // Ghi lại số tiền lẻ chưa đủ 1 Vàng vào bộ nhớ data nghề nghiệp để tích lũy cho lần sau
        data.setMerchantAccumulatedCoins(leftoverCoins);

        // Nếu tổng số tiền tích lũy từ các giao dịch trước đạt đủ mốc từ 1 Vàng trở lên
        if (goldUnits > 0) {
            // Cân bằng lại tỉ lệ: Cấp * 0.5% cho mỗi 1 Vàng. Tối đa 25.0% ở Cấp 50.
            double tokenDropChance = Math.min(level * 0.5, 25.0);
            double totalExpected = goldUnits * (tokenDropChance / 100.0);

            // Thuật toán xúc xắc an toàn (Ví dụ: 1.25 -> Chắc chắn 1, có 25% nhận thêm cái thứ 2)
            int tokens = (int) totalExpected;
            if (Math.random() < (totalExpected - tokens)) {
                tokens++;
            }

            // Tiến hành cấp Chứng Nhận Thương Nhân nếu trúng thưởng
            if (tokens > 0) {
                org.bukkit.inventory.ItemStack tokenItem = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().buildItem("misc_merchant_token", tokens);
                if (tokenItem != null) {
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftOver = player.getInventory().addItem(tokenItem);
                    for (org.bukkit.inventory.ItemStack drop : leftOver.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    player.sendMessage("§e§l[Uy Tín Thương Trường] §fDòng tiền giao dịch đạt §6" + goldUnits + " Vàng§f, bạn nhận được §a+" + tokens + " Chứng Nhận Thương Nhân§f!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
            }
        }
    }
}