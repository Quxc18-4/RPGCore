package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class MirageMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (2.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);

        player.getWorld().playSound(targetLocation, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;

                // Cấp Potion Tàng Hình Vanilla để quái vật ngừng Target
                ally.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) durationTicks, 0, false, false, true));

                // Mở khóa Giao tranh để reset các cơ chế truy đuổi (nếu có)
                RPGPlayer rpgAlly = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());
                if (rpgAlly != null) {
                    rpgAlly.removeCombat();
                }

                ally.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, ally.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.02);
            }
        }

        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}