package vn.minecraft.rPGCore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.minecraft.rPGCore.managers.VipManager;

public class VipListener implements Listener {

    private final VipManager vipManager;

    public VipListener(VipManager vipManager) {
        this.vipManager = vipManager;
    }

    // 1. Đặc quyền vào server kể cả khi đầy slot
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
            if (vipManager.checkAndSyncVip(event.getPlayer().getUniqueId())) {
                event.allow();
            }
        }
    }

    // 2. Hiển thị tiền tố [VIP] khi chat
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (vipManager.isOnlineVip(player.getUniqueId())) {
            String currentDisplay = player.getDisplayName();
            if (!currentDisplay.contains("[VIP]")) {
                player.setDisplayName(VipManager.VIP_PREFIX + player.getName());
            }
        }
    }

    // 3. Dọn cache RAM khi thoát game
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        vipManager.removeOnlineVip(event.getPlayer().getUniqueId());
    }
}