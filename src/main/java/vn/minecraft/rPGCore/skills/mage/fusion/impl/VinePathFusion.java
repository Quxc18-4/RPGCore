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
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.ArrayList;
import java.util.List;

public class VinePathFusion implements IFusion {
    @Override public MageElement getElement1() { return MageElement.NATURE; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }
    @Override public String getReactionName() { return "§2§lMỘC THẦN ĐẠO"; }
    @Override public double getDamageMultiplier() { return 1.25; }

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        Vector flatDir = direction.clone().setY(0).normalize();
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(hitLoc, Sound.BLOCK_GRASS_BREAK, 1.5f, 0.5f);

        List<Location> pathNodes = new ArrayList<>();
        Particle.DustOptions vineDust = new Particle.DustOptions(org.bukkit.Color.GREEN, 1.5f);

        // Gây sát thương khởi tạo và lưu đường đi
        for (int i = 0; i <= 10; i += 2) {
            Location node = hitLoc.clone().add(flatDir.clone().multiply(i));
            pathNodes.add(node);

            node.getWorld().spawnParticle(Particle.DUST, node, 30, 1.5, 0.2, 1.5, 0, vineDust, true);

            for (Entity entity : node.getWorld().getNearbyEntities(node, 2.0, 2.0, 2.0)) {
                if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!target.hasMetadata("VinePath_Hit_" + caster.getUniqueId())) {
                        target.setMetadata("VinePath_Hit_" + caster.getUniqueId(), new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

                        double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                        CombatManager.applySkillDamage(caster, target, hitDmg);
                    }
                }
            }
        }

        // Duy trì 3 giây (60 ticks) để làm chậm kẻ bước vào
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || !caster.isOnline()) { this.cancel(); return; }

                for (Location node : pathNodes) {
                    if (ticks % 5 == 0) node.getWorld().spawnParticle(Particle.DUST, node, 5, 1.5, 0.2, 1.5, 0, vineDust, true);

                    for (Entity entity : node.getWorld().getNearbyEntities(node, 2.0, 2.0, 2.0)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            // Giảm 30% tốc độ chạy bằng StatusEffectManager
                            StatusEffectManager.applySlow((LivingEntity) entity, 40, 0.3, 1);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}