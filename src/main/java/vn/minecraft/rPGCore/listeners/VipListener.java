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

    // 2. Định dạng tiền tố [ADMIN] / [VIP] + [lvl.xx] + <Tên> khi chat
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 1. Xác định huy hiệu vai trò ([ADMIN] hoặc [VIP])
        String rolePrefix = "";
        if (player.isOp() || player.hasPermission("rpgcore.admin")) {
            rolePrefix = "§c§l[ADMIN] ";
        } else if (vipManager.isOnlineVip(player.getUniqueId())) {
            rolePrefix = "§6§l[VIP] ";
        }

        // 2. Lấy cấp độ hiện tại của nhân vật đang chơi
        int level = 1;
        vn.minecraft.rPGCore.RPGCore core = vn.minecraft.rPGCore.RPGCore.getInstance();
        if (core != null && core.getPlayerManager() != null) {
            vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = core.getPlayerManager().getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) {
                level = rpgPlayer.getLevel();
            }
        }

        // 3. Gán format chat: [VIP|ADMIN] [lvl.xx] <Kuuhaku> nội dung
        // Lưu ý: Bắt buộc dùng %2$s cho nội dung chat để tránh lỗi khi người chơi gõ ký tự '%'
        String levelTag = "§7[§blvl." + level + "§7] ";
        String nameTag = "§f<" + player.getName() + "> §f";
        event.setFormat(rolePrefix + levelTag + nameTag + "%2$s");
    }

    // 3. Dọn cache RAM khi thoát game
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        vipManager.removeOnlineVip(event.getPlayer().getUniqueId());
    }
}