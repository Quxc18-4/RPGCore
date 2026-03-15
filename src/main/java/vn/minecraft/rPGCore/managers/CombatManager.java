package vn.minecraft.rPGCore.managers;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Map;

public class CombatManager {

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, boolean isMagic, boolean isCrossbow) {
        return calculateDamage(attacker, rpgPlayer, target, dmgMultiplier, extraArmorPen, 0.0, 0.0, isMagic, isCrossbow);
    }

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, double extraCritRate, boolean isMagic, boolean isCrossbow) {
        return calculateDamage(attacker, rpgPlayer, target, dmgMultiplier, extraArmorPen, extraCritRate, 0.0, isMagic, isCrossbow);
    }

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, double extraCritRate, double extraCritDamage, boolean isMagic, boolean isCrossbow) {
        Map<String, Double> totalStats = rpgPlayer.getTotalStatsMap();

        double minDmg = totalStats.getOrDefault(isMagic ? "Sát thương phép thuật (Nhỏ nhất)" : "Sát thương vật lý (Nhỏ nhất)", 5.0);
        double maxDmg = totalStats.getOrDefault(isMagic ? "Sát thương phép thuật (Lớn nhất)" : "Sát thương vật lý (Lớn nhất)", 10.0);

        double rawDamage = minDmg + (Math.random() * (maxDmg - minDmg));
        if (isCrossbow) rawDamage *= 1.55;

        double critRate = totalStats.getOrDefault("Tỉ lệ chí mạng", 5.0) + extraCritRate;
        double critDamage = totalStats.getOrDefault("Sát thương chí mạng", 50.0) + extraCritDamage;

        if ((Math.random() * 100) <= critRate) {
            rawDamage = rawDamage * (1.0 + (critDamage / 100.0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }

        double targetDefense = 0;
        if (isMagic && target instanceof Player) {
            RPGPlayer targetRpg = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
            if (targetRpg != null) targetDefense = targetRpg.getTotalStatsMap().getOrDefault("Kháng phép", 0.0);
        } else if (!isMagic) {
            AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
            if (armorAttr != null) targetDefense = armorAttr.getValue();
        }

        double statPen = totalStats.getOrDefault(isMagic ? "Tỉ lệ xuyên kháng phép" : "Tỉ lệ xuyên giáp", 0.0);
        statPen = Math.min(100.0, Math.max(0.0, statPen));
        double skillPen = Math.min(100.0, Math.max(0.0, extraArmorPen));

        double finalDefenseMulti = (1.0 - (statPen / 100.0)) * (1.0 - (skillPen / 100.0));
        targetDefense = targetDefense * finalDefenseMulti;

        double finalDamage = Math.max(1.0, (rawDamage - targetDefense) * dmgMultiplier);

        // ===========================================
        // [MỚI]: HỆ THỐNG MÁY QUÉT DUMMY TEST SÁT THƯƠNG
        // ===========================================
        if (target.getCustomName() != null && ChatColor.stripColor(target.getCustomName()).contains("Dummy")) {
            attacker.sendMessage("§e§l[DUMMY] §fSát thương nhận vào: §c" + String.format("%.1f", finalDamage) + " ⚔");

            // Hồi đầy máu và dập tắt lửa để Dummy không bao giờ chết
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isValid() && !target.isDead()) {
                        AttributeInstance maxHp = target.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHp != null) target.setHealth(maxHp.getValue());
                        target.setFireTicks(0);
                    }
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);
        }

        return finalDamage;
    }

    public static double calculateDamageTaken(Player victim, RPGPlayer rpgVictim, double incomingDamage) {
        Map<String, Double> totalStats = rpgVictim.getTotalStatsMap();

        double dodgeRate = totalStats.getOrDefault("Tỉ lệ né tránh", 0.0);
        if (dodgeRate > 0 && (Math.random() * 100) <= dodgeRate) {
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            victim.playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a§l[NÉ THÀNH CÔNG]"));
            return 0.0;
        }

        double finalDamage = incomingDamage;

        double blockRate = totalStats.getOrDefault("Tỉ lệ đỡ đòn", 0.0);
        if (blockRate > 0 && (Math.random() * 100) <= blockRate) {
            victim.getWorld().spawnParticle(Particle.ITEM, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.SHIELD));
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            finalDamage *= 0.5;
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§9§l[ĐỠ ĐÒN: -50% SÁT THƯƠNG]"));
        }

        return finalDamage;
    }

    public static void applyLifeSteal(Player attacker, RPGPlayer rpgPlayer, double damageDealt, double extraLifeStealPct, boolean isMagic) {
        if (attacker.hasMetadata("Assassin_AntiHeal")) {
            long expire = attacker.getMetadata("Assassin_AntiHeal").get(0).asLong();
            if (System.currentTimeMillis() < expire) return;
        }

        Map<String, Double> totalStats = rpgPlayer.getTotalStatsMap();
        String stealType = isMagic ? "Tỉ lệ hút máu phép" : "Tỉ lệ hút máu";
        double totalLifeSteal = totalStats.getOrDefault(stealType, 0.0) + extraLifeStealPct;

        if (totalLifeSteal <= 0) return;

        double healAmount = damageDealt * (totalLifeSteal / 100.0);
        if (healAmount <= 0) return;

        double maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHp, attacker.getHealth() + healAmount));

        Particle pType = isMagic ? Particle.SOUL : Particle.HEART;
        attacker.getWorld().spawnParticle(pType, attacker.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
    }

    // ==========================================
    // HỆ THỐNG GÂY SÁT THƯƠNG KỸ NĂNG (DÙNG CHUNG CHO MỌI CLASS)
    // ==========================================
    public static void applySkillDamage(Player attacker, LivingEntity target, double finalDamage) {
        // 1. Xóa I-Frame (Giây bất tử của Vanilla) để chiêu thức chạm là ăn sát thương
        target.setNoDamageTicks(0);

        // 2. Cắm cờ Khóa Sát Thương để PlayerListener không tính lặp thành đòn đánh tay
        attacker.setMetadata("Skill_Damage_Lock", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), true));

        // 3. Gây sát thương (Ghi nhận người ra đòn để quái dí theo và tính Kill)
        target.damage(finalDamage, attacker);

        // 4. Rút cờ Khóa Sát Thương ra
        attacker.removeMetadata("Skill_Damage_Lock", vn.minecraft.rPGCore.RPGCore.getInstance());
    }
}