package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.ElementalSwapSkill;
import vn.minecraft.rPGCore.classes.impl.mage.attacks.*;
import vn.minecraft.rPGCore.skills.mage.fusion.impl.HolyBladesFusion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MageClass extends RPGClass {

    private final Map<java.util.UUID, Long> nextAttackTime = new java.util.HashMap<>();
    private final Map<MageElement, IMageBasicAttack> attackStrategies = new EnumMap<>(MageElement.class);

    public MageClass(String id, String displayName, Material icon, List<String> description,
                     double maxHealth, double maxResource, ResourceType resourceType,
                     double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
        this.magicPenCap = 80.0;

        // Đăng ký 6 Chiến lược (Strategy) cho đòn đánh thường
        attackStrategies.put(MageElement.LIGHT, new LightAttack());
        attackStrategies.put(MageElement.FIRE, new FireAttack());
        attackStrategies.put(MageElement.WATER, new WaterAttack());
        attackStrategies.put(MageElement.WIND, new WindAttack());
        attackStrategies.put(MageElement.NATURE, new NatureAttack());
        attackStrategies.put(MageElement.DARK, new DarkAttack());

        // [HOOK ÁNH SÁNG]: +15% DMG xuyên mục tiêu
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (target.hasMetadata("Mage_Exposure_Debuff")) {
                    target.removeMetadata("Mage_Exposure_Debuff", RPGCore.getInstance());
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
                    target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                    return 0.15; // +15% DMG
                }
                return 0.0;
            }
        });

        // [HOOK ÁNH SÁNG]: Giảm 8% sát thương nhận vào khi đứng trong Thánh Vực (Cho cả Bản thân và Đồng minh)
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata("Light_Domain_Protection")) {
                    // [FIX LỖI]: Bọc thép kiểm tra danh sách rỗng
                    java.util.List<org.bukkit.metadata.MetadataValue> meta = victim.getMetadata("Light_Domain_Protection");
                    if (!meta.isEmpty()) {
                        long expire = meta.get(0).asLong();
                        if (System.currentTimeMillis() < expire) return 0.08; // Giảm 8%
                    }
                }
                return 0.0;
            }
        });
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        double currentMagicPen = stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        stats.put("Tỉ lệ xuyên kháng phép", currentMagicPen + 25.0);

        // ==========================================
        // [BẠN THÊM ĐOẠN NÀY VÀO]: ÉP TỈ LỆ TỐC ĐỘ TỪ KỸ NĂNG GIÓ
        // ==========================================
        Player player = rpgPlayer.getBukkitPlayer();
        if (player.hasMetadata("Wind_Burst_Speed")) {
            double extraSpeed = player.getMetadata("Wind_Burst_Speed").get(0).asDouble();
            // Hàm Cha RPGClass sẽ tự động khóa giới hạn (Cap) tốc độ sau bước này
            stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + extraSpeed);
        }

        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        attacker.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✖ Pháp Sư không thể đánh cận chiến!"));
        return false;
    }

    @Override
    public void applyPetDynamicStats(Player owner, RPGPlayer rpgOwner, org.bukkit.entity.Entity pet, Map<String, Double> petStats) {
        double ownerMagicPen = rpgOwner.getTotalStatsMap().getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        petStats.put("ArmorPen", petStats.getOrDefault("ArmorPen", 0.0) + (ownerMagicPen * 0.3));
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        vn.minecraft.rPGCore.skills.ISkill swapSkill = RPGCore.getInstance().getSkillManager().getSkill("mage_elemental_swap");
        if (swapSkill != null) {
            return swapSkill.execute(player, rpgPlayer, 1);
        }
        return false;
    }

    // ==========================================
    // SIÊU HÀM: ĐÒN ĐÁNH THƯỜNG (ÁP DỤNG STRATEGY PATTERN)
    // ==========================================
    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        if (player.hasMetadata("Mage_Domain_Channeling")) {
            return false; // Thoát hàm ngay lập tức, chặn không cho gọi Strategy
        }
        long currentTime = System.currentTimeMillis();
        long allowedTime = nextAttackTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return false;

        double totalAtkSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long attackCooldown = (long) (1000 / (1.0 + (totalAtkSpeedBonus / 100.0)));
        attackCooldown = Math.max(300, attackCooldown);
        nextAttackTime.put(player.getUniqueId(), currentTime + attackCooldown);

        MageElement element = ElementalSwapSkill.getActiveElement(player);

        // ==========================================
        // [HOOK ÁNH SÁNG]: Khai hỏa Thánh Kiếm nếu đang có đội hình
        // ==========================================
        HolyBladesFusion.tryFireBlade(player, rpgPlayer);

        // Gọi Strategy tương ứng để xử lý
        IMageBasicAttack attackStrategy = attackStrategies.get(element);
        if (attackStrategy != null) {
            attackStrategy.cast(player, rpgPlayer, weapon);
        }

        return false; // Trả về false để chặn RPGClass trừ độ bền Vanilla
    }
}