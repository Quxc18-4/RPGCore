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
    protected double dodgeCap = 20.0;
    protected double blockCap = 20.0;
    protected double lifeStealCap = 50.0;
    protected double effectResistCap = 80.0;
    protected double moveSpeedCap = 400.0;
    // [THÊM MỚI]: Cap hồi năng lượng mặc định cho tất cả các phái là 400%
    protected double resourceRegenCap = 400.0;
    protected double costReductionCap = 33.0; // [THÊM MỚI]: Giới hạn Giảm tiêu hao NL tối đa 33%

    // ==========================================
    // CÁC HÀM SỰ KIỆN (HOOKS) CHO PHÉP OVERRIDE
    // ==========================================
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) { return false; }

    public boolean onRightClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) { return false; }

    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {
        double multiplier = rawDamage; // Đầu vào thường là 1.0

        // Chạy qua tất cả các Buff của Server đang cắm ở CombatManager
        for (vn.minecraft.rPGCore.managers.CombatManager.ICombatModifier mod : vn.minecraft.rPGCore.managers.CombatManager.combatModifiers) {
            multiplier += mod.getOutgoingDamageModifier(attacker, target);
        }

        return Math.max(0.1, multiplier);
    }

    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, LivingEntity attacker, double incomingDamage) {
        double damage = incomingDamage;

        // 1. KIỂM TRA PVP: Nếu người đánh là Player hoặc Pet -> Bỏ qua vì calculateDamage của PvP đã trừ giáp rồi
        boolean isPvP = (attacker instanceof Player) || (attacker != null && attacker.hasMetadata("Summoner_Pet"));

        // 2. NẾU KHÔNG PHẢI PVP (Quái thường, Skill MythicMobs, Mechanic AOE không có attacker...) -> BẮT BUỘC TRỪ GIÁP
        if (!isPvP) {
            boolean isMagicAttack = (attacker != null && attacker.hasMetadata("Magic_Attack"));
            double playerDefense = isMagicAttack ? rpgVictim.getTotalStatsMap().getOrDefault("Kháng phép", 0.0)
                    : rpgVictim.getTotalStatsMap().getOrDefault("Giáp", 0.0);

            // Trừ thẳng Giáp/Kháng phép
            damage = Math.max(1.0, incomingDamage - playerDefense);
        }

        // 3. MÀNG LỌC % MIỄN THƯƠNG (Combat Modifiers)
        for (vn.minecraft.rPGCore.managers.CombatManager.ICombatModifier mod : vn.minecraft.rPGCore.managers.CombatManager.combatModifiers) {
            double reduction = mod.getIncomingDamageReduction(victim, attacker);
            damage *= (1.0 - reduction);
        }

        return Math.max(0.0, damage);
    }

    // ==========================================
    // PHỄU LỌC TRUNG TÂM (TEMPLATE METHOD) - CHUẨN SOLID
    // Tự động kiểm tra điều kiện và trừ độ bền nếu tung chiêu thành công
    // ==========================================
    public final void executeLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        vn.minecraft.rPGCore.listeners.EquipmentListener eqListener = new vn.minecraft.rPGCore.listeners.EquipmentListener(vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager());
        if (!eqListener.canPlayerUse(player, weapon)) return; // Chặn vũ khí vượt cấp

        boolean success = this.onLeftClick(player, rpgPlayer, weapon);

        // Chỉ trừ độ bền nếu Class báo cáo tung chiêu/trúng đích thành công
        if (success) {
            vn.minecraft.rPGCore.managers.DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
        }
    }

    public final void executeRightClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        vn.minecraft.rPGCore.listeners.EquipmentListener eqListener = new vn.minecraft.rPGCore.listeners.EquipmentListener(vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager());
        if (!eqListener.canPlayerUse(player, weapon)) return;

        boolean success = this.onRightClick(player, rpgPlayer, weapon);
        if (success) {
            vn.minecraft.rPGCore.managers.DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
        }
    }

    // [THÊM MỚI]: Hàm dành riêng cho việc xử lý đạn/mũi tên chạm mục tiêu
    public void onProjectileHit(Player shooter, RPGPlayer rpgShooter, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) {}

    // ==========================================
    // HÀM 1: CỘNG CHỈ SỐ TĨNH (GỌI TỪ RPGPlayer)
    // Cho phép Class tự cộng thêm % nội tại vào Status của nhân vật
    // ==========================================
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, java.util.Map<String, Double> stats) {
        // Sau khi Class con cộng xong nội tại, hàm cha sẽ ép tất cả về đúng Cap
        for (vn.minecraft.rPGCore.managers.CombatManager.IStatModifier mod : vn.minecraft.rPGCore.managers.CombatManager.statModifiers) {
            mod.applyBonusStats(rpgPlayer.getBukkitPlayer(), stats);
        }
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
        // Khóa Cap Hồi Năng Lượng
        combatStats.put("Hồi Năng lượng", Math.min(combatStats.getOrDefault("Hồi Năng lượng", 0.0), resourceRegenCap));
        combatStats.put("Giảm tiêu hao Năng lượng", Math.min(combatStats.getOrDefault("Giảm tiêu hao Năng lượng", 0.0), costReductionCap));
        // Nhóm Chí mạng
        processCrit(combatStats, combatStats.getOrDefault("Tỉ lệ chí mạng", 5.0));
    }

    protected void processCrit(java.util.Map<String, Double> combatStats, double critRate) {
        combatStats.put("Tỉ lệ chí mạng", Math.min(critRate, critRateCap));
    }

    // ==========================================
    // HÀM KIỂM SOÁT ĐÁNH CẬN CHIẾN (MELEE CONTROL)
    // Trả về TRUE: Cho phép chém tay (Warrior, Assassin...)
    // Trả về FALSE: Hủy đòn chém tay (Archer, Mage, Summoner...)
    // ==========================================
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        return true; // Mặc định các hệ phái đều được đánh cận chiến
    }

    // ==========================================
    // CÁC HÀM HOOK DÀNH CHO COMBAT MANAGER ĐIỀU PHỐI
    // ==========================================

    /**
     * Hook 1: Báo cáo đòn tấn công đã trúng đích (Dùng để tích nội tại như Sát Ý, Stack đòn đánh...)
     */
    public void onAttackLanded(Player attacker, LivingEntity target) { }

    /**
     * Hook 2: Sửa đổi chỉ số linh hoạt ngay trước khi nổ sát thương (Dành cho Đâm lén, Tàng hình...)
     */
    public void applyDynamicCombatStats(Player attacker, LivingEntity target, java.util.Map<String, Double> combatStats) { }

    /**
     * Hook 3: Trả về lượng % Hút máu cộng thêm linh hoạt (Dành cho các chiêu buff tạm thời như Khát Máu)
     */
    public double getDynamicLifeSteal(Player attacker, RPGPlayer rpgPlayer) {
        return 0.0;
    }

    /**
     * Hook 4: Truyền chỉ số động cho Đệ tử (Dành cho Summoner)
     */
    public void applyPetDynamicStats(Player owner, RPGPlayer rpgOwner, org.bukkit.entity.Entity pet, java.util.Map<String, Double> petStats) { }

    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) { return false; }

    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, double incomingDamage) {
        return incomingDamage; // Mặc định trả về nguyên vẹn, các class con như Warrior sẽ override lại để chia đôi (Parry)
    }
}