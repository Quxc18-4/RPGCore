package vn.minecraft.rPGCore.managers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class StatusEffectManager {

    // 1. HÀM KHÁNG HIỆU ỨNG TRẠNG THÁI (Độc, Làm Chậm, Trói...)
    public static void applyEffect(LivingEntity target, PotionEffectType type, int durationTicks, int amplifier) {
        double resistance = 0.0;
        boolean isImmune = false;

        if (target instanceof Player) {
            Player player = (Player) target;
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());

            if (rpgPlayer != null) {
                resistance = rpgPlayer.getTotalStatsMap().getOrDefault("Kháng hiệu ứng", 0.0);

                // [HOOK MỚI]: Quét xem có đang bọc bùa lợi Cộng Hưởng (Gió) không
                if (player.hasMetadata("Resonance_Stats")) {
                    Object val = player.getMetadata("Resonance_Stats").get(0).value();
                    if (val instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Double> buffs = (java.util.Map<String, Double>) val;
                        resistance += buffs.getOrDefault("EffectResistance", 0.0);
                    }
                }

                int swiftCount = rpgPlayer.getStats().setCounts.getOrDefault("swift", 0);
                if (swiftCount >= 4 && type.equals(PotionEffectType.SLOWNESS)) {
                    isImmune = true;
                }
            }
        }

        if (isImmune) {
            if (target instanceof Player) {
                target.sendMessage("§a§l[SWIFT] §fBạn đã miễn nhiễm với hiệu ứng Làm Chậm!");
            }
            return;
        }

        // Đảm bảo kháng tối đa là 100%
        resistance = Math.min(resistance, 100.0);
        int finalDuration = (int) (durationTicks * (1.0 - (resistance / 100.0)));

        if (finalDuration > 0) {
            target.addPotionEffect(new PotionEffect(type, finalDuration, amplifier));
        }
    }

    // 2. HÀM KHÁNG LỰC KÉO/ĐẨY (Dùng cho Mũi tên Lỗ đen)
    public static Vector calculateResistedVelocity(LivingEntity target, Vector originalVelocity) {
        if (!(target instanceof Player)) return originalVelocity;

        Player player = (Player) target;
        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return originalVelocity;

        double resistance = rpgPlayer.getTotalStatsMap().getOrDefault("Kháng hiệu ứng", 0.0);

        // [HOOK MỚI]: Quét bùa lợi Cộng Hưởng (Gió)
        if (player.hasMetadata("Resonance_Stats")) {
            Object val = player.getMetadata("Resonance_Stats").get(0).value();
            if (val instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Double> buffs = (java.util.Map<String, Double>) val;
                resistance += buffs.getOrDefault("EffectResistance", 0.0);
            }
        }

        resistance = Math.min(resistance, 100.0);
        double factor = Math.max(0.0, 1.0 - (resistance / 100.0));
        return originalVelocity.multiply(factor);
    }
}