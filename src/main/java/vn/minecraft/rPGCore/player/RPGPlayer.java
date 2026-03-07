package vn.minecraft.rPGCore.player;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.data.PlayerAttributeData;
import vn.minecraft.rPGCore.player.data.PlayerSkillData;

public class RPGPlayer {
    private final Player bukkitPlayer;
    private RPGClass currentClass;

    // Core Stats
    private double currentResource;
    private double currentExp = 0;
    private long lastCombatTime = 0;

    // Các Mô-đun (Components)
    private final PlayerAttributeData attributeData;
    private final PlayerSkillData skillData;

    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;
        this.currentResource = defaultClass.getMaxResource();

        // Khởi tạo các Component
        this.attributeData = new PlayerAttributeData(this);
        this.skillData = new PlayerSkillData();
    }

    // --- GETTERS CHO COMPONENTS ---
    public PlayerAttributeData getAttributeData() { return attributeData; }
    public PlayerSkillData getSkillData() { return skillData; }

    // --- CÁC HÀM CƠ BẢN ---
    public RPGClass getCurrentClass() { return currentClass; }
    public Player getBukkitPlayer() { return bukkitPlayer; }
    public double getCurrentResource() { return currentResource; }
    public double getCurrentHealth() { return this.bukkitPlayer.getHealth(); }
    public double getCurrentExp() { return currentExp; }
    public void setCurrentExp(double exp) { this.currentExp = exp; }

    public void setInCombat() { this.lastCombatTime = System.currentTimeMillis(); }
    public boolean isInCombat() { return (System.currentTimeMillis() - this.lastCombatTime) <= 10000; }

    // --- HỆ THỐNG EXP & LEVEL ---
    public double getRequiredExp() {
        int level = this.bukkitPlayer.getLevel();
        return 500 + (level * 100) + Math.pow(level, 3) * level/4;
    }

    public void addExp(double amount) {
        this.currentExp += amount;
        while (this.currentExp >= getRequiredExp()) {
            this.currentExp -= getRequiredExp();
            int newLevel = this.bukkitPlayer.getLevel() + 1;
            this.bukkitPlayer.setLevel(newLevel);

            // Gọi Component để thưởng điểm
            this.attributeData.addUnspentAttrPoints(3);
            this.skillData.addSkillPoints(1);

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

    public void loseDeathExp(double percentage) {
        if (this.currentExp <= 0) return;
        double lostAmount = this.currentExp * percentage;
        this.currentExp = Math.max(0, this.currentExp - lostAmount);
        updateExpBar();
        this.bukkitPlayer.sendMessage("§c§l☠ BẠN ĐÃ TỬ VONG!");
        this.bukkitPlayer.sendMessage("§cHình phạt cấp độ > 20: Mất §e" + (int)lostAmount + " EXP §c(30% tiến độ).");
    }

    // --- HỒI PHỤC VÀ CHỈ SỐ MAX ---
    public void heal(double amount) {
        if (!this.bukkitPlayer.isDead()) {
            AttributeInstance maxAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr == null) return;
            double newHp = Math.min(this.bukkitPlayer.getHealth() + amount, maxAttr.getValue());
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
        AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) maxHpAttr.setBaseValue(totalMaxHp);
        this.bukkitPlayer.setHealthScale(20.0);
        this.bukkitPlayer.setHealthScaled(true);
    }

    public void setCurrentResource(double currentResource) {
        this.currentResource = Math.max(0, Math.min(currentResource, getTotalMaxResource()));
    }

    public double getTotalMaxHealth() {
        double baseHp = this.currentClass.getMaxHealth();
        double levelBonus = this.bukkitPlayer.getLevel() * 50.0;
        // Lấy VIT từ Component
        double vitBonus = this.attributeData.getAttribute("VIT") * 15.0;
        return baseHp + levelBonus + vitBonus;
    }

    public double getTotalMaxResource() {
        double baseRes = this.currentClass.getMaxResource();
        // Lấy INT từ Component
        double intBonus = this.attributeData.getAttribute("INT") * 1.0;
        return baseRes + intBonus;
    }
}