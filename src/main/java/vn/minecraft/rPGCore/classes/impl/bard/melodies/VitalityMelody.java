package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class VitalityMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((5.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));
        double highMultiplier = ToneModifier.getHighToneMultiplier(player);

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {0.5f, 0.7f, 0.9f, 1.1f, 1.3f, 1.5f, 1.8f, 2.0f};
        int[] ticks = {0, 5, 10, 15, 20, 25, 30, 45}; // Lướt tay rất nhanh
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.LIME, Particle.GLOW);

        boolean hasFermata = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_7") > 0;
        double healAmount = (rpgPlayer.getTotalMaxHealth() * 0.05) * highMultiplier;

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                RPGPlayer rpgAlly = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());

                if (rpgAlly != null) {
                    rpgAlly.heal(healAmount);
                    long expire = System.currentTimeMillis() + (durationTicks * 50);
                    ally.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(RPGCore.getInstance(), healAmount));
                    ally.setMetadata("RPG_Absorption_Shield_Expire", new FixedMetadataValue(RPGCore.getInstance(), expire));

                    MelodyUtils.drawTargetFeedback(ally, Particle.HEART);

                    if (hasFermata) {
                        StatusEffectManager.cleanseNegativeEffects(ally);
                    }
                }
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}