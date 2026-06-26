package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class MirageMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((2.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {1.5f, 1.8f, 1.2f, 2.0f, 1.0f, 1.5f};
        int[] ticks = {0, 10, 20, 35, 50, 60};
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.SILVER, Particle.CAMPFIRE_COSY_SMOKE);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                ally.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) durationTicks, 0, false, false, true));
                MelodyUtils.drawTargetFeedback(ally, Particle.WHITE_ASH);

                RPGPlayer rpgAlly = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());
                if (rpgAlly != null) rpgAlly.removeCombat();
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}