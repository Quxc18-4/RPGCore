package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class UpbeatMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((6.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {1.0f, 1.0f, 1.2f, 1.5f, 1.5f, 1.8f};
        int[] ticks = {0, 5, 15, 20, 30, 35}; // Đánh theo cặp (tằng tằng... tằng tằng)
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.YELLOW, Particle.ELECTRIC_SPARK);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                RPGPlayer rpgAlly = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());

                MelodyUtils.applyTimedBuff(ally, rpgAlly, "BARD_UPBEAT_BUFF", durationTicks, true);
                MelodyUtils.drawTargetFeedback(ally, Particle.WAX_ON);
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}