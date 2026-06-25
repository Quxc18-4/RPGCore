package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class AegisMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (6.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);
        int sharpCount = ToneModifier.getSharpToneCount(player);

        player.getWorld().playSound(targetLocation, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                long expireTime = System.currentTimeMillis() + (long) (durationTicks * 50);

                // Gắn Buff Can Trường (Giảm 10% DMG nhận vào)
                ally.setMetadata(BardClass.META_AEGIS_BUFF, new FixedMetadataValue(RPGCore.getInstance(), expireTime));

                if (sharpCount > 0) {
                    ally.setMetadata("BARD_SHARP_BUFF", new FixedMetadataValue(RPGCore.getInstance(), expireTime));
                }

                ally.getWorld().spawnParticle(Particle.SCRAPE, ally.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0);
            }
        }

        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}