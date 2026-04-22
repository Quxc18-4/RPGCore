package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class BlinkDark implements IBlink {
    @Override
    public void executeBlink(Player player, RPGPlayer rpgPlayer, int skillLevel) {
        Location start = player.getLocation();
        // [ĐÃ SỬA]: Lướt lùi về phía sau song song với mặt đất
        double yaw = Math.toRadians(start.getYaw());
        Vector dir = new Vector(Math.sin(yaw), 0, -Math.cos(yaw)); // Đảo dấu X và Z

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

        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

        player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(RPGCore.getInstance(), true));
        player.teleport(targetLoc);

        targetLoc.getWorld().spawnParticle(Particle.SQUID_INK, targetLoc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

        int durationSeconds = 4 + skillLevel;

        // Tạo bãi khói mù tại điểm xuất phát
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel(); return;
                }

                if (ticks % 5 == 0) {
                    start.getWorld().spawnParticle(Particle.LARGE_SMOKE, start.clone().add(0, 0.5, 0), 20, 1.5, 0.5, 1.5, 0.02);
                    start.getWorld().spawnParticle(Particle.PORTAL, start.clone().add(0, 1, 0), 10, 1.5, 1.0, 1.5, 0);
                }

                if (ticks % 10 == 0) {
                    for (org.bukkit.entity.Entity ent : start.getWorld().getNearbyEntities(start, 2.0, 2.0, 2.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            // Áp hiệu ứng Darkness (che khuất tầm nhìn mạnh hơn Blindness)
                            StatusEffectManager.applyEffect((LivingEntity) ent, PotionEffectType.DARKNESS, 30, 0);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}