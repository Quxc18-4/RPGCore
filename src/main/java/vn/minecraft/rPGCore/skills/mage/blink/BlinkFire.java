package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class BlinkFire implements IBlink {
    @Override
    public void executeBlink(Player player, RPGPlayer rpgPlayer, int skillLevel) {
        Location start = player.getLocation();
        // [ĐÃ SỬA]: Lướt thẳng về phía trước song song với mặt đất
        double yaw = Math.toRadians(start.getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));

        Location targetLoc = start.clone();
        for (double d = 0; d <= 6.0; d += 0.5) {
            Location checkLoc = targetLoc.clone().add(dir.clone().multiply(0.5));
            if (checkLoc.getBlock().getType().isSolid() || checkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                break;
            }
            targetLoc = checkLoc;
        }

        targetLoc.setPitch(start.getPitch());
        targetLoc.setYaw(start.getYaw());

        targetLoc.setPitch(start.getPitch());
        targetLoc.setYaw(start.getYaw());

        start.getWorld().spawnParticle(Particle.FLAME, start.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Chống sát thương ngã nếu lướt lên không trung
        player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(RPGCore.getInstance(), true));
        player.teleport(targetLoc);

        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc.add(0, 1, 0), 1);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 20, 1.5, 1.0, 1.5, 0.1);

        double damageMulti = 1.5 + (skillLevel * 0.3);

        for (org.bukkit.entity.Entity ent : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.0, 3.0, 3.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, damageMulti, 0.0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
            }
        }
    }
}