package vn.minecraft.rPGCore.jobs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerJobData {
    private String currentJobId;

    // Lưu trữ cấp độ và EXP
    private Map<String, Integer> jobLevels;
    private Map<String, Double> jobExps;

    // Thêm biến thuộc tính này vào đầu class PlayerJobData
    private long merchantAccumulatedCoins = 0L;

    // HỆ THỐNG ĐIỂM JP VÀ CÂY KỸ NĂNG (END-GAME)
    private Map<String, Integer> jobPoints;
    private Map<String, List<String>> unlockedSkills;

    private int blacksmithScraps;

    // Hệ thống Daily/Weekly Commission
    private List<String> activeDailyQuests;
    private List<String> activeWeeklyQuests;
    private int dailyMilestone;
    private int currentForgeStreak;

    // Lưu trữ Nhiệm Vụ
    private Map<String, Integer> questProgress;
    private Map<String, Boolean> questClaimed;
    private long lastDailyReset;
    private long lastWeeklyReset;

    // [BỔ SUNG]: QUẢN LÝ LỊCH SỬ MUA HÀNG SHOP (Giải quyết lỗi đỏ)
    private Map<String, Integer> shopMonthlyPurchases;
    private long lastMonthlyShopReset;

    public PlayerJobData() {
        this.currentJobId = "NONE";
        this.jobLevels = new HashMap<>();
        this.jobExps = new HashMap<>();
        this.jobPoints = new HashMap<>();
        this.unlockedSkills = new HashMap<>();

        this.blacksmithScraps = 0;

        this.questProgress = new HashMap<>();
        this.questClaimed = new HashMap<>();
        this.lastDailyReset = System.currentTimeMillis();
        this.lastWeeklyReset = System.currentTimeMillis();

        this.activeDailyQuests = new ArrayList<>();
        this.activeWeeklyQuests = new ArrayList<>();
        this.dailyMilestone = 0;
        this.currentForgeStreak = 0;

        // Khởi tạo biến Shop
        this.shopMonthlyPurchases = new HashMap<>();
        this.lastMonthlyShopReset = System.currentTimeMillis();
    }

    // Các hàm Getter/Setter cơ bản
    public String getJobId() { return currentJobId; }
    public void setJobId(String jobId) { this.currentJobId = jobId.toUpperCase(); }

    public int getLevel(String jobId) { return jobLevels.getOrDefault(jobId.toUpperCase(), 1); }
    public int getLevel() { return getLevel(currentJobId); }
    public void setLevel(String jobId, int level) { jobLevels.put(jobId.toUpperCase(), level); }

    public double getExp(String jobId) { return jobExps.getOrDefault(jobId.toUpperCase(), 0.0); }
    public double getExp() { return getExp(currentJobId); }
    public void setExp(String jobId, double exp) { jobExps.put(jobId.toUpperCase(), exp); }

    public int getBlacksmithScraps() { return blacksmithScraps; }
    public void addBlacksmithScraps(int amount) { this.blacksmithScraps += amount; }

    // CÁC HÀM QUẢN LÝ ĐIỂM JP VÀ KỸ NĂNG
    public Map<String, Integer> getJobPointsMap() { return this.jobPoints; }
    public Map<String, List<String>> getUnlockedSkillsMap() { return this.unlockedSkills; }

    public int getJobPoints(String jobId) { return jobPoints.getOrDefault(jobId.toUpperCase(), 0); }
    public int getJobPoints() { return getJobPoints(currentJobId); }
    public void setJobPoints(String jobId, int points) { jobPoints.put(jobId.toUpperCase(), points); }
    public void addJobPoints(String jobId, int points) { setJobPoints(jobId, getJobPoints(jobId) + points); }

    public List<String> getUnlockedSkills(String jobId) { return unlockedSkills.getOrDefault(jobId.toUpperCase(), new ArrayList<>()); }
    public boolean hasUnlockedSkill(String jobId, String skillId) { return getUnlockedSkills(jobId).contains(skillId); }
    public void unlockSkill(String jobId, String skillId) {
        List<String> skills = getUnlockedSkills(jobId);
        if (!skills.contains(skillId)) {
            skills.add(skillId);
            unlockedSkills.put(jobId.toUpperCase(), skills);
        }
    }

    // [BỔ SUNG]: CÁC HÀM ĐỂ SHOP GỌI KHÔNG BỊ LỖI
    public Map<String, Integer> getShopMonthlyPurchases() { return shopMonthlyPurchases; }
    public int getPurchasedAmount(String itemId) { return shopMonthlyPurchases.getOrDefault(itemId, 0); }
    public void addPurchase(String itemId, int amount) { shopMonthlyPurchases.put(itemId, getPurchasedAmount(itemId) + amount); }
    public long getLastMonthlyShopReset() { return lastMonthlyShopReset; }
    public void setLastMonthlyShopReset(long time) { this.lastMonthlyShopReset = time; }

    // Các hàm Getter/Setter cho Quest
    public int getQuestProgress(String questKey) { return questProgress.getOrDefault(questKey, 0); }
    public void setQuestProgress(String questKey, int amount) { questProgress.put(questKey, amount); }
    public void addQuestProgress(String questKey, int amount) { questProgress.put(questKey, getQuestProgress(questKey) + amount); }

    public boolean isQuestClaimed(String questKey) { return questClaimed.getOrDefault(questKey, false); }
    public void setQuestClaimed(String questKey, boolean state) { questClaimed.put(questKey, state); }

    public long getLastDailyReset() { return lastDailyReset; }
    public void setLastDailyReset(long time) { this.lastDailyReset = time; }
    public long getLastWeeklyReset() { return lastWeeklyReset; }
    public void setLastWeeklyReset(long time) { this.lastWeeklyReset = time; }

    // Hỗ trợ lưu YAML
    public Map<String, Integer> getJobLevelsMap() { return this.jobLevels; }
    public Map<String, Double> getJobExpsMap() { return this.jobExps; }
    public Map<String, Integer> getQuestProgressMap() { return this.questProgress; }
    public Map<String, Boolean> getQuestClaimedMap() { return this.questClaimed; }

    // Commission Gacha
    public List<String> getActiveDailyQuests() { return activeDailyQuests; }
    public void setActiveDailyQuests(List<String> activeDailyQuests) { this.activeDailyQuests = activeDailyQuests; }
    public List<String> getActiveWeeklyQuests() { return activeWeeklyQuests; }
    public void setActiveWeeklyQuests(List<String> activeWeeklyQuests) { this.activeWeeklyQuests = activeWeeklyQuests; }
    public int getDailyMilestone() { return dailyMilestone; }
    public void setDailyMilestone(int dailyMilestone) { this.dailyMilestone = dailyMilestone; }
    public void addDailyMilestone(int amount) { this.dailyMilestone += amount; }
    public int getCurrentForgeStreak() { return currentForgeStreak; }
    public void setCurrentForgeStreak(int streak) { this.currentForgeStreak = streak; }
    // Thêm hai hàm Getter và Setter này vào cuối class
    public long getMerchantAccumulatedCoins() {
        return this.merchantAccumulatedCoins;
    }

    public void setMerchantAccumulatedCoins(long coins) {
        this.merchantAccumulatedCoins = coins;
    }
}