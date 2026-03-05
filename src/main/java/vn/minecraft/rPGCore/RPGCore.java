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
        // 1. Khởi tạo các hệ thống
        playerManager = new PlayerManager();
        bossBarManager = new BossBarManager();
        ClassManager classManager = new ClassManager(this);
        classManager.loadClasses(); // Đọc dữ liệu từ file classes.yml lên bộ nhớ
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
    }

    @Override
    public void onDisable() {
        // Có thể dọn dẹp các thứ ở đây nếu cần thiết
        getLogger().info("RPGCore da tat.");
    }
}