package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
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

public class BlackFireFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.DARK; }
    @Override public String getReactionName() { return "§5§lHẮC HỎA"; }
    @Override public double getDamageMultiplier() { return 1.0; }

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.5f);

        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 5.0, 5.0, 5.0)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                // Nổ sát thương lần đầu
                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);

                // [SÁT THƯƠNG ĐỊNH KỲ]: Rút máu 5% đã mất trong 4s
                startBlackFireDoT(caster, target);
            }
        }
    }

    private void startBlackFireDoT(Player caster, LivingEntity target) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 4 || target.isDead() || !target.isValid() || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 10, 0.4, 0.5, 0.4, 0);
                target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 10, 0.4, 0.5, 0.4, 0);

                double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHealth = target.getHealth();
                double missingHealth = maxHealth - currentHealth;

                double blackFireDmg = missingHealth * 0.05;
                blackFireDmg = Math.min(blackFireDmg, 350.0);
                blackFireDmg = Math.max(blackFireDmg, 10.0);

                CombatManager.applySkillDamage(caster, target, blackFireDmg);
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L);
    }
}