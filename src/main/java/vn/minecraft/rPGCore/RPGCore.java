package vn.minecraft.rPGCore;

import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.commands.ClassCommand;
import vn.minecraft.rPGCore.listeners.PlayerListener;
import vn.minecraft.rPGCore.listeners.ClassGUIListener; // Thêm import này
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.ui.BossBarManager;
import vn.minecraft.rPGCore.player.tasks.RegenTask;
import vn.minecraft.rPGCore.ui.ClassGUI;


public final class RPGCore extends JavaPlugin {

    private PlayerManager playerManager;
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {
        // ... (khởi tạo ClassManager trước) ...
        ClassManager classManager = new ClassManager(this);
        classManager.loadClasses();

        // TRUYỀN `this` VÀ `classManager` VÀO
        playerManager = new PlayerManager(this, classManager);
        bossBarManager = new BossBarManager();
        ClassGUI classGUI = new ClassGUI();

        // 2. Đăng ký Listener
        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, bossBarManager, classManager), this);

        // THÊM DÒNG NÀY: Đăng ký sự kiện click chuột trong GUI Chọn Class
        getServer().getPluginManager().registerEvents(new ClassGUIListener(playerManager, bossBarManager, classManager), this);

        // 3. Chạy Task sau 0 ticks, lặp lại mỗi 20 ticks (1 giây)
        new RegenTask(playerManager, bossBarManager).runTaskTimer(this, 0L, 20L);

        // 4. Đăng ký lệnh
        getCommand("class").setExecutor(new ClassCommand(classManager, classGUI));

        getLogger().info("RPGCore da duoc bat thanh cong!");

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (playerManager.getRPGPlayer(player.getUniqueId()) == null) {
                vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = playerManager.loadPlayer(player);
                bossBarManager.createBars(player, rpgPlayer);
            }
        }
    }

    @Override
    public void onDisable() {
        // LƯU TẤT CẢ NGƯỜI CHƠI TRƯỚC KHI SERVER TẮT ĐỂ KHÔNG MẤT DATA
        if (playerManager != null) {
            playerManager.saveAllPlayers();
        }
        getLogger().info("RPGCore da tat va luu du lieu an toan.");
    }
}