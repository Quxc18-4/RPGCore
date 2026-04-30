package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class LightBeamFusion implements IFusion {
    @Override public MageElement getElement1() { return MageElement.LIGHT; }
    @Override public MageElement getElement2() { return MageElement.LIGHT; }
    @Override public String getReactionName() { return "§e§lQUANG THẦN PHÁO"; }
    @Override public double getDamageMultiplier() { return 1.40; } // Thuần sát thương xuyên

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        Vector beamDir = direction.clone().normalize();
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 2.0f);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.5f);

        // Raycast Laser 15 block
        for (double i = 0; i <= 15; i += 0.5) {
            Location pLoc = hitLoc.clone().add(beamDir.clone().multiply(i));
            pLoc.getWorld().spawnParticle(Particle.END_ROD, pLoc, 5, 0.2, 0.2, 0.2, 0.05);
            pLoc.getWorld().spawnParticle(Particle.FIREWORK, pLoc, 3, 0.1, 0.1, 0.1, 0);

            // Xuyên tường thì dừng tia sáng
            if (pLoc.getBlock().getType().isSolid()) break;

            for (Entity entity : pLoc.getWorld().getNearbyEntities(pLoc, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!target.hasMetadata("Laser_Hit_" + caster.getUniqueId())) {
                        target.setMetadata("Laser_Hit_" + caster.getUniqueId(), new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

                        double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                        CombatManager.applySkillDamage(caster, target, hitDmg);
                    }
                }
            }
        }
    }
}