package vn.minecraft.rPGCore.managers;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class FoodBuffManager {

    public enum StatType {
        DMG, CDR, MOVE_SPEED, ATK_SPEED, ARMOR_PEN, REGEN, MAX_HP
    }

    private static class ActiveBuff {
        double amount;
        long expiryTime;

        ActiveBuff(double amount, long expiryTime) {
            this.amount = amount;
            this.expiryTime = expiryTime;
        }
    }

    // Map lưu trữ: UUID -> (Loại Stat -> Buff)
    private final Map<UUID, Map<StatType, ActiveBuff>> playerBuffs = new HashMap<>();

    public FoodBuffManager(JavaPlugin plugin) {
        // Vòng lặp dọn dẹp Buff hết hạn mỗi giây
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map<StatType, ActiveBuff> buffs : playerBuffs.values()) {
                    buffs.entrySet().removeIf(entry -> now > entry.getValue().expiryTime);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void applyBuff(UUID uuid, StatType type, double amount, int durationMinutes) {
        if (amount == 0) return;
        playerBuffs.putIfAbsent(uuid, new HashMap<>());
        long expiry = System.currentTimeMillis() + (durationMinutes * 60000L);

        // Ghi đè buff mới (có thể code cộng dồn tùy ý, nhưng ghi đè hợp lý với đồ ăn hơn)
        playerBuffs.get(uuid).put(type, new ActiveBuff(amount, expiry));
    }

    // Các hàm này sẽ được gọi ở hệ thống Chiến đấu / Profile của bạn
    public double getBonus(UUID uuid, StatType type) {
        if (!playerBuffs.containsKey(uuid)) return 0;
        ActiveBuff buff = playerBuffs.get(uuid).get(type);
        if (buff != null && System.currentTimeMillis() < buff.expiryTime) {
            return buff.amount;
        }
        return 0;
    }
}