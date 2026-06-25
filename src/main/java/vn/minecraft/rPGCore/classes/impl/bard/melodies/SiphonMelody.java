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
import vn.minecraft.rPGCore.player.RPGPlayer;

public class SiphonMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (8.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);

        player.getWorld().playSound(targetLocation, Sound.ENTITY_VEX_CHARGE, 1.0f, 0.5f);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;
                long expireTime = System.currentTimeMillis() + (long) (durationTicks * 50);

                // Ám Dấu Ấn Hút Hồn lên Quái Vật (Xử lý hồi máu bên trong Hook onAttackLanded của BardClass)
                enemy.setMetadata(BardClass.META_SIPHON_MARK, new FixedMetadataValue(RPGCore.getInstance(), expireTime));

                enemy.getWorld().spawnParticle(Particle.SOUL, enemy.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
            }
        }

        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}