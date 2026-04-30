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

public class GaleDragFusion implements IFusion {
    @Override public MageElement getElement1() { return MageElement.WIND; }
    @Override public MageElement getElement2() { return MageElement.WIND; }
    @Override public String getReactionName() { return "§a§lCUỒNG PHONG TÁP"; }
    @Override public double getDamageMultiplier() { return 1.25; }

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        Vector travelDir = direction.clone().normalize().multiply(0.8); // Bay chậm lại để hút
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        new BukkitRunnable() {
            Location currentLoc = hitLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 20 || currentLoc.getBlock().getType().isSolid()) {
                    // NỔ TẠI ĐIỂM CUỐI
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.5f);
                    currentLoc.getWorld().spawnParticle(Particle.EXPLOSION, currentLoc, 2);
                    currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 40, 3.0, 3.0, 3.0, 0.2);

                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 4.0, 4.0, 4.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) entity, finalDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) entity, hitDmg);
                        }
                    }
                    this.cancel();
                    return;
                }

                currentLoc.add(travelDir);
                currentLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentLoc, 2, 1.5, 1.5, 1.5, 0);
                if (ticks % 5 == 0) currentLoc.getWorld().playSound(currentLoc, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.5f);

                // Hút/Kéo rê kẻ địch theo hướng lốc xoáy
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 3.5, 3.5, 3.5)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        entity.setVelocity(travelDir.clone().multiply(1.2).setY(0.2));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}