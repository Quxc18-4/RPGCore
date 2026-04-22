package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class BlinkWater implements IBlink {
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

        start.getWorld().playSound(start, Sound.ENTITY_PLAYER_SPLASH, 1f, 0.5f);
        player.teleport(targetLoc);
        targetLoc.getWorld().spawnParticle(Particle.SPLASH, targetLoc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

        int durationSeconds = 3 + (skillLevel / 2); // 3.5s -> 6s

        // Tạo bãi mây sương mù tại điểm BẮT ĐẦU (chứ không phải điểm đến)
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Vẽ mây sương mù (4x4 => Bán kính 2.0)
                if (ticks % 5 == 0) {
                    start.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, start.clone().add(0, 0.5, 0), 20, 1.5, 0.5, 1.5, 0.02);
                    start.getWorld().spawnParticle(Particle.FALLING_WATER, start.clone().add(0, 1, 0), 10, 1.5, 1.0, 1.5, 0);
                }

                // Gây mù lòa liên tục cho kẻ địch bước vào (Check mỗi 10 tick = 0.5s)
                if (ticks % 10 == 0) {
                    for (org.bukkit.entity.Entity ent : start.getWorld().getNearbyEntities(start, 2.0, 2.0, 2.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            // Áp mù lòa 1 giây (20 tick). Nếu họ ra khỏi vùng sương sẽ hết mù ngay lập tức.
                            StatusEffectManager.applyEffect((LivingEntity) ent, PotionEffectType.BLINDNESS, 30, 0);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}