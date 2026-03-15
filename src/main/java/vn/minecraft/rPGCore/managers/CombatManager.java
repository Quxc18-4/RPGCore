package vn.minecraft.rPGCore.managers;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Map;

public class CombatManager {

    public static double calculateDamage(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double dmgMultiplier, double extraArmorPen, boolean isMagic, boolean isCrossbow) {
        Map<String, Double> totalStats = rpgPlayer.getTotalStatsMap();

        double minDmg = totalStats.getOrDefault(isMagic ? "Sát thương phép thuật (Nhỏ nhất)" : "Sát thương vật lý (Nhỏ nhất)", 5.0);
        double maxDmg = totalStats.getOrDefault(isMagic ? "Sát thương phép thuật (Lớn nhất)" : "Sát thương vật lý (Lớn nhất)", 10.0);

        double rawDamage = minDmg + (Math.random() * (maxDmg - minDmg));
        if (isCrossbow) rawDamage *= 1.55;

        // BẠO KÍCH
        double critRate = totalStats.getOrDefault("Tỉ lệ chí mạng", 5.0);
        double critDamage = totalStats.getOrDefault("Sát thương chí mạng", 50.0);

        if ((Math.random() * 100) <= critRate) {
            rawDamage = rawDamage * (1.0 + (critDamage / 100.0));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }

        // XUYÊN GIÁP & XUYÊN KHÁNG PHÉP
        double targetDefense = 0;

        if (isMagic) {
            // Nếu đánh phép -> Check Kháng phép của nạn nhân (nếu nạn nhân là người chơi)
            // Quái Vanilla mặc định không có Kháng Phép trừ khi bạn dùng MythicMobs cấu hình
            targetDefense = 0.0; // Mặc định với quái
            if (target instanceof Player) {
                RPGPlayer targetRpg = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
                if (targetRpg != null) {
                    targetDefense = targetRpg.getTotalStatsMap().getOrDefault("Kháng phép", 0.0);
                }
            }
        } else {
            // Nếu đánh vật lý -> Check Giáp
            AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
            if (armorAttr != null) targetDefense = armorAttr.getValue();
        }

        // Kẻ địch bị trừ phòng thủ tùy theo loại sát thương
        double totalPen = totalStats.getOrDefault(isMagic ? "Tỉ lệ xuyên kháng phép" : "Tỉ lệ xuyên giáp", 0.0) + extraArmorPen;
        totalPen = Math.min(100.0, totalPen);
        targetDefense = targetDefense * (1.0 - (totalPen / 100.0));

        return Math.max(1.0, (rawDamage - targetDefense) * dmgMultiplier);
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

    // ĐÃ BỔ SUNG isMagic ĐỂ CHIA HÚT MÁU
    public static void applyLifeSteal(Player attacker, RPGPlayer rpgPlayer, double damageDealt, double extraLifeStealPct, boolean isMagic) {
        Map<String, Double> totalStats = rpgPlayer.getTotalStatsMap();

        String stealType = isMagic ? "Tỉ lệ hút máu phép" : "Tỉ lệ hút máu";
        double totalLifeSteal = totalStats.getOrDefault(stealType, 0.0) + extraLifeStealPct;

        if (totalLifeSteal <= 0) return;

        double healAmount = damageDealt * (totalLifeSteal / 100.0);
        if (healAmount <= 0) return;

        double maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHp, attacker.getHealth() + healAmount));

        // Hút máu vật lý ra tim đỏ, hút máu phép ra tim xanh
        Particle pType = isMagic ? Particle.SOUL : Particle.HEART;
        attacker.getWorld().spawnParticle(pType, attacker.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
    }
}