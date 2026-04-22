package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class BlinkWind implements IBlink {
    @Override
    public void executeBlink(Player player, RPGPlayer rpgPlayer, int skillLevel) {
        Location start = player.getLocation();

        double yaw = Math.toRadians(start.getYaw());
        // [ĐÃ SỬA]: X=sin, Z=-cos để lùi lại. Y=1.0 để hất lên 45 độ
        Vector dir = new Vector(Math.sin(yaw), 1.0, -Math.cos(yaw)).normalize();

        Location targetLoc = start.clone();
        for (double d = 0; d <= 6.0; d += 0.5) {
            Location checkLoc = targetLoc.clone().add(dir.clone().multiply(0.5));
            if (checkLoc.getBlock().getType().isSolid() || checkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                break;
            }
            targetLoc = checkLoc;
        }

        targetLoc.setPitch(start.getPitch());
        targetLoc.setYaw(start.getYaw());

        start.getWorld().spawnParticle(Particle.CLOUD, start.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        start.getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);

        player.teleport(targetLoc);

        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.2f);
        targetLoc.getWorld().spawnParticle(Particle.GUST, targetLoc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

        // Hover 2s -> 5s
        int hoverSeconds = 2 + (skillLevel / 2);
        StatusEffectManager.applyCustomHover(player, hoverSeconds * 20, 0.02f, true);
    }
}