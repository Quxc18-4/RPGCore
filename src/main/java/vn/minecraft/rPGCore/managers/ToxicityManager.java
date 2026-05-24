package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToxicityManager {

    // Điểm Sốc Thuốc (0 - 100). Đạt 100 = Giảm 50% hiệu quả mọi buff thuốc
    private final Map<UUID, Double> toxicityPoints = new HashMap<>();

    // Stack lờn Mana (Mỗi lần nốc bình Mana +1 stack). Giảm sát thương phép.
    private final Map<UUID, Integer> manaSickness = new HashMap<>();

    public ToxicityManager(JavaPlugin plugin) {
        startMetabolism(plugin);
    }

    // Vòng lặp đào thải độc tố (Chạy mỗi 1 phút = 1200 ticks)
    private void startMetabolism(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    // Giảm 10 điểm Sốc Thuốc mỗi phút
                    if (toxicityPoints.containsKey(uuid)) {
                        double current = toxicityPoints.get(uuid);
                        double newPoints = Math.max(0, current - 10.0);
                        toxicityPoints.put(uuid, newPoints);
                    }

                    // Giảm 1 stack Mana Sickness mỗi phút
                    if (manaSickness.containsKey(uuid)) {
                        int stacks = manaSickness.get(uuid);
                        if (stacks > 0) {
                            manaSickness.put(uuid, stacks - 1);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    public void addToxicity(UUID uuid, double amount) {
        double current = toxicityPoints.getOrDefault(uuid, 0.0);
        toxicityPoints.put(uuid, Math.min(100.0, current + amount));
    }

    public boolean isOverdosed(UUID uuid) {
        // Nếu Độc tính > 80, bị coi là Sốc thuốc
        return toxicityPoints.getOrDefault(uuid, 0.0) >= 80.0;
    }

    public void addManaSickness(UUID uuid) {
        int current = manaSickness.getOrDefault(uuid, 0);
        manaSickness.put(uuid, Math.min(5, current + 1)); // Tối đa 5 stacks
    }

    // Hàm để RPGPlayer gọi: Trả về % sát thương phép bị giảm (Ví dụ: 3 stack = giảm 30%)
    public double getManaSicknessDebuff(UUID uuid) {
        int stacks = manaSickness.getOrDefault(uuid, 0);
        return stacks * 0.10; // Mỗi stack giảm 10%
    }
}