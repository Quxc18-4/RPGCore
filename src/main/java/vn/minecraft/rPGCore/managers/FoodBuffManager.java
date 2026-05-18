package vn.minecraft.rPGCore.managers;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FoodBuffManager {

    public enum StatType {
        DMG, CDR, MOVE_SPEED, ATK_SPEED, ARMOR_PEN, REGEN, MAX_HP,
        LIFE_STEAL, CRIT_RATE, DROP_RATE
    }

    private static class ActiveBuff {
        double amount;
        long expiryTime;

        ActiveBuff(double amount, long expiryTime) {
            this.amount = amount;
            this.expiryTime = expiryTime;
        }
    }

    // Map lồng List để lưu nhiều Buff cùng lúc thay vì ghi đè
    private final Map<UUID, Map<StatType, List<ActiveBuff>>> playerBuffs = new HashMap<>();

    public FoodBuffManager(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map<StatType, List<ActiveBuff>> types : playerBuffs.values()) {
                    for (List<ActiveBuff> buffs : types.values()) {
                        buffs.removeIf(buff -> now > buff.expiryTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void applyBuff(UUID uuid, StatType type, double amount, int durationMinutes) {
        if (amount == 0) return;
        playerBuffs.putIfAbsent(uuid, new HashMap<>());
        playerBuffs.get(uuid).putIfAbsent(type, new ArrayList<>());

        long expiry = System.currentTimeMillis() + (durationMinutes * 60000L);
        // Thêm vào danh sách thay vì ghi đè
        playerBuffs.get(uuid).get(type).add(new ActiveBuff(amount, expiry));
    }

    public double getBonus(UUID uuid, StatType type) {
        if (!playerBuffs.containsKey(uuid)) return 0;

        List<ActiveBuff> buffs = playerBuffs.get(uuid).get(type);
        if (buffs == null || buffs.isEmpty()) return 0;

        double totalBonus = 0;
        long now = System.currentTimeMillis();

        // Cộng dồn toàn bộ Buff và Debuff đang có hiệu lực
        for (ActiveBuff buff : buffs) {
            if (now < buff.expiryTime) {
                totalBonus += buff.amount;
            }
        }
        return totalBonus;
    }

    // ===============================================
    // [MỚI]: HÀM XÓA TOÀN BỘ BUFF ĐỂ TRÁNH CỘNG DỒN LỖI
    // ===============================================
    public void clearAllBuffs(UUID uuid) {
        if (playerBuffs.containsKey(uuid)) {
            playerBuffs.get(uuid).clear();
        }
    }
}