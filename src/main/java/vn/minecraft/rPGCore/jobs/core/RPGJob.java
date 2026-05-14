package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public abstract class RPGJob {
    protected String id;
    protected String name;
    protected int maxLevel = 50;

    public RPGJob(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getMaxLevel() { return maxLevel; }

    // Giao diện hiển thị trên GUI
    public abstract ItemStack getIcon(int currentLevel);
    public abstract List<String> getDescription(int currentLevel);

    /* =========================================
     * CÁC HÀM ẢO (VIRTUAL METHODS) CHO HOOK API
     * (Chỉ override ở class nghề tương ứng)
     * ========================================= */

    // --- DÀNH CHO BLACKSMITH (Thợ Rèn) ---
    public double getForgeSuccessBonus(int level) { return 0.0; }
    public double getLineageStarBonus(int level) { return 0.0; }
    public double getStoneSaveChance(int level) { return 0; }
    public double getScrapRefundChance(int level) { return 0.0; }

    // --- DÀNH CHO MASTER CHEF (Đầu Bếp) ---
    public double getFreshnessMultiplier(int level) { return 1.0; } // 1.0 = 100% (Bình thường)
    public double getDurationMultiplier(int level) { return 1.0; }
    public double getBoredomReduction(int level) { return 0.0; }
    public double getDoublePortionChance(int level) { return 0.0; }

    // --- DÀNH CHO MERCHANT (Thương Nhân) ---
    public double getCoinDropBonus(int level) { return 0.0; }
    public double getDepositInterestBonus(int level) { return 0.0; }
}