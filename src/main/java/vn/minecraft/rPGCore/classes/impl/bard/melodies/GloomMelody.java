package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class GloomMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((5.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));
        double slowPercent = 0.30 * ToneModifier.getHighToneMultiplier(player);

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {1.0f, 0.8f, 0.7f, 0.6f, 0.5f};
        int[] ticks = {0, 20, 40, 60, 80}; // Đánh rất chậm, cách nhau 1s
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.PURPLE, Particle.SQUID_INK);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;
                StatusEffectManager.applySlow(enemy, (int) durationTicks, slowPercent, 1);
                MelodyUtils.applyTimedBuff(enemy, null, BardClass.META_GLOOM_DEBUFF, durationTicks, false);
                MelodyUtils.drawTargetFeedback(enemy, Particle.WITCH);
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}