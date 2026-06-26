package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class AegisMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((6.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));
        int sharpCount = ToneModifier.getSharpToneCount(player);

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {1.0f, 1.25f, 1.0f, 1.25f, 1.0f, 0.7f};
        int[] ticks = {0, 15, 30, 45, 60, 70};
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.AQUA, Particle.END_ROD);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                MelodyUtils.applyTimedBuff(ally, null, BardClass.META_AEGIS_BUFF, durationTicks, false);
                MelodyUtils.drawTargetFeedback(ally, Particle.TOTEM_OF_UNDYING);

                if (sharpCount > 0) {
                    MelodyUtils.applyTimedBuff(ally, null, "BARD_SHARP_BUFF", durationTicks, false);
                }
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}