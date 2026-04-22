package vn.minecraft.rPGCore.skills.mage.focus;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.List;

public class FocusLight implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
        double damagePerShot = 0.20;
        int maxTotalShots = (int) (totalFocusMulti / damagePerShot); // VD: 16 tia tổng cộng

        Location spawnLoc = player.getEyeLocation().add(0, 4, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);

        new BukkitRunnable() {
            int shotsFired = 0;
            int visualTicks = 0;

            @Override
            public void run() {
                visualTicks++;
                if (shotsFired >= maxTotalShots || !player.isOnline()) {
                    spawnLoc.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 15, 0.5, 0.5, 0.5, 0.1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                    this.cancel();
                    return;
                }

                // VISUAL: Sphere + 2 Cánh
                Particle.DustOptions orbColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
                Particle.DustOptions wingColor = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.0f);

                spawnLoc.getWorld().spawnParticle(Particle.DUST, spawnLoc, 5, 0.3, 0.3, 0.3, orbColor);

                // Cánh xoay nhẹ
                double wingOffset = Math.sin(visualTicks * 0.1) * 0.2;
                Location wing1 = spawnLoc.clone().add(1.2, wingOffset, 0);
                Location wing2 = spawnLoc.clone().add(-1.2, wingOffset, 0);
                spawnLoc.getWorld().spawnParticle(Particle.DUST, wing1, 3, 0.2, 0.4, 0.2, wingColor);
                spawnLoc.getWorld().spawnParticle(Particle.DUST, wing2, 3, 0.2, 0.4, 0.2, wingColor);

                // Logic bắn mỗi 5 ticks
                if (visualTicks % 5 == 0) {
                    List<LivingEntity> targets = new ArrayList<>();
                    for (org.bukkit.entity.Entity ent : spawnLoc.getWorld().getNearbyEntities(spawnLoc, 8, 8, 8)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            targets.add((LivingEntity) ent);
                        }
                    }

                    // Ưu tiên quái gần nhất
                    targets.sort((e1, e2) -> Double.compare(e1.getLocation().distanceSquared(spawnLoc), e2.getLocation().distanceSquared(spawnLoc)));

                    int firedThisTick = 0;
                    for (LivingEntity target : targets) {
                        if (firedThisTick >= 3 || shotsFired >= maxTotalShots) break; // Bắn tối đa 3 tia cùng lúc

                        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
                        Location tLoc = target.getLocation().add(0, 1, 0);
                        Vector laserDir = tLoc.toVector().subtract(spawnLoc.toVector());
                        double dist = laserDir.length();
                        Vector step = laserDir.normalize().multiply(0.5);
                        Location ray = spawnLoc.clone();

                        Particle.DustOptions beamColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 0.8f);
                        for (double d = 0; d < dist; d += 0.5) {
                            // DUST tan nhanh và không có trọng lực
                            ray.getWorld().spawnParticle(Particle.DUST, ray, 1, beamColor);
                            ray.add(step);
                        }

                        ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.LIGHT);
                        double finalDmgMulti = damagePerShot * reaction.damageMultiplier;

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                        target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.05);

                        firedThisTick++;
                        shotsFired++;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
    }
}