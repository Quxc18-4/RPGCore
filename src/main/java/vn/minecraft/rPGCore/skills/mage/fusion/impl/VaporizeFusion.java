package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class VaporizeFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.WATER; }
    @Override public String getReactionName() { return "§c§lBỐC HƠI"; }
    @Override public double getDamageMultiplier() { return 1.50; } // Khuếch đại 150%[cite: 25]

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.5f, 0.5f);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.8f);

        Vector normDir = direction.clone().normalize();
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        // [XỬ LÝ HITBOX HÌNH NÓN (CONE)]
        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 8.0, 5.0, 8.0)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                // Toán học Vector: Góc 45 độ (Tổng góc quét 90 độ)
                Vector toTarget = target.getLocation().toVector().subtract(hitLoc.toVector()).normalize();
                double angle = Math.toDegrees(normDir.angle(toTarget));

                if (angle <= 45.0) {
                    double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                    CombatManager.applySkillDamage(caster, target, hitDmg);
                }
            }
        }

        // [VFX]: Làn khí nóng lan tỏa[cite: 25]
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step > 6) { this.cancel(); return; }

                Location spread = hitLoc.clone().add(normDir.clone().multiply(step * 1.5));
                double r = 1.5 + (step * 0.8);

                spread.getWorld().spawnParticle(Particle.CLOUD, spread, 30 * (step+1), r, r*0.5, r, 0.05);
                spread.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, spread, 15 * (step+1), r, r*0.5, r, 0.02);

                step++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}