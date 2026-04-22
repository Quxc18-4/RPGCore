package vn.minecraft.rPGCore.skills.mage.focus;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FocusDark implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        player.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            Location lastLoc = startLoc.clone();
            Set<UUID> hitSet = new HashSet<>();
            LivingEntity isolatedTarget = null;
            boolean isFirstHit = true;

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) { this.cancel(); return; }

                lastLoc = currentLoc.clone();
                currentLoc.add(direction);

                double dist = lastLoc.distance(currentLoc);
                Vector step = direction.clone().normalize().multiply(0.5);
                Location tempLoc = lastLoc.clone();
                for (double d = 0; d < dist; d += 0.5) {
                    tempLoc.getWorld().spawnParticle(Particle.SQUID_INK, tempLoc, 3, 0.3, 0.3, 0.3, 0);
                    tempLoc.getWorld().spawnParticle(Particle.PORTAL, tempLoc, 10, 0.4, 0.4, 0.4, 0);
                    tempLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, tempLoc, 1, 0.2, 0.2, 0.2, 0);
                    tempLoc.add(step);
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 0.5f);
                    currentLoc.getWorld().spawnParticle(Particle.SQUID_INK, currentLoc, 30, 0.5, 0.5, 0.5, 0.05);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                        LivingEntity target = (LivingEntity) ent;
                        hitSet.add(target.getUniqueId());

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.5f);
                        target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);

                        double appliedDamage = 0;

                        if (isFirstHit) {
                            isolatedTarget = target;
                            isFirstHit = false;
                            appliedDamage = totalFocusMulti * 0.90; // 90% cho mục tiêu đầu
                        } else {
                            appliedDamage = totalFocusMulti * 0.30; // 30% cho mục tiêu sau
                            StatusEffectManager.applyEffect(target, PotionEffectType.BLINDNESS, 80, 0);
                            if (target instanceof Mob) ((Mob) target).setTarget(null); // Xóa Aggro
                        }

                        ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.DARK);
                        double finalDmgMulti = appliedDamage * reaction.damageMultiplier;

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                    }
                }
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                // Cơ chế Cô lập (Isolation): Nếu đạn biến mất mà chỉ trúng 1 mục tiêu
                if (hitSet.size() == 1 && isolatedTarget != null && isolatedTarget.isValid() && !isolatedTarget.isDead()) {
                    isolatedTarget.getWorld().playSound(isolatedTarget.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.5f, 1.5f);
                    isolatedTarget.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, isolatedTarget.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);

                    double extraDmgMulti = totalFocusMulti * 0.40; // Bù thêm 40%
                    player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                    double extraDmg = CombatManager.calculateDamage(player, rpgPlayer, isolatedTarget, extraDmgMulti, 0.0, true, false);
                    CombatManager.applySkillDamage(player, isolatedTarget, extraDmg);
                    player.removeMetadata("Ghost_L_Blocker", plugin);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
    }
}