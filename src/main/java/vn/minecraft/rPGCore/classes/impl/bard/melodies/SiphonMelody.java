package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class SiphonMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        // Tâm điểm giờ đây là targetLocation (Vị trí quả cầu nổ) chứ không phải player
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((8.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));

        // Nhịp điệu dồn dập, ma mị
        float[] pitches = {0.8f, 1.2f, 0.5f, 1.5f};
        int[] ticks = {0, 10, 20, 30};
        MelodyUtils.playMelodySequence(targetLocation, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, pitches, ticks);

        // Vẽ Aura tại tâm Hố Đen
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.PURPLE, Particle.SOUL_FIRE_FLAME);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            // Chỉ đánh dấu lên Kẻ Địch[cite: 17, 19]
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity enemy = (LivingEntity) e;

                // Cắm cờ Hút Hồn[cite: 17]
                MelodyUtils.applyTimedBuff(enemy, null, BardClass.META_SIPHON_MARK, durationTicks, false);

                // VFX Linh hồn bị rút ra
                new BukkitRunnable() {
                    int i = 0;
                    @Override
                    public void run() {
                        if (i++ > 10 || enemy.isDead()) { this.cancel(); return; }
                        enemy.getWorld().spawnParticle(Particle.SOUL, enemy.getLocation().add(0, 1 + (i*0.2), 0), 1, 0.2, 0.2, 0.2, 0.05);
                        enemy.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, enemy.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0.1);
                    }
                }.runTaskTimer(RPGCore.getInstance(), 0L, 2L);
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}