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
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class GloomMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (5.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);

        // Âm Cao (+25%) áp dụng vào hệ số Slow (Base 30% -> 37.5%)
        double slowPercent = 0.30 * ToneModifier.getHighToneMultiplier(player);

        player.getWorld().playSound(targetLocation, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;

                // Gọi API Slow của Server
                StatusEffectManager.applySlow(enemy, (int) durationTicks, slowPercent, 1);

                // Gắn debuff giảm 10% sát thương (Hook ở BardClass sẽ đọc)
                long expire = System.currentTimeMillis() + (long) (durationTicks * 50);
                enemy.setMetadata(BardClass.META_GLOOM_DEBUFF, new FixedMetadataValue(RPGCore.getInstance(), expire));

                enemy.getWorld().spawnParticle(Particle.LARGE_SMOKE, enemy.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}