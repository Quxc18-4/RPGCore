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

public class ValorMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((8.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));
        int sharpCount = ToneModifier.getSharpToneCount(player);

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {0.7f, 1.0f, 1.25f, 1.5f, 1.5f}; // Cao độ tăng dần
        int[] ticks = {0, 10, 20, 30, 40}; // Mỗi nốt cách nhau 0.5s
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.ORANGE, Particle.FLAME);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                MelodyUtils.applyTimedBuff(ally, null, BardClass.META_VALOR_BUFF, durationTicks, false);
                MelodyUtils.drawTargetFeedback(ally, Particle.HAPPY_VILLAGER);

                if (sharpCount > 0) {
                    MelodyUtils.applyTimedBuff(ally, null, "BARD_SHARP_BUFF", durationTicks, false);
                }
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}