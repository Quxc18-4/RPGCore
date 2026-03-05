package vn.minecraft.rPGCore.player;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.RPGClass;

public class RPGPlayer {
    private final Player bukkitPlayer;
    private RPGClass currentClass;
    private double currentResource;

    // THÊM: Biến lưu trữ thời gian lần cuối bị đánh hoặc đánh người khác
    private long lastCombatTime = 0;

    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;
        this.currentResource = defaultClass.getMaxResource();
    }

    public RPGClass getCurrentClass() { return currentClass; }
    public Player getBukkitPlayer() { return bukkitPlayer; }
    public double getCurrentResource() { return currentResource; }
    public double getCurrentHealth() { return this.bukkitPlayer.getHealth(); }

    // --- CÁC HÀM XỬ LÝ GIAO TRANH & HỒI MÁU ---
    public void setInCombat() {
        this.lastCombatTime = System.currentTimeMillis();
    }

    public boolean isInCombat() {
        // Kiểm tra xem 10 giây (10000ms) đã trôi qua kể từ lần cuối giao tranh chưa
        return (System.currentTimeMillis() - this.lastCombatTime) <= 10000;
    }

    public void heal(double amount) {
        if (!this.bukkitPlayer.isDead()) {
            double maxHp = this.currentClass.getMaxHealth();
            double newHp = Math.min(this.bukkitPlayer.getHealth() + amount, maxHp);
            this.bukkitPlayer.setHealth(newHp);
        }
    }
    // ------------------------------------------

    public void setClass(RPGClass newClass) {
        this.currentClass = newClass;
        this.currentResource = newClass.getMaxResource();

        AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.setBaseValue(newClass.getMaxHealth());
        }

        this.bukkitPlayer.setHealthScale(20.0);
        this.bukkitPlayer.setHealthScaled(true);
        this.bukkitPlayer.setHealth(newClass.getMaxHealth());
    }

    public void setCurrentResource(double currentResource) {
        double maxResource = this.currentClass.getMaxResource();
        this.currentResource = Math.max(0, Math.min(currentResource, maxResource));
    }

    /**
     * Tính toán tổng lượng máu tối đa dựa trên công thức:
     * $TotalMaxHealth = BaseHP + (Level \times 50)$
     */
    public double getTotalMaxHealth() {
        // Lấy máu gốc được định nghĩa trong file classes.yml
        double baseHp = this.currentClass.getMaxHealth();

        // Mỗi cấp độ (Level) của người chơi sẽ được cộng thêm 50 máu
        double levelBonus = this.bukkitPlayer.getLevel() * 50.0;

        // TODO: Trong tương lai, bạn có thể cộng thêm chỉ số từ Trang bị hoặc Attr Point tại đây
        return baseHp + levelBonus;
    }

    /**
     * Tính toán tổng năng lượng (Mana/Stamina) tối đa.
     * Hiện tại đang lấy trực tiếp từ cấu hình của Class nhân vật.
     */
    public double getTotalMaxResource() {
        // Bạn có thể mở rộng logic cộng dồn theo Level tương tự như máu nếu cần thiết
        return this.currentClass.getMaxResource();
    }
}