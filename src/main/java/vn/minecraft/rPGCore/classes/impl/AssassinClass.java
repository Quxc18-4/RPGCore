package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;

public class AssassinClass extends RPGClass {

    public AssassinClass(String id, String displayName, Material icon, List<String> description,
                         double maxHealth, double maxResource, ResourceType resourceType,
                         double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, java.util.Map<String, Double> stats) {
        // Nội tại Sát thủ: +15% Tỉ lệ chí mạng cơ bản
        double currentCrit = stats.getOrDefault("Tỉ lệ chí mạng", 5.0);
        stats.put("Tỉ lệ chí mạng", currentCrit + 15.0);
    }

    @Override
    protected void processCrit(java.util.Map<String, Double> combatStats, double critRate) {
        // LOGIC CHUYỂN HÓA DƯ THỪA CHUẨN MỰC
        if (critRate > 100.0) {
            double excess = critRate - 100.0;
            double currentCritDmg = combatStats.getOrDefault("Sát thương chí mạng", 50.0);

            combatStats.put("Sát thương chí mạng", currentCritDmg + (excess * 2.0));
            combatStats.put("Tỉ lệ chí mạng", 100.0); // Khóa lại ở 100%
        } else {
            combatStats.put("Tỉ lệ chí mạng", critRate);
        }
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {
        if (attacker.hasMetadata("Assassin_Stealth")) {
            attacker.removeMetadata("Assassin_Stealth", RPGCore.getInstance());
            attacker.removeMetadata("Assassin_Smoke_Bomb_Active", RPGCore.getInstance());
            attacker.sendMessage("§c§l[!] Đòn đánh bất ngờ! Hiện hình!");

            // Gắn cờ để CombatManager tự bù % Crit cho đủ 100%
            attacker.setMetadata("Basic_Stealth_Attack", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(RPGCore.getInstance(), attacker);
            attacker.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }
        return rawDamage; // Trả về rawDamage vì Crit đã x2 Dame sẵn rồi
    }
}