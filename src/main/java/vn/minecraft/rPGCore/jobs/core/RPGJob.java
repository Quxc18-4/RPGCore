package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Arrays;

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

    public abstract java.util.Map<String, vn.minecraft.rPGCore.jobs.quests.RPGQuest> getQuests();

    public abstract ItemStack getIcon(Player player);
    public abstract List<String> getDescription(Player player);

    public abstract String getShopGUITitle();
    public abstract void openShop(Player player);
    public abstract void handleShopClick(org.bukkit.event.inventory.InventoryClickEvent event);

    public abstract String getSkillTreeGUITitle();
    public abstract void openSkillTree(Player player);
    public abstract void handleSkillTreeClick(org.bukkit.event.inventory.InventoryClickEvent event);

    // =========================================
    // HỆ THỐNG TRẢ THƯỞNG NHIỆM VỤ THEO NGHỀ
    // =========================================
    public void giveDailyMilestoneReward(Player player, int milestone) {}
    public void giveWeeklyMilestoneReward(Player player) {}

    // [MỚI]: Lấy Lore hiển thị cho Rương Phần Thưởng (Tránh hardcode)
    public List<String> getDailyRewardLore(int milestone) { return Arrays.asList("§6+ Phần thưởng nghề"); }
    public List<String> getWeeklyRewardLore() { return Arrays.asList("§6+ Phần thưởng nghề lớn"); }

    /* =========================================
     * CÁC HÀM ẢO (VIRTUAL METHODS) CHO HOOK API
     * ========================================= */
    public double getForgeSuccessBonus(int jobLevel, int itemLevel) { return 0.0; }
    public double getLineageStarBonus(int level) { return 0.0; }
    public double getStoneSaveChance(int level) { return 0; }
    public double getScrapRefundChance(int level) { return 0.0; }

    public double getFreshnessMultiplier(int level) { return 1.0; }
    public double getDurationMultiplier(int level) { return 1.0; }
    public double getBoredomReduction(int level) { return 0.0; }
    public double getDoublePortionChance(int level) { return 0.0; }

    public double getCoinDropBonus(int level) { return 0.0; }
    public double getDepositInterestBonus(int level) { return 0.0; }
}