package vn.minecraft.rPGCore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;
    private final ClassManager classManager; // Thêm ClassManager

    public PlayerListener(PlayerManager playerManager, BossBarManager bossBarManager, ClassManager classManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
        this.classManager = classManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Lấy dữ liệu class "commoner" từ file yml đã được nạp
        RPGClass defaultClass = classManager.getClassById("commoner");

        // Chú ý: Cần truyền defaultClass vào constructor mới của RPGPlayer
        RPGPlayer rpgPlayer = new RPGPlayer(player, defaultClass);
        playerManager.addPlayer(player.getUniqueId(), rpgPlayer);

        bossBarManager.createBars(player, rpgPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bossBarManager.removeBars(player);
        playerManager.removePlayer(player.getUniqueId());
    }
}