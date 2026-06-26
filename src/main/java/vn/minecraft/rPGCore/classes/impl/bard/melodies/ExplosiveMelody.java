package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ExplosiveMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double kbForce = 1.5 * ToneModifier.getHighToneMultiplier(player);

        // Thay thế MelodyUtils.playArpeggio bằng:
        float[] pitches = {0.5f, 0.8f, 1.0f};
        int[] ticks = {0, 2, 4}; // Quạt tay cực mạnh
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_NOTE_BLOCK_GUITAR, 1.5f, pitches, ticks);
        player.getWorld().playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.MAROON, Particle.EXPLOSION);

        player.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLocation.add(0, 1, 0), 1);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;
                Vector direction = enemy.getLocation().toVector().subtract(targetLocation.toVector());
                if (direction.lengthSquared() == 0) direction = new Vector(0, 1, 0);
                else direction.normalize();

                direction.multiply(kbForce).setY(Math.min(0.8, kbForce * 0.5));
                enemy.setVelocity(direction);
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, 0);
    }
}