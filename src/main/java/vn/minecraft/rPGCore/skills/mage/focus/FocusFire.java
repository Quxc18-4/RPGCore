package vn.minecraft.rPGCore.skills.mage.focus;

import org.bukkit.Location;
import org.bukkit.Material;
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

public class FocusFire implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();

        // 1. Tiêu thụ Buff ngay lập tức
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        player.getWorld().playSound(startLoc, Sound.ENTITY_GHAST_SHOOT, 1.2f, 0.8f);

        org.bukkit.entity.ItemDisplay coreDisplay = null;
        try {
            coreDisplay = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
                display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD);
                display.setTeleportDuration(1);
                org.bukkit.util.Transformation transform = display.getTransformation();
                transform.getScale().set(0.8f, 0.8f, 0.8f);
                display.setTransformation(transform);
            });
        } catch (Exception ignored) {}
        final org.bukkit.entity.ItemDisplay finalCore = coreDisplay;

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) { explode(currentLoc, totalFocusMulti, player, rpgPlayer); this.cancel(); return; }
                currentLoc.add(direction);

                if (finalCore != null && finalCore.isValid()) {
                    Location displayLoc = currentLoc.clone();
                    displayLoc.setYaw(displayLoc.getYaw() + (ticks * 20));
                    displayLoc.setPitch(displayLoc.getPitch() + (ticks * 20));
                    finalCore.teleport(displayLoc);
                }

                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 10, 0.4, 0.4, 0.4, 0.05);
                currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc, 2, 0.2, 0.2, 0.2, 0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    explode(currentLoc, totalFocusMulti, player, rpgPlayer);
                    this.cancel();
                    return;
                }
                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        explode(currentLoc, totalFocusMulti, player, rpgPlayer);
                        this.cancel();
                        return;
                    }
                }
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                if (finalCore != null && finalCore.isValid()) finalCore.remove();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
    }

    private void explode(Location hitLoc, double totalDmgMulti, Player player, RPGPlayer rpgPlayer) {
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1);
        hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 50, 2.0, 2.0, 2.0, 0.1);

        List<LivingEntity> validTargets = new ArrayList<>();
        for (org.bukkit.entity.Entity ent : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                validTargets.add((LivingEntity) ent);
            }
        }

        if (validTargets.isEmpty()) return;

        double dmgPerTarget = Math.max(totalDmgMulti / validTargets.size(), totalDmgMulti * 0.30);

        for (LivingEntity target : validTargets) {
            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.FIRE);
            double finalDmgMulti = dmgPerTarget * reaction.damageMultiplier;

            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
            CombatManager.applySkillDamage(player, target, finalDmg);
            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            target.setFireTicks(100);
        }
    }
}