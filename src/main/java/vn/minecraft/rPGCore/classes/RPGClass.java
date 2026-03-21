package vn.minecraft.rPGCore.classes;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;

// Đã biến thành Abstract Class
public abstract class RPGClass {
    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> description;
    private final double maxHealth;
    private final double maxResource;
    private final ResourceType resourceType;
    private final double regenRate;
    private final boolean isEnchanted;

    public RPGClass(String id, String displayName, Material icon, List<String> description,
                    double maxHealth, double maxResource, ResourceType resourceType,
                    double regenRate, boolean isEnchanted) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.maxHealth = maxHealth;
        this.maxResource = maxResource;
        this.resourceType = resourceType;
        this.regenRate = regenRate;
        this.isEnchanted = isEnchanted;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public List<String> getDescription() { return description; }
    public double getMaxHealth() { return maxHealth; }
    public double getMaxResource() { return maxResource; }
    public ResourceType getResourceType() { return resourceType; }
    public double getRegenRate() { return regenRate; }
    public boolean isEnchanted() { return isEnchanted; }

    // Khai báo các mốc giới hạn (Cap) mặc định
    protected double critRateCap = 100.0;
    protected double armorPenCap = 50.0;
    protected double magicPenCap = 50.0;
    protected double cdrPenCap = 40.0;
    protected double dodgeCap = 30.0;
    protected double blockCap = 30.0;
    protected double lifeStealCap = 50.0;
    protected double effectResistCap = 80.0;
    protected double moveSpeedCap = 400.0;

    // ==========================================
    // CÁC HÀM SỰ KIỆN (HOOKS) CHO PHÉP OVERRIDE
    // ==========================================
    public void onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {}

    public void onRightClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {}

    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {
        return rawDamage;
    }

    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, LivingEntity attacker, double incomingDamage) {
        return incomingDamage;
    }

    // [THÊM MỚI]: Hàm dành riêng cho việc xử lý đạn/mũi tên chạm mục tiêu
    public void onProjectileHit(Player shooter, RPGPlayer rpgShooter, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) {}

    // ==========================================
    // HÀM 1: CỘNG CHỈ SỐ TĨNH (GỌI TỪ RPGPlayer)
    // Cho phép Class tự cộng thêm % nội tại vào Status của nhân vật
    // ==========================================
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, java.util.Map<String, Double> stats) {
        // Sau khi Class con cộng xong nội tại, hàm cha sẽ ép tất cả về đúng Cap
        applyOffensivePassives(stats);
    }

    // ==========================================
    // HÀM 2: MỞ GAP VÀ XỬ LÝ CHỈ SỐ ĐỘNG (GỌI TỪ CombatManager)
    // Khóa các chỉ số lại theo đúng Giới hạn (Cap) đặc trưng của Class
    // ==========================================
    // HÀM 2: THỰC THI "THIẾT QUÂN LUẬT" (CAPPING LOGIC)
    public void applyOffensivePassives(java.util.Map<String, Double> combatStats) {
        // Nhóm Xuyên thấu
        combatStats.put("Tỉ lệ xuyên giáp", Math.min(combatStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0), armorPenCap));
        combatStats.put("Tỉ lệ xuyên kháng phép", Math.min(combatStats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0), magicPenCap));

        // Nhóm Đa dụng & Sinh tồn (Chuyển từ RPGPlayer sang)
        combatStats.put("Giảm hồi chiêu", Math.min(combatStats.getOrDefault("Giảm hồi chiêu", 0.0), cdrPenCap));
        combatStats.put("Tỉ lệ né tránh", Math.min(combatStats.getOrDefault("Tỉ lệ né tránh", 0.0), dodgeCap));
        combatStats.put("Tỉ lệ đỡ đòn", Math.min(combatStats.getOrDefault("Tỉ lệ đỡ đòn", 0.0), blockCap));
        combatStats.put("Kháng hiệu ứng", Math.min(combatStats.getOrDefault("Kháng hiệu ứng", 0.0), effectResistCap));
        combatStats.put("Tỉ lệ hút máu", Math.min(combatStats.getOrDefault("Tỉ lệ hút máu", 0.0), lifeStealCap));
        combatStats.put("Tỉ lệ hút máu phép", Math.min(combatStats.getOrDefault("Tỉ lệ hút máu phép", 0.0), lifeStealCap));
        combatStats.put("Tốc độ đánh", Math.min(combatStats.getOrDefault("Tốc độ đánh", 0.0), 100.0));
        combatStats.put("Tốc độ di chuyển", Math.min(combatStats.getOrDefault("Tốc độ di chuyển", 0.0), moveSpeedCap));

        // Nhóm Chí mạng
        processCrit(combatStats, combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0));
    }

    protected void processCrit(java.util.Map<String, Double> combatStats, double critRate) {
        combatStats.put("Tỉ lệ chí mạng", Math.min(critRate, critRateCap));
    }
}