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

    // Thêm 3 dòng này vào phần khai báo biến (Dưới biến isEnchanted)
    protected double critRateCap = 100.0;
    protected double armorPenCap = 50.0;
    protected double magicPenCap = 50.0;

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
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, java.util.Map<String, Double> stats) {}

    // ==========================================
    // HÀM 2: MỞ GAP VÀ XỬ LÝ CHỈ SỐ ĐỘNG (GỌI TỪ CombatManager)
    // Khóa các chỉ số lại theo đúng Giới hạn (Cap) đặc trưng của Class
    // ==========================================
    public void applyOffensivePassives(java.util.Map<String, Double> combatStats) {
        double armorPen = combatStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0);
        double magicPen = combatStats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);

        // Ép giới hạn theo Cap của từng phái
        combatStats.put("Tỉ lệ xuyên giáp", Math.min(armorPen, armorPenCap));
        combatStats.put("Tỉ lệ xuyên kháng phép", Math.min(magicPen, magicPenCap));

        // Đẩy Crit cho hàm processCrit tự xử lý
        processCrit(combatStats, combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0));
    }

    // Hàm con cho phép Sát Thủ ghi đè để bắt lấy phần Crit dư thừa
    protected void processCrit(java.util.Map<String, Double> combatStats, double critRate) {
        combatStats.put("Tỉ lệ chí mạng", Math.min(critRate, critRateCap));
    }
}