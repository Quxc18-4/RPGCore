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

    // 1. Thay đổi bên trong hàm startMetabolism:
    private void startMetabolism(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    double baseReduction = 10.0;

                    // [NHÚNG LOGIC]: Tự động scale % đào thải dựa trên Level Dược Sư
                    double decayBonus = vn.minecraft.rPGCore.jobs.types.AlchemistJob.getToxDecayBonus(player);
                    baseReduction *= (1.0 + (decayBonus / 100.0));

                    if (toxicityPoints.containsKey(uuid)) {
                        double current = toxicityPoints.get(uuid);
                        double newPoints = Math.max(0, current - baseReduction);
                        toxicityPoints.put(uuid, newPoints);
                    }

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

    // 2. Thay đổi bên trong hàm addToxicity:
    public void addToxicity(UUID uuid, double amount) {
        Player player = Bukkit.getPlayer(uuid);
        double finalAmount = amount;

        if (player != null) {
            // [NHÚNG LOGIC]: Ultimate 2 Miễn nhiễm hoàn toàn
            if (vn.minecraft.rPGCore.jobs.types.AlchemistJob.hasUltimate(player, "ALCH_ULT_2")) {
                if (Math.random() <= 0.50) {
                    player.sendMessage("§a§l[Kháng Thể Độc Tố] §fCơ thể bạn đã tự động trung hòa hoàn toàn độc tính từ thuốc!");
                    return; // Không tăng bất kỳ điểm sốc thuốc nào
                }
            }

            // [NHÚNG LOGIC]: Nội tại 3 Giảm điểm gánh chịu
            double toxResist = vn.minecraft.rPGCore.jobs.types.AlchemistJob.getToxResistance(player);
            finalAmount = amount * (1.0 - (toxResist / 100.0));
        }

        double current = toxicityPoints.getOrDefault(uuid, 0.0);
        toxicityPoints.put(uuid, Math.min(100.0, current + finalAmount));
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