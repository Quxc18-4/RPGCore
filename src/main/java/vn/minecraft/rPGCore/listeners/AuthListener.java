package vn.minecraft.rPGCore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import vn.minecraft.rPGCore.managers.AuthManager;

public class AuthListener implements Listener {
    private final AuthManager authManager;

    public AuthListener(AuthManager authManager) {
        this.authManager = authManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Tịch thu sạch sẽ túi đồ lúc mới vào (tránh bị hack nhìn trộm đồ)
        player.getInventory().clear();
        player.getEnderChest().clear();

        // 2. Ép họ phải đứng yên đọc thông báo
        player.sendMessage("§c§l[BẢO MẬT] §fVui lòng đăng nhập hoặc đăng ký để tiếp tục!");
        player.sendMessage("§7- Đăng nhập: /login <mật khẩu>");
        player.sendMessage("§7- Đăng ký: /register <mật khẩu> <email@gmail.com>");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Chỉ chặn di chuyển tọa độ (XYZ), cho phép xoay góc nhìn (Camera)
        if (!authManager.isLoggedIn(event.getPlayer())) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    // Chặn TẤT CẢ các hành vi tương tác khác
    @EventHandler public void onInteract(PlayerInteractEvent e) { if (!authManager.isLoggedIn(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onInvClick(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player && !authManager.isLoggedIn((Player) e.getWhoClicked())) e.setCancelled(true); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { if (!authManager.isLoggedIn(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent e) { if (e.getEntity() instanceof Player && !authManager.isLoggedIn((Player) e.getEntity())) e.setCancelled(true); }
    @EventHandler public void onBreak(BlockBreakEvent e) { if (!authManager.isLoggedIn(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (!authManager.isLoggedIn(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onChat(AsyncPlayerChatEvent e) {
        if (!authManager.isLoggedIn(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cBạn phải đăng nhập mới được chat!");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandProcess(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage().toLowerCase();

        // Cho phép 3 lệnh này đi qua
        if (cmd.startsWith("/login") || cmd.startsWith("/register") || cmd.startsWith("/forgotpw")) {
            return;
        }

        if (!authManager.isLoggedIn(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c§l[!] §cBạn phải đăng nhập để sử dụng lệnh!");
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Xóa trạng thái đăng nhập của người chơi khi họ thoát server
        authManager.logout(event.getPlayer());
    }
}