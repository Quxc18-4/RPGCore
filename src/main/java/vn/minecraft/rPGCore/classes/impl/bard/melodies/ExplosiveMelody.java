package vn.minecraft.rPGCore.classes.impl.bard.melodies;

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
        // Knockback Base là 1.5 (Tương đương văng 3 Block). Khuếch đại bởi Âm Cao.
        double kbForce = 1.5 * ToneModifier.getHighToneMultiplier(player);

        player.getWorld().playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        // Hiệu ứng Sóng Xung Kích chuẩn 1.21.x
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLocation.add(0, 1, 0), 1);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;

                // Tính toán Vector đẩy lùi văng ra khỏi tâm targetLocation
                Vector direction = enemy.getLocation().toVector().subtract(targetLocation.toVector());
                if (direction.lengthSquared() == 0) {
                    direction = new Vector(0, 1, 0); // Tránh lỗi chia cho 0 nếu đứng trùng tâm
                } else {
                    direction.normalize();
                }

                // Tạo lực đẩy (Hất lên trên một chút để tạo độ nảy)
                direction.multiply(kbForce).setY(Math.min(0.8, kbForce * 0.5));
                enemy.setVelocity(direction);
            }
        }

        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, 0); // Tức thời -> Duration = 0
    }
}