package vn.minecraft.rPGCore.managers;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlchemistBuffManager {

    private static class ActivePotionBuff {
        double amount;
        long expiryTime;

        ActivePotionBuff(double amount, long expiryTime) {
            this.amount = amount;
            this.expiryTime = expiryTime;
        }
    }

    // [ĐÃ SỬA] Đổi List thành Object đơn để GHI ĐÈ
    private final Map<UUID, Map<String, ActivePotionBuff>> playerBuffs = new HashMap<>();

    public AlchemistBuffManager(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map<String, ActivePotionBuff> types : playerBuffs.values()) {
                    types.entrySet().removeIf(entry -> now > entry.getValue().expiryTime);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void applyBuff(UUID uuid, String statName, double amount, int durationMinutes) {
        if (amount == 0 || durationMinutes <= 0) return;

        playerBuffs.putIfAbsent(uuid, new HashMap<>());
        long expiry = System.currentTimeMillis() + (durationMinutes * 60000L);

        // [CƠ CHẾ GHI ĐÈ]: Thuốc mới sẽ ghi đè hoàn toàn thuốc cũ cùng loại
        playerBuffs.get(uuid).put(statName, new ActivePotionBuff(amount, expiry));
    }

    public double getBonus(UUID uuid, String statName) {
        if (!playerBuffs.containsKey(uuid)) return 0;

        ActivePotionBuff buff = playerBuffs.get(uuid).get(statName);
        if (buff == null || System.currentTimeMillis() > buff.expiryTime) return 0;

        return buff.amount;
    }
}