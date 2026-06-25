package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class VitalityMelody implements IMelody {
    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (5.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);
        double highMultiplier = ToneModifier.getHighToneMultiplier(player);

        player.getWorld().playSound(targetLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        // LÕI 7: Dấu Lặng Ngân (Fermata) - Thanh Tẩy
        boolean hasFermata = rpgPlayer.hasSetBonus("BARD_CORE_7", 1);

        // Tính toán Máu & Khiên (Base 5% Max HP Bard)
        double healAmount = (rpgPlayer.getTotalMaxHealth() * 0.05) * highMultiplier;

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof Player && !CombatManager.canTarget(player, e)) {
                Player ally = (Player) e;
                RPGPlayer rpgAlly = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());

                if (rpgAlly != null) {
                    // Gọi hàm heal có màng lọc Anti-Heal
                    rpgAlly.heal(healAmount);

                    // Cấp Khiên (Sử dụng Meta khiên có sẵn trong RPGCore cleanup)
                    long expire = System.currentTimeMillis() + (long) (durationTicks * 50);
                    ally.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(RPGCore.getInstance(), healAmount));
                    ally.setMetadata("RPG_Absorption_Shield_Expire", new FixedMetadataValue(RPGCore.getInstance(), expire));

                    // Xử lý Lõi 7: Thanh Tẩy
                    if (hasFermata) {
                        StatusEffectManager.cleanseNegativeEffects(ally);
                    }

                    ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
                }
            }
        }

        // Xử lý chung: Dissonance, Afterglow, Eternal Melody
        MelodyUtils.processGlobalPerks(player, rpgPlayer, targetLocation, radius, durationTicks);
    }
}