package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class ChaosFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.LIGHT; }
    @Override public MageElement getElement2() { return MageElement.DARK; }
    @Override public String getReactionName() { return "§d§lHỖN ĐỘN"; }
    @Override public double getDamageMultiplier() { return 1.50; }

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        hitLoc.getWorld().spawnParticle(Particle.WITCH, hitLoc.add(0, 1, 0), 50, 2.5, 2.5, 2.5, 0.1);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);

        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 5.0, 5.0, 5.0)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                // [DEBUFF]: Phá 20% Giáp & Kháng Phép (5s)
                target.setMetadata("Chaos_Armor_Shred", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000L));
                target.setMetadata("Chaos_Magic_Shred", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000L));

                // [SÁT THƯƠNG LÕI]
                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }
}