package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class OvergrowthFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.WATER; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }
    @Override public String getReactionName() { return "§a§lSINH SÔI"; }
    @Override public double getDamageMultiplier() { return 1.0; }

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();
        boolean hitAny = false;

        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 5.0, 5.0, 5.0)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;
                hitAny = true;

                // [KHỐNG CHẾ]: Trói chân 3 giây
                StatusEffectManager.applySlow(target, 60, 1.0, 10);
                target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }

        // [HỒI MÁU]: Chỉ hồi khi trúng ít nhất 1 mục tiêu
        if (hitAny) {
            double maxHp = caster.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healAmount = maxHp * 0.05;

            caster.setHealth(Math.min(maxHp, caster.getHealth() + healAmount));
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            caster.getWorld().spawnParticle(Particle.HEART, caster.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
        }
    }
}