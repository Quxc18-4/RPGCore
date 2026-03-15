package vn.minecraft.rPGCore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AssassinListener implements Listener {

    private final JavaPlugin plugin;

    public AssassinListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 1. Quái vật mất mục tiêu nếu Sát thủ tàng hình
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (player.hasMetadata("Assassin_Stealth")) {
                event.setCancelled(true); // Cấm quái nhìn thấy
            }
        }
    }

    // 2. Đòn đánh thường từ Tàng hình -> Phá tàng hình & Gây chí mạng tuyệt đối
    @EventHandler
    public void onStealthAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            if (player.hasMetadata("Assassin_Stealth")) {
                // Xóa trạng thái tàng hình
                player.removeMetadata("Assassin_Stealth", plugin);
                player.sendMessage("§c§l[!] Đòn đánh bất ngờ! Hiện hình!");

                // Hiển thị lại với người chơi khác
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showPlayer(plugin, player);
                }

                // Nếu bạn dùng Vanilla hit cho đòn chém thường, ta có thể buff dame trực tiếp ở đây.
                // Nếu dùng hệ thống CombatManager cho đòn đánh thường sau này, bạn sẽ pass biến `100.0` extraCrit vào đó.
                // Tạm thời buff x2 sát thương vanilla nếu dùng kiếm chém thẳng:
                if (event.getEntity() instanceof LivingEntity) {
                    event.setDamage(event.getDamage() * 2.0); // Mô phỏng Chí mạng cho Vanilla Hit
                    player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                }
            }
        }
    }
}