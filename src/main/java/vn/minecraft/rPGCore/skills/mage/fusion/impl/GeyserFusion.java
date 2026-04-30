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

public class GeyserFusion implements IFusion {
    @Override public MageElement getElement1() { return MageElement.WATER; }
    @Override public MageElement getElement2() { return MageElement.WATER; }
    @Override public String getReactionName() { return "§b§lHẢI THẦN NỘ"; }
    @Override public double getDamageMultiplier() { return 1.25; } // Đánh đổi lấy Hất tung

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_SPLASH, 2.0f, 0.5f);
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_WATER_AMBIENT, 2.0f, 1.5f);

        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        // Quét và Hất tung ngay lập tức
        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.5, 3.5, 3.5)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;
                target.setVelocity(new Vector(0, 1.4, 0)); // Hard CC: Hất bổng

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }

        // Animation Cột nước sục lên
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 10) { this.cancel(); return; }
                for (double y = 0; y < 6; y += 0.5) {
                    Location pLoc = hitLoc.clone().add(0, y, 0);
                    pLoc.getWorld().spawnParticle(Particle.SPLASH, pLoc, 20, 1.5, 0.2, 1.5, 0.1);
                    pLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 5, 1.0, 0.2, 1.0, 0.05);
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}