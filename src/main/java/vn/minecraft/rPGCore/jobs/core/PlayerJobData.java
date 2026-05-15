package vn.minecraft.rPGCore.jobs.core;

import java.util.HashMap;
import java.util.Map;

public class PlayerJobData {
    private String currentJobId;

    // Lưu trữ cấp độ và EXP
    private Map<String, Integer> jobLevels;
    private Map<String, Double> jobExps;

    private int blacksmithScraps;

    // Hệ thống Daily/Weekly Commission
    private java.util.List<String> activeDailyQuests;
    private java.util.List<String> activeWeeklyQuests;
    private int dailyMilestone; // Điểm năng động ngày (0 -> 3)
    private int currentForgeStreak; // Đếm chuỗi đập thành công

    // Lưu trữ Nhiệm Vụ
    private Map<String, Integer> questProgress;
    private Map<String, Boolean> questClaimed;
    private long lastDailyReset;
    private long lastWeeklyReset;

    public PlayerJobData() {
        this.currentJobId = "NONE";
        this.jobLevels = new HashMap<>();
        this.jobExps = new HashMap<>();
        this.blacksmithScraps = 0;

        this.questProgress = new HashMap<>();
        this.questClaimed = new HashMap<>();
        this.lastDailyReset = System.currentTimeMillis();
        this.lastWeeklyReset = System.currentTimeMillis();

        this.activeDailyQuests = new java.util.ArrayList<>();
        this.activeWeeklyQuests = new java.util.ArrayList<>();
        this.dailyMilestone = 0;
        this.currentForgeStreak = 0;
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
    public void addExp(double amount) { setExp(currentJobId, getExp() + amount); }

    public int getBlacksmithScraps() { return blacksmithScraps; }
    public void addBlacksmithScraps(int amount) { this.blacksmithScraps += amount; }

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

    // ==========================================
    // [MỚI]: HỖ TRỢ TRÍCH XUẤT ĐỂ LƯU XUỐNG FILE YML
    // ==========================================
    public Map<String, Integer> getJobLevelsMap() { return this.jobLevels; }
    public Map<String, Double> getJobExpsMap() { return this.jobExps; }
    public Map<String, Integer> getQuestProgressMap() { return this.questProgress; }
    public Map<String, Boolean> getQuestClaimedMap() { return this.questClaimed; }

    // ==========================================
    // CÁC HÀM QUẢN LÝ GACHA COMMISSION
    // ==========================================
    public java.util.List<String> getActiveDailyQuests() { return activeDailyQuests; }
    public void setActiveDailyQuests(java.util.List<String> activeDailyQuests) { this.activeDailyQuests = activeDailyQuests; }

    public java.util.List<String> getActiveWeeklyQuests() { return activeWeeklyQuests; }
    public void setActiveWeeklyQuests(java.util.List<String> activeWeeklyQuests) { this.activeWeeklyQuests = activeWeeklyQuests; }

    public int getDailyMilestone() { return dailyMilestone; }
    public void setDailyMilestone(int dailyMilestone) { this.dailyMilestone = dailyMilestone; }
    public void addDailyMilestone(int amount) { this.dailyMilestone += amount; }

    public int getCurrentForgeStreak() { return currentForgeStreak; }
    public void setCurrentForgeStreak(int streak) { this.currentForgeStreak = streak; }
}