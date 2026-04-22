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

    // ==========================================
    // [UNIVERSAL CC] KHÓA DI CHUYỂN & KHÁNG ĐẨY LÙI TUYỆT ĐỐI
    // ==========================================
    public static void applyAbsoluteRoot(org.bukkit.entity.Player player, int durationTicks) {
        long expireTime = System.currentTimeMillis() + (durationTicks * 50L);
        player.setMetadata("RPGCore_Rooted", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), expireTime));

        // 1. Khóa tốc độ và khóa nút Nhảy
        player.setWalkSpeed(0f);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, durationTicks, 250, false, false, false));

        // 2. Tắt va chạm thực thể (CHÌA KHÓA CHỐNG QUÁI HÚC/ĐẨY LÙI)
        player.setCollidable(false);

        // 3. Kháng Knockback (Chuẩn 1.21+, không dùng valueOf để tránh lỗi báo vàng)
        org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);

        org.bukkit.Location anchor = player.getLocation().clone();

        // Task neo vị trí (Đè gia tốc)
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata("RPGCore_Rooted")) {
                    this.cancel();
                    return;
                }
                long currentExpire = player.getMetadata("RPGCore_Rooted").get(0).asLong();
                if (System.currentTimeMillis() >= currentExpire) {
                    removeAbsoluteRoot(player);
                    this.cancel();
                    return;
                }

                // Ép gia tốc về 0
                player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

                // Nếu client cố tình lách vị trí > 0.1 block, giật lại ngay
                org.bukkit.Location current = player.getLocation();
                if (current.distanceSquared(anchor) > 0.01) {
                    org.bukkit.Location tp = anchor.clone();
                    tp.setYaw(current.getYaw());
                    tp.setPitch(current.getPitch());
                    player.teleport(tp);
                }
            }
        }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 1L);
    }

    public static void removeAbsoluteRoot(org.bukkit.entity.Player player) {
        if (player.hasMetadata("RPGCore_Rooted")) {
            player.removeMetadata("RPGCore_Rooted", vn.minecraft.rPGCore.RPGCore.getInstance());

            // Trả lại trạng thái bình thường
            player.setWalkSpeed(0.2f);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
            player.setCollidable(true);

            org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) kbAttr.setBaseValue(0.0);
        }
    }

    // ==========================================
    // [UNIVERSAL]: BAY LƠ LỬNG AN TOÀN (CUSTOM HOVER)
    // ==========================================
    public static void applyCustomHover(org.bukkit.entity.Player player, int durationTicks, float flySpeed, boolean safeFall) {
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flySpeed);

        // Gắn cờ SafeFall để chống sốc sát thương ngã
        if (safeFall) {
            player.setMetadata("RPGCore_SafeFall", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), true));
        }

        player.setMetadata("RPGCore_Hovering", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + (durationTicks * 50L)));

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata("RPGCore_Hovering")) {
                    this.cancel();
                    return;
                }
                long expire = player.getMetadata("RPGCore_Hovering").get(0).asLong();
                if (System.currentTimeMillis() >= expire) {
                    removeCustomHover(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 2L);
    }

    public static void removeCustomHover(org.bukkit.entity.Player player) {
        if (player.hasMetadata("RPGCore_Hovering")) {
            player.removeMetadata("RPGCore_Hovering", vn.minecraft.rPGCore.RPGCore.getInstance());

            // Xóa quyền bay (trừ phi đang Sáng Tạo)
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            player.setFlySpeed(0.1f); // Trả về tốc độ bay Vanilla
            player.setFallDistance(0f); // Xóa khoảng cách rơi tích lũy

            // Note: Để an toàn tuyệt đối, bên PlayerListener bắt sự kiện EntityDamageEvent (FALL),
            // check nếu có meta RPGCore_SafeFall thì hủy sát thương ngã và xóa cờ đó đi.
        }
    }
}