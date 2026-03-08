package vn.minecraft.rPGCore.managers;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class CombatManager {

    /**
     * HÀM TÍNH TOÁN SÁT THƯƠNG TỔNG HỢP CHO MỌI CLASS
     * @param attacker Người tấn công
     * @param rpgPlayer Dữ liệu RPG của người tấn công
     * @param target Nạn nhân
     * @param dmgMultiplier Hệ số sát thương của Kỹ năng/Mũi tên (Mặc định là 1.0)
     * @param extraArmorPen Phần trăm xuyên giáp cộng thêm từ Kỹ năng
     * @param isMagic Đòn đánh này là Phép thuật hay Vật lý?
     * @param isCrossbow Vũ khí đang dùng có phải Nỏ không? (Để nhận bonus sát thương)
     * @return Sát thương cuối cùng sẽ gây ra
     */
    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, boolean isMagic, boolean isCrossbow) {
        RPGPlayer.PlayerStats stats = rpgPlayer.getStats();

        // 1. TÍNH BASE STATS (STR / INT)
        double baseStat = isMagic ? (rpgPlayer.getAttributeData().getAttribute("INT") * 2.0) : (rpgPlayer.getAttributeData().getAttribute("STR") * 2.0);

        // 2. LẤY SÁT THƯƠNG VŨ KHÍ (Min ~ Max)
        double minDmg = isMagic ? stats.minMagicalDamage : stats.minPhysicalDamage;
        double maxDmg = isMagic ? stats.maxMagicalDamage : stats.maxPhysicalDamage;

        if (maxDmg <= 0) { // Đánh tay không
            minDmg = 5.0; maxDmg = 10.0;
        }

        double weaponDmg = minDmg + (Math.random() * (maxDmg - minDmg));

        // Nỏ có thời gian nạp lâu hơn Cung nên sát thương gốc được x1.55
        if (isCrossbow) weaponDmg *= 1.55;

        double rawDamage = baseStat + weaponDmg;

        // 3. TÍNH TOÁN BẠO KÍCH (CRITICAL HIT)
        boolean isCrit = (Math.random() * 100) <= stats.critRate;
        if (isCrit) {
            rawDamage = rawDamage * (1.0 + (stats.critDamage / 100.0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }

        // 4. TÍNH TOÁN GIÁP NẠN NHÂN VÀ XUYÊN GIÁP
        double targetArmor = 0;
        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) targetArmor = armorAttr.getValue();

        // Tổng xuyên giáp = Xuyên từ đồ + Xuyên từ kỹ năng đặc biệt
        double totalArmorPen = Math.min(100.0, stats.armorPen + extraArmorPen);
        targetArmor = targetArmor * (1.0 - (totalArmorPen / 100.0));

        // 5. TÍNH SÁT THƯƠNG CUỐI CÙNG (Trừ giáp và nhân với hệ số Kỹ năng)
        double finalDamage = Math.max(1.0, (rawDamage - targetArmor) * dmgMultiplier);

        return finalDamage;
    }

    /**
     * HÀM ÁP DỤNG HÚT MÁU CHO MỌI CLASS
     */
    public static void applyLifeSteal(Player attacker, RPGPlayer rpgPlayer, double damageDealt, double extraLifeStealPct) {
        double totalLifeSteal = (rpgPlayer.getStats() != null ? rpgPlayer.getStats().lifeSteal : 0.0) + extraLifeStealPct;
        if (totalLifeSteal <= 0) return;

        double healAmount = damageDealt * (totalLifeSteal / 100.0);
        if (healAmount <= 0) return;

        double maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHp, attacker.getHealth() + healAmount));
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
    }
}