package vn.minecraft.rPGCore.classes.impl.bard;

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

import java.util.List;
import java.util.Map;

public class BardClass extends RPGClass {

    // Hằng số Metadata Keys dựa theo GDD
    public static final String META_VALOR_BUFF = "Bard_Valor_Buff";
    public static final String META_AEGIS_BUFF = "Bard_Aegis_Buff";
    public static final String META_GLOOM_DEBUFF = "Bard_Gloom_Debuff";
    public static final String META_DISSONANCE_DEBUFF = "Bard_Dissonance_Debuff";
    public static final String META_SIPHON_MARK = "Bard_Siphon_Mark";

    // Quản lý Cooldown hồi máu của Hút Hồn để chống lạm dụng (Spam)
    private final Map<java.util.UUID, Long> siphonHealCooldowns = new java.util.HashMap<>();

    public BardClass(String id, String displayName, Material icon, List<String> description,
                     double maxHealth, double maxResource, ResourceType resourceType,
                     double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);

        // ==========================================
        // THIẾT LẬP GIỚI HẠN (CAP) THEO GDD
        // ==========================================
        this.moveSpeedCap = 600.0; // Mở khóa Tốc độ di chuyển lên 600% (Tiến trình vi mô)
        this.effectResistCap = 100.0; // Đạt max 100% Kháng hiệu ứng

        // ==========================================
        // HOOK 1: GIAI ĐIỆU DŨNG CẢM (VALOR)
        // +10% Sát thương đầu ra cho đồng minh
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata(META_VALOR_BUFF)) {
                    return 0.10;
                }
                return 0.0;
            }
        });

        // ==========================================
        // HOOK 2: GIAI ĐIỆU CAN TRƯỜNG (AEGIS)
        // -10% Sát thương nhận vào cho đồng minh
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_AEGIS_BUFF)) {
                    return 0.10; // Giảm 10%
                }
                return 0.0;
            }
        });

        // ==========================================
        // HOOK 3: GIAI ĐIỆU U ÁM (GLOOM) & LÕI 6 (DISSONANCE)
        // U Ám: Quái vật bị giảm 10% sát thương gây ra.
        // Nghịch Âm: Quái vật chịu thêm 15% sát thương (Mô phỏng -15% Giáp/Kháng phép)
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata(META_GLOOM_DEBUFF)) {
                    return -0.10; // Quái vật đánh yếu đi 10%
                }
                return 0.0;
            }

            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_DISSONANCE_DEBUFF)) {
                    return -0.15; // Trả về ÂM -> Nạn nhân nhận THÊM 15% sát thương
                }
                return 0.0;
            }
        });
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        Player player = rpgPlayer.getBukkitPlayer();

        // 1. TIẾN TRÌNH VI MÔ: Bơm % Giảm hồi chiêu (CDR)
        if (player.hasMetadata("BARD_BONUS_CDR")) {
            double bonusCdr = player.getMetadata("BARD_BONUS_CDR").get(0).asDouble();
            stats.put("Giảm hồi chiêu", stats.getOrDefault("Giảm hồi chiêu", 0.0) + bonusCdr);
        }

        // 2. LÕI 5: BỘI ÂM (OVERTONE) - Cấp MS và Kháng Hiệu Ứng động theo số Âm đang giữ
        if (player.hasMetadata("BARD_OVERTONE_ACTIVE") && player.hasMetadata("BARD_TONE_COUNT")) {
            int toneCount = player.getMetadata("BARD_TONE_COUNT").get(0).asInt();
            if (toneCount > 0) {
                stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (20.0 * toneCount));
                stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + (15.0 * toneCount));
            }
        }

        // 3. ÂM SẮC (SHARP TONE) - Dạng Buff (Kháng CC)
        if (player.hasMetadata("BARD_SHARP_BUFF")) {
            stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + 10.0);
        }

        // 4. ÂM SẮC (SHARP TONE) - Dạng Tấn Công (Xuyên Thấu)
        if (player.hasMetadata("BARD_SHARP_ATTACK")) {
            stats.put("Tỉ lệ xuyên giáp", stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + 10.0);
            stats.put("Tỉ lệ xuyên kháng phép", stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + 10.0);
        }

        // 5. GIAI ĐIỆU DỒN DẬP (UPBEAT) - Bơm MS và Attack Speed
        if (player.hasMetadata("BARD_UPBEAT_BUFF")) {
            stats.put("Tốc độ đánh", stats.getOrDefault("Tốc độ đánh", 0.0) + 15.0);
            stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + 30.0);
        }

        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // ==========================================
    // HOOK SỰ KIỆN TẤN CÔNG (ON ATTACK LANDED)
    // Xử lý Giai điệu Hút Hồn (Siphon) - Hồi máu trên đòn đánh
    // ==========================================
    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        if (target.hasMetadata(META_SIPHON_MARK)) {
            long currentTime = System.currentTimeMillis();
            long lastHealTime = siphonHealCooldowns.getOrDefault(attacker.getUniqueId(), 0L);

            // Tối đa hồi 5 lần/giây (Tương đương Delay 200ms) để chống lạm dụng
            if (currentTime - lastHealTime >= 200) {
                RPGPlayer rpgAttacker = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
                if (rpgAttacker != null) {
                    // Sử dụng hàm an toàn từ RPGPlayer
                    double maxHp = rpgAttacker.getTotalMaxHealth();
                    double healAmount = maxHp * 0.005; // 0.5% Max HP

                    // Gọi thông qua RPGPlayer.heal() để áp dụng màng lọc Anti-Heal
                    rpgAttacker.heal(healAmount);

                    // Chỉ hiển thị Particle khi đòn đánh trúng, không cần biết có bị chặn hồi máu hay không (để giữ feedback hình ảnh)
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.HEART, attacker.getLocation().add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);

                    siphonHealCooldowns.put(attacker.getUniqueId(), currentTime);
                }
            }
        }
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        // Bard có thể chém tay hoặc gõ bằng nhạc cụ, nên mặc định trả về true
        return true;
    }

    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // Có thể áp dụng đánh xa như Cung thủ hoặc nốt nhạc cơ bản nếu cần
        return false;
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // Dành cho Ulti hoặc kích hoạt Menu Đổi Lõi Nâng Cấp thủ công
        return false;
    }
}