package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
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

public class LightAttack implements IMageBasicAttack {

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {

        // 1. TRẠM ĐIỀU PHỐI TỤ LỰC
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            new vn.minecraft.rPGCore.skills.mage.focus.FocusLight().castFocus(player, rpgPlayer, weapon, focusLevel);
            return;
        }

        // 2. LOGIC ĐÁNH THƯỜNG
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();
        player.getWorld().playSound(startLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
        Vector dir = startLoc.getDirection().normalize().multiply(0.5);
        Location currentLoc = startLoc.clone();

        int pierceCount = 0;
        boolean firstHit = true;
        boolean hasHit = false;
        Set<UUID> hitSet = new HashSet<>();

        Particle.DustOptions fastRayColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 0.8f);

        for (int i = 0; i < 68; i++) {
            currentLoc.add(dir);

            if (i > 6) {
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 1, fastRayColor);
                if (i % 2 == 0) currentLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 1, 0, 0, 0, 0);
            }

            if (currentLoc.getBlock().getType().isSolid()) {
                currentLoc.getWorld().spawnParticle(Particle.FIREWORK, currentLoc, 5, 0.05, 0.05, 0.05, 0.05);
                currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.5f);
                break;
            }

            for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                    LivingEntity hitTarget = (LivingEntity) ent;
                    hitSet.add(hitTarget.getUniqueId());
                    pierceCount++;
                    hasHit = true;

                    hitTarget.getWorld().playSound(hitTarget.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.2f);
                    double damage = CombatManager.calculateDamage(player, rpgPlayer, hitTarget, 0.6, 0.0, true, false);

                    if (firstHit) {
                        hitTarget.setMetadata("Mage_Exposure_Debuff", new FixedMetadataValue(plugin, true));
                        firstHit = false;
                    }

                    ReactionEngine.applyMarkOnly(hitTarget, MageElement.LIGHT);
                    player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                    CombatManager.applySkillDamage(player, hitTarget, damage);
                    player.removeMetadata("Ghost_L_Blocker", plugin);

                    if (pierceCount >= 3) break;
                }
            }
            if (pierceCount >= 3) break;
        }
        if (hasHit) DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
    }
}