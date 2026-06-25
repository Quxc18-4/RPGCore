package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class UpbeatMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (6.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);
        int sharpCount = ToneModifier.getSharpToneCount(player);

        player.getWorld().playSound(targetLocation, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.5f);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                long expireTime = System.currentTimeMillis() + (long) (durationTicks * 50);

                // Gắn Buff Dồn Dập (BardClass tự động đọc cờ này để +15% AS và +30% MS)
                ally.setMetadata("BARD_UPBEAT_BUFF", new FixedMetadataValue(RPGCore.getInstance(), expireTime));

                if (sharpCount > 0) {
                    ally.setMetadata("BARD_SHARP_BUFF", new FixedMetadataValue(RPGCore.getInstance(), expireTime));
                }

                // Ép Bukkit cập nhật lại MS lập tức do có sự can thiệp của Buff Tốc độ di chuyển
                RPGPlayer rpgAlly = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());
                if (rpgAlly != null) rpgAlly.updateMoveSpeed();

                ally.getWorld().spawnParticle(Particle.WAX_ON, ally.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0);
            }
        }

        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}