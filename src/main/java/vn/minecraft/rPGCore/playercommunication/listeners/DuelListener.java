package vn.minecraft.rPGCore.playercommunication.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.minecraft.rPGCore.RPGCore;

import java.util.UUID;

public class DuelListener implements Listener {
    private final RPGCore plugin;

    public DuelListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // CAN THIỆP GIẢM SÁT THƯƠNG ĐẤU TRƯỜNG (AN TOÀN)
    // Dùng HIGH thay vì HIGHEST để các lớp khiên ảo (Absorption)
    // hoặc các Event quan trọng khác vẫn có thể chạy sau nó nếu cần.
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDuelDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();

            // Nếu cả 2 đều đang ở trong trận Duel
            if (victim.hasMetadata("IN_DUEL") && attacker.hasMetadata("IN_DUEL")) {

                // Mẹo an toàn: Lấy sát thương hiện tại (đã qua các lớp tính toán trước)
                // và nhân nó với 0.2 (Tức là trừ đi 80%).
                // Việc này không hề can thiệp vào các logic của CombatManager
                // mà chỉ giảm lượng sát thương thô trước khi nó thực sự trừ máu.
                double currentDamage = event.getDamage();
                event.setDamage(currentDamage * 0.2);
            }
        }
    }

    // ==========================================
    // CAN THIỆP CÁI CHẾT (CHỐNG MẤT ĐỒ & EXP)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDuelDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();

        if (plugin.getDuelManager().isInDuel(loser)) {
            // Hủy rớt đồ và Không trừ EXP
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setDeathMessage(null); // Không báo cái chết lên toàn server

            UUID winnerId = plugin.getDuelManager().getOpponent(loser.getUniqueId());
            Player winner = Bukkit.getPlayer(winnerId);

            // Ép Hồi Sinh tại chỗ ngay lập tức (Chờ 2 ticks cho an toàn)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                loser.spigot().respawn();
                plugin.getDuelManager().endDuel(loser, winner, false);
            }, 2L);
        }
    }

    // ==========================================
    // BẮT SỰ KIỆN THOÁT GAME GIỮA CHỪNG QUY HÀNG
    // ==========================================
    @EventHandler
    public void onDuelQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        if (plugin.getDuelManager().isInDuel(quitter)) {
            UUID winnerId = plugin.getDuelManager().getOpponent(quitter.getUniqueId());
            Player winner = Bukkit.getPlayer(winnerId);

            if (winner != null && winner.isOnline()) {
                plugin.getDuelManager().endDuel(quitter, winner, true);
            } else {
                plugin.getDuelManager().restorePlayerAfterDuel(quitter);
            }
        }
    }
}