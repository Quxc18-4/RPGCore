package vn.minecraft.rPGCore.managers;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Map;

public class StatusEffectManager {

    // ==========================================
    // [SIÊU HOOK]: TỰ ĐỘNG TRỪ TỐC CHẠY CHO PLAYER KHI BỊ SLOW
    // ==========================================
    static {
        CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
            @Override
            public void applyBonusStats(Player player, Map<String, Double> stats) {
                if (player.hasMetadata("STATUS_SLOW_PERCENT") && player.hasMetadata("STATUS_SLOW_EXPIRE")) {
                    long expire = player.getMetadata("STATUS_SLOW_EXPIRE").get(0).asLong();
                    if (System.currentTimeMillis() <= expire) {
                        double slowPct = player.getMetadata("STATUS_SLOW_PERCENT").get(0).asDouble();
                        // Trừ trực tiếp vào Tốc độ di chuyển (VD: 0.15 = -15%)
                        stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) - (slowPct * 100.0));
                    } else {
                        // Dọn rác nếu hết hạn
                        player.removeMetadata("STATUS_SLOW_PERCENT", RPGCore.getInstance());
                        player.removeMetadata("STATUS_SLOW_EXPIRE", RPGCore.getInstance());
                    }
                }
                // [BỔ SUNG]: Đọc Metadata Tốc chạy
                if (player.hasMetadata("STATUS_SPEED_PERCENT") && player.hasMetadata("STATUS_SPEED_EXPIRE")) {
                    long expire = player.getMetadata("STATUS_SPEED_EXPIRE").get(0).asLong();
                    if (System.currentTimeMillis() <= expire) {
                        double speedPct = player.getMetadata("STATUS_SPEED_PERCENT").get(0).asDouble();
                        // Cộng trực tiếp vào Tốc độ di chuyển (VD: 0.15 = +15%)
                        stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (speedPct * 100.0));
                    } else {
                        player.removeMetadata("STATUS_SPEED_PERCENT", RPGCore.getInstance());
                        player.removeMetadata("STATUS_SPEED_EXPIRE", RPGCore.getInstance());
                    }
                }
            }
        });
    }

    // ==========================================
    // HÀM TÍNH TOÁN KHÁNG HIỆU ỨNG CHUNG
    // ==========================================
    public static double getEffectResistance(LivingEntity target, PotionEffectType type) {
        double resistance = 0.0;
        if (target instanceof Player) {
            Player player = (Player) target;
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());

            if (rpgPlayer != null) {
                resistance = rpgPlayer.getTotalStatsMap().getOrDefault("Kháng hiệu ứng", 0.0);

                if (player.hasMetadata("Resonance_Stats")) {
                    Object val = player.getMetadata("Resonance_Stats").get(0).value();
                    if (val instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Double> buffs = (java.util.Map<String, Double>) val;
                        resistance += buffs.getOrDefault("EffectResistance", 0.0);
                    }
                }

                int swiftCount = rpgPlayer.getStats().setCounts.getOrDefault("swift", 0);
                if (swiftCount >= 4 && type != null && type.equals(PotionEffectType.SLOWNESS)) {
                    return 100.0; // Miễn nhiễm hoàn toàn
                }
            }
        }
        return Math.min(resistance, 100.0);
    }

    // ==========================================
    // 1. HÀM APPLY POTION EFFECT VANILLA (Đã Refactor)
    // ==========================================
    public static void applyEffect(LivingEntity target, PotionEffectType type, int durationTicks, int amplifier) {
        double resistance = getEffectResistance(target, type);

        if (resistance >= 100.0) {
            if (target instanceof Player && type.equals(PotionEffectType.SLOWNESS)) {
                target.sendMessage("§a§l[SWIFT] §fBạn đã miễn nhiễm với hiệu ứng!");
            }
            return;
        }

        int finalDuration = (int) (durationTicks * (1.0 - (resistance / 100.0)));
        if (finalDuration > 0) {
            target.addPotionEffect(new PotionEffect(type, finalDuration, amplifier));
        }
    }

    // ==========================================
    // [HÀM MỚI]: LÀM CHẬM UNIVERSAL (PLAYER DÙNG STATS, QUÁI DÙNG POTION)
    // ==========================================
    public static void applySlow(LivingEntity target, int durationTicks, double slowPercent, int mobAmplifier) {
        double resistance = getEffectResistance(target, PotionEffectType.SLOWNESS);

        if (resistance >= 100.0) {
            if (target instanceof Player) target.sendMessage("§a§l[MIỄN NHIỄM] §fBạn đã kháng lại hiệu ứng Làm chậm!");
            return;
        }

        int finalDuration = (int) (durationTicks * (1.0 - (resistance / 100.0)));
        if (finalDuration <= 0) return;

        if (target instanceof Player) {
            Player p = (Player) target;
            long expireTime = System.currentTimeMillis() + (finalDuration * 50L);

            // [MỚI]: Nếu bị Root (Slow >= 100%), ép thêm Jump Boost âm để không thể nhảy
            if (slowPercent >= 1.0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, finalDuration, 250, false, false, false));
            }

            // Ghi đè chỉ số Slow mới
            p.setMetadata("STATUS_SLOW_PERCENT", new FixedMetadataValue(RPGCore.getInstance(), slowPercent));
            p.setMetadata("STATUS_SLOW_EXPIRE", new FixedMetadataValue(RPGCore.getInstance(), expireTime));

            // Cập nhật tốc chạy ngay lập tức
            RPGPlayer rpgP = RPGCore.getInstance().getPlayerManager().getRPGPlayer(p.getUniqueId());
            if (rpgP != null) rpgP.updateMoveSpeed();

            // Lên lịch gỡ Slow
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() && p.hasMetadata("STATUS_SLOW_EXPIRE")) {
                        long currentExpire = p.getMetadata("STATUS_SLOW_EXPIRE").get(0).asLong();
                        // Chỉ xóa nếu thời gian hiện tại đã vượt qua Expire mới nhất (Chống dính spam)
                        if (System.currentTimeMillis() >= currentExpire) {
                            p.removeMetadata("STATUS_SLOW_PERCENT", RPGCore.getInstance());
                            p.removeMetadata("STATUS_SLOW_EXPIRE", RPGCore.getInstance());
                            if (rpgP != null) rpgP.updateMoveSpeed(); // Phục hồi tốc độ
                        }
                    }
                }
            }.runTaskLater(RPGCore.getInstance(), finalDuration);

        } else {
            // Đối với quái thì ném PotionEffect Vanilla
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, finalDuration, mobAmplifier));
        }
    }

    // ==========================================
    // 2. KHÁNG LỰC KÉO/ĐẨY (Giữ nguyên)
    // ==========================================
    public static Vector calculateResistedVelocity(LivingEntity target, Vector originalVelocity) {
        if (!(target instanceof Player)) return originalVelocity;

        double resistance = getEffectResistance(target, null);
        double factor = Math.max(0.0, 1.0 - (resistance / 100.0));
        return originalVelocity.multiply(factor);
    }

    // ==========================================
    // 3. KHÓA DI CHUYỂN TUYỆT ĐỐI (ROOT)
    // ==========================================
    public static void applyAbsoluteRoot(LivingEntity target, int durationTicks) {
        // [SET 4] SWIFT: Miễn nhiễm hoàn toàn với Trói Chân (Root)
        if (target instanceof Player) {
            Player player = (Player) target;
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null && rpgPlayer.hasSetBonus("swift", 4)) {
                player.sendMessage("§a§l[SWIFT] §fBạn đã kháng lại hiệu ứng Trói chân!");
                return; // Thoát hàm ngay lập tức, không cho Root
            }
        }
        long expireTime = System.currentTimeMillis() + (durationTicks * 50L);
        target.setMetadata("RPGCore_Rooted", new FixedMetadataValue(RPGCore.getInstance(), expireTime));

        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 250, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 10, false, false, false));

        if (target instanceof Player) {
            Player player = (Player) target;
            player.setWalkSpeed(0f);
            player.setCollidable(false);
            org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) kbAttr.setBaseValue(1.0);
        } else {
            org.bukkit.attribute.AttributeInstance kbAttr = target.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) kbAttr.setBaseValue(1.0);
        }

        org.bukkit.Location anchor = target.getLocation().clone();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || !target.hasMetadata("RPGCore_Rooted")) {
                    this.cancel();
                    return;
                }
                long currentExpire = target.getMetadata("RPGCore_Rooted").get(0).asLong();
                if (System.currentTimeMillis() >= currentExpire) {
                    removeAbsoluteRoot(target);
                    this.cancel();
                    return;
                }

                target.setVelocity(new Vector(0, 0, 0));
                org.bukkit.Location current = target.getLocation();
                if (current.distanceSquared(anchor) > 0.01) {
                    org.bukkit.Location tp = anchor.clone();
                    tp.setYaw(current.getYaw());
                    tp.setPitch(current.getPitch());
                    target.teleport(tp);
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    public static void removeAbsoluteRoot(LivingEntity target) {
        if (target.hasMetadata("RPGCore_Rooted")) {
            target.removeMetadata("RPGCore_Rooted", RPGCore.getInstance());
            target.removePotionEffect(PotionEffectType.JUMP_BOOST);
            target.removePotionEffect(PotionEffectType.SLOWNESS);

            org.bukkit.attribute.AttributeInstance kbAttr = target.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) kbAttr.setBaseValue(0.0);

            if (target instanceof Player) {
                Player player = (Player) target;
                player.setWalkSpeed(0.2f);
                player.setCollidable(true);
            }
        }
    }

    // ==========================================
    // 4. BAY LƠ LỬNG AN TOÀN
    // ==========================================
    // Hàm cũ: Giữ nguyên để không gây lỗi cho các Kỹ năng khác đang xài
    public static void applyCustomHover(Player player, int durationTicks, float flySpeed, boolean safeFall) {
        applyCustomHover(player, durationTicks, flySpeed, safeFall, true); // Mặc định là cho phép tắt bằng Shift
    }

    // Hàm mới: Bổ sung tham số cancelOnSneak (Không tắt khi bấm Shift)
    public static void applyCustomHover(Player player, int durationTicks, float flySpeed, boolean safeFall, boolean cancelOnSneak) {
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flySpeed);

        if (safeFall) {
            player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(RPGCore.getInstance(), true));
        }

        if (!cancelOnSneak) {
            player.setMetadata("RPGCore_NoSneakCancel", new FixedMetadataValue(RPGCore.getInstance(), true));
        }

        player.setMetadata("RPGCore_Hovering", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + (durationTicks * 50L)));

        new BukkitRunnable() {
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
        }.runTaskTimer(RPGCore.getInstance(), 0L, 2L);
    }

    public static void removeCustomHover(Player player, boolean grantSafeFall) {
        if (player.hasMetadata("RPGCore_Hovering")) {
            player.removeMetadata("RPGCore_Hovering", RPGCore.getInstance());
            player.removeMetadata("RPGCore_NoSneakCancel", RPGCore.getInstance()); // Dọn cờ của Mage nếu có

            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            player.setFlySpeed(0.1f);

            // [CHỈNH SỬA]: Nếu grantSafeFall = true thì mới reset FallDistance và giữ Meta SafeFall
            if (grantSafeFall) {
                player.setFallDistance(0f);
            } else {
                // Nếu cạn mana hoặc out game -> Tước quyền miễn sát thương rơi
                player.removeMetadata("RPGCore_SafeFall", RPGCore.getInstance());
            }
        }
    }

    // Giữ lại hàm cũ (Overload) để không làm lỗi các class cũ đang gọi
    public static void removeCustomHover(Player player) {
        removeCustomHover(player, true);
    }

    // ==========================================
    // 5. SÁT THƯƠNG CHUẨN (TRUE DAMAGE)
    // ==========================================
    public static void applyTrueDamage(LivingEntity target, double amount) {
        if (target == null || target.isDead() || !target.isValid()) return;
        double currentHp = target.getHealth();
        double finalHp = Math.max(0, currentHp - amount);
        target.damage(0.0001);
        target.setHealth(finalHp);
    }

    // ==========================================
    // 6. QUẢN LÝ VẾT THƯƠNG SÂU (ANTI-HEAL)
    // ==========================================
    public static void applyAntiHeal(LivingEntity target, int durationTicks) {
        if (target == null) return;
        long expireTime = System.currentTimeMillis() + (durationTicks * 50L);
        target.setMetadata("STATUS_ANTI_HEAL", new FixedMetadataValue(RPGCore.getInstance(), expireTime));

        if (target instanceof Player) {
            Player p = (Player) target;
            p.sendMessage("§c§l[!] §fBạn đang bị §4Vết thương sâu§f, khả năng hồi phục bị đình trệ!");
            p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 0.5f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() && !isHealingBlocked(p)) {
                        p.sendMessage("§a§l[!] §fVết thương dần lành lại, khả năng hồi phục được khôi phục.");
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    }
                }
            }.runTaskLater(RPGCore.getInstance(), durationTicks);
        }
    }

    public static boolean isHealingBlocked(LivingEntity target) {
        // NẾU CÓ CỜ "TRUE HEAL" -> CHO PHÉP HỒI MÁU BẤT CHẤP ANTI-HEAL!
        if (target.hasMetadata("RPG_True_Heal_Priority")) {
            return false;
        }

        if (!target.hasMetadata("STATUS_ANTI_HEAL")) return false;
        long expire = target.getMetadata("STATUS_ANTI_HEAL").get(0).asLong();
        return System.currentTimeMillis() < expire;
    }

    /**
     * Giải trừ toàn bộ hiệu ứng xấu (Debuffs) và hỏa thiêu.
     * Không ảnh hưởng đến các chỉ số cộng thêm hoặc cơ chế tăng/giảm sát thương từ Metadata.
     */
    public static void cleanseNegativeEffects(LivingEntity target) {
        if (target == null) return;

        // Danh sách các PotionEffect xấu (Debuffs)
        PotionEffectType[] debuffs = {
                PotionEffectType.SLOWNESS,            // Chậm chạp
                PotionEffectType.MINING_FATIGUE,    // Đào chậm
                PotionEffectType.NAUSEA,       // Chóng mặt
                PotionEffectType.BLINDNESS,       // Mù lòa
                PotionEffectType.HUNGER,          // Đói khát
                PotionEffectType.WEAKNESS,        // Yếu ớt
                PotionEffectType.POISON,          // Thuốc độc
                PotionEffectType.WITHER,          // Héo mòn
                PotionEffectType.DARKNESS,        // Bóng tối (1.19+)
                PotionEffectType.UNLUCK,          // Xui xẻo
                PotionEffectType.BAD_OMEN         // Điềm gở
        };

        for (PotionEffectType type : debuffs) {
            if (target.hasPotionEffect(type)) {
                target.removePotionEffect(type);
            }
        }

        // Dập lửa ngay lập tức
        target.setFireTicks(0);

        // Xóa hiệu ứng Vết thương sâu (Anti-heal) do chính Manager này quản lý
        target.removeMetadata("STATUS_ANTI_HEAL", vn.minecraft.rPGCore.RPGCore.getInstance());
    }
    // ==========================================
    // [HÀM MỚI]: BUFF TỐC ĐỘ CHẠY THÔNG QUA STATS
    // ==========================================
    public static void applySpeed(Player p, int durationTicks, double speedPercent) {
        long expireTime = System.currentTimeMillis() + (durationTicks * 50L);

        p.setMetadata("STATUS_SPEED_PERCENT", new FixedMetadataValue(RPGCore.getInstance(), speedPercent));
        p.setMetadata("STATUS_SPEED_EXPIRE", new FixedMetadataValue(RPGCore.getInstance(), expireTime));

        // Cập nhật tốc chạy ngay lập tức
        RPGPlayer rpgP = RPGCore.getInstance().getPlayerManager().getRPGPlayer(p.getUniqueId());
        if (rpgP != null) rpgP.updateMoveSpeed();

        // Lên lịch gỡ Buff
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline() && p.hasMetadata("STATUS_SPEED_EXPIRE")) {
                    long currentExpire = p.getMetadata("STATUS_SPEED_EXPIRE").get(0).asLong();
                    if (System.currentTimeMillis() >= currentExpire) {
                        p.removeMetadata("STATUS_SPEED_PERCENT", RPGCore.getInstance());
                        p.removeMetadata("STATUS_SPEED_EXPIRE", RPGCore.getInstance());
                        if (rpgP != null) rpgP.updateMoveSpeed(); // Phục hồi tốc độ
                    }
                }
            }
        }.runTaskLater(RPGCore.getInstance(), durationTicks);
    }
    // ==========================================
    // [HÀM MỚI]: MIỄN SÁT THƯƠNG RƠI TẠM THỜI (DÙNG CHO KỸ NĂNG NẢY/LƯỚT)
    // ==========================================
    public static void grantTemporarySafeFall(Player player, int durationTicks) {
        // Cấp cờ an toàn (Hệ thống Event Listener của bạn sẽ đọc cờ này để hủy sát thương rơi)
        player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.hasMetadata("RPGCore_SafeFall")) {
                    player.setFallDistance(0f); // Reset khoảng cách rơi ảo trước khi gỡ cờ
                    player.removeMetadata("RPGCore_SafeFall", RPGCore.getInstance());
                }
            }
        }.runTaskLater(RPGCore.getInstance(), durationTicks);
    }
}