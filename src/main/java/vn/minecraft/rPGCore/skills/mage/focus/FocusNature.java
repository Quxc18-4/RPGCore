package vn.minecraft.rPGCore.skills.mage.focus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
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

import java.util.ArrayList;
import java.util.List;

public class FocusNature implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        player.getWorld().playSound(startLoc, Sound.ENTITY_SLIME_JUMP, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) { explodeSeed(currentLoc, totalFocusMulti, player, rpgPlayer); this.cancel(); return; }
                currentLoc.add(direction);

                Particle.DustOptions seedColor = new Particle.DustOptions(org.bukkit.Color.LIME, 2.0f);
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 3, 0.1, 0.1, 0.1, seedColor);
                currentLoc.getWorld().spawnParticle(Particle.COMPOSTER, currentLoc, 1, 0.1, 0.1, 0.1, 0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    explodeSeed(currentLoc, totalFocusMulti, player, rpgPlayer);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        explodeSeed(currentLoc, totalFocusMulti, player, rpgPlayer);
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
    }

    private void explodeSeed(Location hitLoc, double totalDmgMulti, Player player, RPGPlayer rpgPlayer) {
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GRASS_BREAK, 1.5f, 0.5f);
        hitLoc.getWorld().spawnParticle(Particle.BLOCK, hitLoc, 50, 2.0, 0.5, 2.0, org.bukkit.Bukkit.createBlockData(Material.MOSS_BLOCK));
        hitLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, hitLoc, 30, 2.0, 1.0, 2.0, 0);

        List<LivingEntity> validTargets = new ArrayList<>();
        for (org.bukkit.entity.Entity ent : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.5, 2.5, 3.5)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                validTargets.add((LivingEntity) ent);
            }
        }

        if (validTargets.isEmpty()) return;

        double dmgPerTarget = Math.max(totalDmgMulti / validTargets.size(), totalDmgMulti * 0.30);

        for (LivingEntity target : validTargets) {
            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10); // Trói 2s

            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.NATURE);
            double finalDmgMulti = dmgPerTarget * reaction.damageMultiplier;

            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
            CombatManager.applySkillDamage(player, target, finalDmg);
            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
        }
    }
}