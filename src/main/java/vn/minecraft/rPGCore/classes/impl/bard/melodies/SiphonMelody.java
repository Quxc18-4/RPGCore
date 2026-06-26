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
import vn.minecraft.rPGCore.player.RPGPlayer;

public class SiphonMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((8.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f};
        int[] ticks = {0, 5, 40, 45, 80, 85}; // Đập thình thịch... thình thịch...
        MelodyUtils.playMelodySequence(targetLocation, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.RED, Particle.SOUL);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;
                MelodyUtils.applyTimedBuff(enemy, null, BardClass.META_SIPHON_MARK, durationTicks, false);
                MelodyUtils.drawTargetFeedback(enemy, Particle.DAMAGE_INDICATOR);
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}