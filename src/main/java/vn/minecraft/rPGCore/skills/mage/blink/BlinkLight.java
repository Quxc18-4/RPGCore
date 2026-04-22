package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class BlinkLight implements IBlink {
    @Override
    public void executeBlink(Player player, RPGPlayer rpgPlayer, int skillLevel) {
        Location start = player.getLocation();
        // [ĐÃ SỬA]: Lướt thẳng về phía trước song song với mặt đất
        double yaw = Math.toRadians(start.getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));

        Location targetLoc = start.clone();
        // Ánh sáng lướt 8 blocks
        for (double d = 0; d <= 8.0; d += 0.5) {
            Location checkLoc = targetLoc.clone().add(dir.clone().multiply(0.5));
            if (checkLoc.getBlock().getType().isSolid() || checkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                break;
            }
            targetLoc = checkLoc;
        }

        targetLoc.setPitch(start.getPitch());
        targetLoc.setYaw(start.getYaw());

        // Hiệu ứng ánh sáng vệt dọc đường lướt
        double dist = start.distance(targetLoc);
        Vector step = dir.clone().multiply(0.5);
        Location trail = start.clone();
        for (double d = 0; d < dist; d += 0.5) {
            trail.getWorld().spawnParticle(Particle.END_ROD, trail.clone().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0);
            trail.add(step);
        }

        start.getWorld().playSound(start, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1.5f);

        player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(RPGCore.getInstance(), true));
        player.teleport(targetLoc);

        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
        targetLoc.getWorld().spawnParticle(Particle.FIREWORK, targetLoc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
    }
}