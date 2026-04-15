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

public class FocusLight implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
        double damagePerShot = 0.20;
        int maxShots = (int) (totalFocusMulti / damagePerShot);

        Location spawnLoc = player.getEyeLocation().add(0, 4, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);

        new BukkitRunnable() {
            int shotsFired = 0;
            @Override
            public void run() {
                if (shotsFired >= maxShots || !player.isOnline()) {
                    spawnLoc.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 15, 0.5, 0.5, 0.5, 0.1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                    this.cancel();
                    return;
                }

                // Tinh cầu Lấp lánh (Glittering)
                Particle.DustOptions orbColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
                spawnLoc.getWorld().spawnParticle(Particle.DUST, spawnLoc, 5, 0.3, 0.3, 0.3, orbColor);
                spawnLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 3, 0.4, 0.4, 0.4, 0);
                spawnLoc.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 1, 0.2, 0.2, 0.2, 0.05);

                LivingEntity target = null;
                double closestDist = Double.MAX_VALUE;
                for (org.bukkit.entity.Entity ent : spawnLoc.getWorld().getNearbyEntities(spawnLoc, 8, 8, 8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        double dist = ent.getLocation().distanceSquared(spawnLoc);
                        if (dist < closestDist) {
                            closestDist = dist;
                            target = (LivingEntity) ent;
                        }
                    }
                }

                if (target != null) {
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
                    Location tLoc = target.getLocation().add(0, 1, 0);
                    Vector laserDir = tLoc.toVector().subtract(spawnLoc.toVector());
                    double dist = laserDir.length();
                    Vector step = laserDir.normalize().multiply(0.5);
                    Location ray = spawnLoc.clone();

                    for (double d = 0; d < dist; d += 0.5) {
                        ray.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ray, 1, 0, 0, 0, 0);
                        ray.getWorld().spawnParticle(Particle.FIREWORK, ray, 1, 0.02, 0.02, 0.02, 0);
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
                    shotsFired++;
                }
            }
        }.runTaskTimer(plugin, 10L, 5L);

        DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
    }
}