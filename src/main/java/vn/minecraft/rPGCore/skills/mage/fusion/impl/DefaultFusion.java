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

public class DefaultFusion implements IFusion {

    @Override public MageElement getElement1() { return null; }
    @Override public MageElement getElement2() { return null; }
    @Override public String getReactionName() { return "§f§lMA LỰC THUẦN TÚY"; }
    @Override public double getDamageMultiplier() { return 1.0; } // Sát thương gốc x1.0

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location startLoc = caster.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(1.2);

        caster.getWorld().playSound(startLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);

        // Bắn một quả cầu ma lực thuần túy (Màu trắng/đục)
        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 25 || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                currentLoc.add(direction);
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 2, 0.05, 0.05, 0.05, 0.0);
                currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 5, 0.1, 0.1, 0.1, 0.0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    explode(caster, rpgPlayer, currentLoc, baseMultiplier);
                    this.cancel();
                    return;
                }

                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        explode(caster, rpgPlayer, currentLoc, baseMultiplier);
                        this.cancel();
                        return;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void explode(Player caster, RPGPlayer rpgPlayer, Location loc, double baseMultiplier) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);

        // [FIX BUG]: Hệ số cuối = Hệ số Kỹ Năng * Hệ số Phản Ứng
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                // CombatManager nhận finalDmgMulti (VD: 5.5) và tự lấy 300 dmg của bạn ra nhân lên
                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }
}