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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FocusWater implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
        Location startLoc = player.getEyeLocation();
        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.8f);

        Vector outDirection = startLoc.getDirection().normalize().multiply(1.5);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            Set<UUID> hitOutward = new HashSet<>();
            Set<UUID> hitInward = new HashSet<>();
            boolean isReturning = false;

            @Override
            public void run() {
                ticks++;

                if (isReturning && currentLoc.distanceSquared(player.getLocation().add(0,1,0)) < 2.25) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1.5f);
                    this.cancel();
                    return;
                }

                // Bay khoảng 13 tick * 1.5 tốc độ = ~19.5 blocks thì quay đầu
                if (!isReturning && ticks > 13) {
                    isReturning = true;
                }

                if (ticks > 60) { this.cancel(); return; }

                Vector travelDir;
                if (isReturning) {
                    travelDir = player.getLocation().add(0, 1, 0).toVector().subtract(currentLoc.toVector()).normalize().multiply(1.5);
                } else {
                    travelDir = outDirection;
                }
                currentLoc.add(travelDir);

                Vector rightVec = travelDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                if (rightVec.lengthSquared() < 0.001) rightVec.setX(1);

                for (double d = -1.5; d <= 1.5; d += 0.3) {
                    double backOffset = Math.abs(d) * 0.8;
                    Location pLoc = currentLoc.clone().add(rightVec.clone().multiply(d)).subtract(travelDir.clone().normalize().multiply(backOffset));
                    currentLoc.getWorld().spawnParticle(Particle.BUBBLE, pLoc, 1, 0, 0, 0, 0);
                    currentLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 1, 0, 0, 0, 0);
                }
                currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 5, 0.2, 0.2, 0.2, 0);

                if (!isReturning && currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.2f);
                    currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 15, 0.5, 0.5, 0.5, 0.1);
                    isReturning = true;
                }

                Set<UUID> currentHitSet = isReturning ? hitInward : hitOutward;

                if (currentHitSet.size() < 10) {
                    for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !currentHitSet.contains(ent.getUniqueId())) {
                            LivingEntity target = (LivingEntity) ent;
                            currentHitSet.add(target.getUniqueId());

                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
                            target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 30, 1, 1, 1, 0.1);

                            // Tính sát thương: Đi 60%. Về: Nếu đã trúng thì 30%, nếu chưa trúng thì 60%.
                            double appliedDamage = totalFocusMulti * 0.60;
                            if (isReturning && hitOutward.contains(target.getUniqueId())) {
                                appliedDamage = totalFocusMulti * 0.30;
                            }

                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WATER);
                            double finalDmgMulti = appliedDamage * reaction.damageMultiplier;

                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            player.removeMetadata("Ghost_L_Blocker", plugin);

                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                            if (currentHitSet.size() >= 10) break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
    }
}