package vn.minecraft.rPGCore.classes.impl.mage.attacks;

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

public class WaterAttack implements IMageBasicAttack {

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        RPGCore plugin = RPGCore.getInstance();

        // 1. TRẠM ĐIỀU PHỐI TỤ LỰC
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            new vn.minecraft.rPGCore.skills.mage.focus.FocusWater().castFocus(player, rpgPlayer, weapon, focusLevel);
            return;
        }

        // 2. LOGIC ĐÁNH THƯỜNG
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);
        Vector rightVec = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (rightVec.lengthSquared() < 0.001) rightVec.setX(1);

        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            boolean hasDamagedWeapon = false;
            Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 34) { this.cancel(); return; }
                currentLoc.add(direction);

                if (ticks > 2) {
                    for (double d = -0.8; d <= 0.8; d += 0.2) {
                        double backOffset = Math.abs(d) * 0.8;
                        Location pLoc = currentLoc.clone().add(rightVec.clone().multiply(d)).subtract(direction.clone().normalize().multiply(backOffset));
                        currentLoc.getWorld().spawnParticle(Particle.BUBBLE, pLoc, 1, 0, 0, 0, 0);
                        currentLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 1, 0, 0, 0, 0);
                    }
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.2f);
                    currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 15, 0.5, 0.5, 0.5, 0.1);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                        LivingEntity target = (LivingEntity) ent;
                        hitSet.add(target.getUniqueId());

                        if (hitSet.size() >= 5) { this.cancel(); }

                        if (!hasDamagedWeapon) {
                            DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
                            hasDamagedWeapon = true;
                        }

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
                        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 20, 1, 0.5, 1, 0.1);

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.4, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        ReactionEngine.applyMarkOnly(target, MageElement.WATER);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        if (hitSet.size() >= 5) return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}