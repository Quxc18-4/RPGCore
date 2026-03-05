package vn.minecraft.rPGCore.player;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.RPGClass;

public class RPGPlayer {
    private Player bukkitPlayer;
    private RPGClass currentClass;

    private double currentHealth;
    private double currentResource;

    // Thay đổi ở đây: Truyền RPGClass mặc định vào qua constructor
    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;

        // Lưu ý: Đổi getBaseMaxHealth() thành getMaxHealth() theo chuẩn POJO mới
        this.currentHealth = this.currentClass.getMaxHealth();
        this.currentResource = this.currentClass.getMaxResource();
    }

    public RPGClass getCurrentClass() { return currentClass; }
    public double getCurrentHealth() { return currentHealth; }
    public double getCurrentResource() { return currentResource; }
    public Player getBukkitPlayer() { return bukkitPlayer; }

    public void setClass(RPGClass newClass) {
        this.currentClass = newClass;
        this.currentHealth = newClass.getMaxHealth();
        this.currentResource = newClass.getMaxResource();
    }

    public void setCurrentHealth(double currentHealth) {
        double maxHealth = this.currentClass.getMaxHealth();
        this.currentHealth = Math.max(0, Math.min(currentHealth, maxHealth));
    }

    public void setCurrentResource(double currentResource) {
        double maxResource = this.currentClass.getMaxResource();
        this.currentResource = Math.max(0, Math.min(currentResource, maxResource));
    }
}