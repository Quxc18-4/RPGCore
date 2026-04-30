package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
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

public class PyroclasmFusion implements IFusion {
    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.FIRE; }
    @Override public String getReactionName() { return "§c§lĐẠI HỎA CẦU"; }
    @Override public double getDamageMultiplier() { return 1.40; } // Thuần Sát thương

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        Location skyLoc = hitLoc.clone().add(0, 15, 0);
        Vector fallDir = new Vector(0, -1.5, 0);

        caster.getWorld().playSound(hitLoc, Sound.ENTITY_GHAST_WARN, 1.5f, 0.5f);

        // Vẽ vòng cảnh báo
        Particle.DustOptions warningDust = new Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
        for(double t = 0; t < Math.PI * 2; t+= 0.2) {
            hitLoc.getWorld().spawnParticle(Particle.DUST, hitLoc.clone().add(5*Math.cos(t), 0.2, 5*Math.sin(t)), 1, 0,0,0, 0, warningDust, true);
        }

        new BukkitRunnable() {
            Location currentMeteor = skyLoc.clone();
            @Override
            public void run() {
                if (currentMeteor.getY() <= hitLoc.getY() || currentMeteor.getBlock().getType().isSolid()) {
                    // NỔ THIÊN THẠCH
                    currentMeteor.getWorld().playSound(currentMeteor, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.5f);
                    currentMeteor.getWorld().spawnParticle(Particle.EXPLOSION, currentMeteor, 2);
                    currentMeteor.getWorld().spawnParticle(Particle.LAVA, currentMeteor, 50, 3.0, 3.0, 3.0, 0.1);

                    double finalDmgMulti = baseMultiplier * getDamageMultiplier();
                    for (Entity entity : currentMeteor.getWorld().getNearbyEntities(currentMeteor, 5.0, 5.0, 5.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) entity, finalDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) entity, hitDmg);
                        }
                    }
                    this.cancel();
                    return;
                }

                // Rơi
                currentMeteor.add(fallDir);
                currentMeteor.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentMeteor, 10, 0.5, 0.5, 0.5, 0.05);
                currentMeteor.getWorld().spawnParticle(Particle.FLAME, currentMeteor, 20, 1.0, 1.0, 1.0, 0.05);
                currentMeteor.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, currentMeteor, 20, 1.0, 1.0, 1.0, 0.1, org.bukkit.Bukkit.createBlockData(Material.MAGMA_BLOCK));
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}