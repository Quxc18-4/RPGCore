package vn.minecraft.rPGCore.managers;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.RPGCore;

import java.util.UUID;

public class CombatManager {

    /**
     * HÀM TÍNH TOÁN SÁT THƯƠNG GÂY RA (Có LUK tăng Chí Mạng)
     */
    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, boolean isMagic, boolean isCrossbow) {
        RPGPlayer.PlayerStats stats = rpgPlayer.getStats();
        UUID uuid = attacker.getUniqueId();

        double baseStat = isMagic ? (rpgPlayer.getAttributeData().getAttribute("INT") * 2.0) : (rpgPlayer.getAttributeData().getAttribute("STR") * 2.0);
        double minDmg = isMagic ? stats.minMagicalDamage : stats.minPhysicalDamage;
        double maxDmg = isMagic ? stats.maxMagicalDamage : stats.maxPhysicalDamage;

        if (maxDmg <= 0) { minDmg = 5.0; maxDmg = 10.0; }
        double weaponDmg = minDmg + (Math.random() * (maxDmg - minDmg));
        if (isCrossbow) weaponDmg *= 1.55;

        double rawDamage = baseStat + weaponDmg;

        double foodDmgBonus = RPGCore.getInstance().getFoodBuffManager().getBonus(uuid, FoodBuffManager.StatType.DMG);
        if (foodDmgBonus != 0) {
            rawDamage = rawDamage * (1.0 + (foodDmgBonus / 100.0));
        }

        // [TÍCH HỢP ATTRIBUTE]: LUK (Mỗi điểm LUK tăng 0.2% Tỉ lệ Chí Mạng)
        double lukBonusCrit = rpgPlayer.getAttributeData().getAttribute("LUK") * 0.2;
        boolean isCrit = (Math.random() * 100) <= (stats.critRate + lukBonusCrit);

        if (isCrit) {
            rawDamage = rawDamage * (1.0 + (stats.critDamage / 100.0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }

        double targetArmor = 0;
        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) targetArmor = armorAttr.getValue();

        double foodArmorPen = RPGCore.getInstance().getFoodBuffManager().getBonus(uuid, FoodBuffManager.StatType.ARMOR_PEN);
        double totalArmorPen = Math.min(100.0, stats.armorPen + extraArmorPen + foodArmorPen);
        targetArmor = targetArmor * (1.0 - (totalArmorPen / 100.0));

        double finalDamage = Math.max(1.0, (rawDamage - targetArmor) * dmgMultiplier);
        return finalDamage;
    }

    /**
     * HÀM MỚI: TÍNH TOÁN KHI BỊ QUÁI ĐÁNH (Né tránh & Đỡ đòn & AGI)
     */
    public static double calculateDamageTaken(Player victim, RPGPlayer rpgVictim, double incomingDamage) {
        RPGPlayer.PlayerStats stats = rpgVictim.getStats();

        // [TÍCH HỢP ATTRIBUTE]: AGI (Mỗi điểm AGI tăng 0.15% Tỉ lệ Né Tránh)
        double agiBonusDodge = rpgVictim.getAttributeData().getAttribute("AGI") * 0.15;
        double totalDodgeRate = Math.min(80.0, stats.dodgeRate + agiBonusDodge); // Giới hạn tối đa né 80%

        // 1. NÉ TRÁNH
        if (totalDodgeRate > 0 && (Math.random() * 100) <= totalDodgeRate) {
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            victim.playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
            // Gửi action bar thay vì chat để không trôi tin nhắn
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l[NÉ THÀNH CÔNG]"));
            return 0.0; // Miễn sát thương
        }

        double finalDamage = incomingDamage;

        // 2. ĐỠ ĐÒN
        if (stats.blockRate > 0 && (Math.random() * 100) <= stats.blockRate) {
            victim.getWorld().spawnParticle(Particle.ITEM, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.SHIELD));
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);

            finalDamage *= 0.5; // Đỡ được giảm 50% sát thương
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§9§l[ĐỠ ĐÒN: -50% SÁT THƯƠNG]"));
        }

        return finalDamage;
    }

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