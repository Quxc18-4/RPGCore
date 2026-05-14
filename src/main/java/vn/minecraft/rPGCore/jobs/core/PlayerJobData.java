package vn.minecraft.rPGCore.jobs.core;

import java.util.HashMap;
import java.util.Map;

public class PlayerJobData {
    private String currentJobId;

    // Lưu trữ cấp độ và EXP của TẤT CẢ các nghề đã học
    private Map<String, Integer> jobLevels;
    private Map<String, Double> jobExps;

    private int blacksmithScraps;

    public PlayerJobData() {
        this.currentJobId = "NONE"; // Mặc định chưa có nghề
        this.jobLevels = new HashMap<>();
        this.jobExps = new HashMap<>();
        this.blacksmithScraps = 0;
    }

    public String getJobId() { return currentJobId; }
    public void setJobId(String jobId) { this.currentJobId = jobId.toUpperCase(); }

    // Các hàm lấy/cài đặt Cấp độ (Level)
    public int getLevel(String jobId) { return jobLevels.getOrDefault(jobId.toUpperCase(), 1); }
    public int getLevel() { return getLevel(currentJobId); }
    public void setLevel(String jobId, int level) { jobLevels.put(jobId.toUpperCase(), level); }

    // Các hàm lấy/cài đặt Kinh nghiệm (EXP)
    public double getExp(String jobId) { return jobExps.getOrDefault(jobId.toUpperCase(), 0.0); }
    public double getExp() { return getExp(currentJobId); }
    public void setExp(String jobId, double exp) { jobExps.put(jobId.toUpperCase(), exp); }
    public void addExp(double amount) { setExp(currentJobId, getExp() + amount); }

    public int getBlacksmithScraps() { return blacksmithScraps; }
    public void addBlacksmithScraps(int amount) { this.blacksmithScraps += amount; }
}