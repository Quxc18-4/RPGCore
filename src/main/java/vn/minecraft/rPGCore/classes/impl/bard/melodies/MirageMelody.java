package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class MirageMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        long durationTicks = (long) ((2.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player));

        float[] pitches = {1.5f, 1.8f, 1.2f, 2.0f, 1.0f, 1.5f};
        int[] ticks = {0, 10, 20, 35, 50, 60};
        MelodyUtils.playMelodySequence(targetLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, pitches, ticks);
        MelodyUtils.drawExpandingAura(targetLocation, radius, Color.SILVER, Particle.CAMPFIRE_COSY_SMOKE);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            // Chỉ áp dụng cho Player đồng minh
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;

                // 1. Tàng hình tuyệt đối (Ẩn cả giáp) bằng API Bukkit
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(ally)) {
                        online.hidePlayer(RPGCore.getInstance(), ally);
                    }
                }

                MelodyUtils.drawTargetFeedback(ally, Particle.WHITE_ASH);

                // 2. Xóa Aggro quái vật
                RPGPlayer rpgAlly = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());
                if (rpgAlly != null) rpgAlly.removeCombat();

                // 3. Hẹn giờ hiện nguyên hình sau khi hết Duration
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (ally.isOnline()) {
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.showPlayer(RPGCore.getInstance(), ally);
                            }
                            ally.getWorld().spawnParticle(Particle.CLOUD, ally.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                        }
                    }
                }.runTaskLater(RPGCore.getInstance(), durationTicks);
            }
        }
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}