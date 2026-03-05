package vn.minecraft.rPGCore.player;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.RPGClass;

public class RPGPlayer {
    private final Player bukkitPlayer;
    private RPGClass currentClass;
    private double currentResource;
    private double currentExp = 0; // Biến này sẽ lưu EXP lẻ
    private long lastCombatTime = 0;

    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;
        this.currentResource = defaultClass.getMaxResource();
    }

    // --- CÁC HÀM GETTER ---
    public RPGClass getCurrentClass() { return currentClass; }
    public Player getBukkitPlayer() { return bukkitPlayer; }
    public double getCurrentResource() { return currentResource; }
    public double getCurrentHealth() { return this.bukkitPlayer.getHealth(); }
    public double getCurrentExp() { return currentExp; }

    // --- GIAO TRANH ---
    public void setInCombat() { this.lastCombatTime = System.currentTimeMillis(); }
    public boolean isInCombat() { return (System.currentTimeMillis() - this.lastCombatTime) <= 10000; }

    // --- HỆ THỐNG EXP VÀ LEVEL ---

    /**
     * Công thức bạn yêu cầu: Base 500 + (Lvl * 100) + (Lvl^2)^2
     * Phân tích độ ảnh hưởng:
     * - Lv 0: 500 (Base chiếm 100%)
     * - Lv 10: 500 + 1000 + 10,000 = 11,500 (L^4 bắt đầu chiếm ưu thế)
     * - Lv 20: 500 + 2000 + 160,000 = 162,500 (Bức tường thực sự)
     */
    public double getRequiredExp() {
        int level = this.bukkitPlayer.getLevel();
        return 500 + (level * 100) + Math.pow(level, 3) * level/4;
    }

    public void addExp(double amount) {
        this.currentExp += amount;

        // Xử lý lên cấp
        while (this.currentExp >= getRequiredExp()) {
            this.currentExp -= getRequiredExp();
            int newLevel = this.bukkitPlayer.getLevel() + 1;
            this.bukkitPlayer.setLevel(newLevel);

            // Cập nhật máu tối đa ngay khi lên cấp
            updateMaxStats();

            this.bukkitPlayer.sendMessage("§b§lLEVEL UP! §fBạn đã đạt cấp độ §e" + newLevel);
            this.bukkitPlayer.playSound(this.bukkitPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        updateExpBar();
    }

    public void updateExpBar() {
        float progress = (float) (this.currentExp / getRequiredExp());
        this.bukkitPlayer.setExp(Math.min(0.999f, progress));
    }

    // --- HỒI PHỤC VÀ CHỈ SỐ ---

    public void heal(double amount) {
        if (!this.bukkitPlayer.isDead()) {
            // Lấy giới hạn máu THỰC TẾ mà server đang gán cho người chơi này
            AttributeInstance maxAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr == null) return;

            double actualMax = maxAttr.getValue(); // Đây là con số 1600.0 trong lỗi của bạn
            double currentHp = this.bukkitPlayer.getHealth();

            // Đảm bảo máu mới không bao giờ vượt quá giới hạn thuộc tính
            double newHp = Math.min(currentHp + amount, actualMax);

            this.bukkitPlayer.setHealth(newHp);
        }
    }

    public void setClass(RPGClass newClass) {
        this.currentClass = newClass;
        this.currentResource = getTotalMaxResource();
        updateMaxStats();
        this.bukkitPlayer.setHealth(getTotalMaxHealth());
    }

    public void updateMaxStats() {
        double totalMaxHp = getTotalMaxHealth();
        // Dùng GENERIC_MAX_HEALTH cho bản 1.21.1
        AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.setBaseValue(totalMaxHp);
        }
        this.bukkitPlayer.setHealthScale(20.0);
        this.bukkitPlayer.setHealthScaled(true);
    }

    public void setCurrentResource(double currentResource) {
        double maxResource = getTotalMaxResource();
        this.currentResource = Math.max(0, Math.min(currentResource, maxResource));
    }

    public double getTotalMaxHealth() {
        return this.currentClass.getMaxHealth() + (this.bukkitPlayer.getLevel() * 50.0);
    }

    public double getTotalMaxResource() {
        return this.currentClass.getMaxResource();
    }

    /**
     * Xử lý hình phạt mất EXP khi chết.
     * @param percentage Tỷ lệ mất (ví dụ 0.3 cho 30%)
     */
    public void loseDeathExp(double percentage) {
        if (this.currentExp <= 0) return;

        // Tính toán lượng EXP bị mất dựa trên tiến độ hiện tại (90k * 0.3)
        double lostAmount = this.currentExp * percentage;
        this.currentExp -= lostAmount;

        // Đảm bảo EXP không bị âm (đề phòng lỗi tính toán)
        if (this.currentExp < 0) this.currentExp = 0;

        // Cập nhật lại thanh EXP xanh lá của Minecraft
        updateExpBar();

        // Thông báo cho người chơi
        this.bukkitPlayer.sendMessage("§c§l☠ BẠN ĐÃ TỬ VONG!");
        this.bukkitPlayer.sendMessage("§cHình phạt cấp độ > 20: Mất §e" + (int)lostAmount + " EXP §c(30% tiến độ).");
    }
}