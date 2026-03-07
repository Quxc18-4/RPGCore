package vn.minecraft.rPGCore;

import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.commands.*;
import vn.minecraft.rPGCore.listeners.ArcherListener;
import vn.minecraft.rPGCore.listeners.ComboListener;
import vn.minecraft.rPGCore.listeners.PlayerListener;
import vn.minecraft.rPGCore.listeners.ClassGUIListener; // Thêm import này
import vn.minecraft.rPGCore.managers.ArrowRecipeManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.skills.SkillManager;
import vn.minecraft.rPGCore.ui.*;
import vn.minecraft.rPGCore.player.tasks.RegenTask;


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
        AttrGUI attrGUI = new AttrGUI(playerManager);
        SkillManager skillManager = new SkillManager(this);
        SkillUIManager skillUI = new SkillUIManager(playerManager, skillManager);

        // ==========================================
        // KHỞI TẠO HỆ THỐNG CHẾ TẠO MŨI TÊN (MỚI)
        // ==========================================
        ArrowRecipeManager arrowRecipeManager = new ArrowRecipeManager(this);
        ArrowCraftingUI arrowCraftingUI = new ArrowCraftingUI(playerManager, arrowRecipeManager); // <--- Truyền thêm arrowRecipeManager vào đây
        AdminArrowCraftingUI adminArrowCraftingUI = new AdminArrowCraftingUI(arrowRecipeManager);


        // 2. Đăng ký Listener
        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, bossBarManager, classManager), this);

        // THÊM DÒNG NÀY: Đăng ký sự kiện click chuột trong GUI Chọn Class, và các thứ liên quan tới class
        getServer().getPluginManager().registerEvents(new ClassGUIListener(playerManager, bossBarManager, classManager), this);
        getServer().getPluginManager().registerEvents(new ArcherListener(this, playerManager, arrowCraftingUI), this);

        //Đăng ký sự kiện cho attribute point
        getServer().getPluginManager().registerEvents(attrGUI, this);

        //Đăng ký sự kiện combo listener và lệnh /skill /bind
        getServer().getPluginManager().registerEvents(new ComboListener(this, playerManager, skillManager), this);
        getServer().getPluginManager().registerEvents(skillUI, this);

        // ==========================================
        // ĐĂNG KÝ LISTENER CHO GIAO DIỆN CHẾ TẠO (MỚI)
        // ==========================================
        getServer().getPluginManager().registerEvents(arrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(adminArrowCraftingUI, this);

        // 3. Chạy Task sau 0 ticks, lặp lại mỗi 20 ticks (1 giây)
        new RegenTask(playerManager, bossBarManager).runTaskTimer(this, 0L, 20L);

        // 4. Đăng ký lệnh
        getCommand("class").setExecutor(new ClassCommand(classManager, classGUI));
        getCommand("rpgadmin").setExecutor(new RPGAdminCommand(playerManager));
        getCommand("profile").setExecutor(new ProfileCommand(playerManager));
        getCommand("attr").setExecutor(new AttrCommand(attrGUI));
        getCommand("skill").setExecutor(new SkillCommand(skillUI));
        getCommand("binds").setExecutor(new BindsCommand(skillUI));
        getCommand("rpgclassadmin").setExecutor(new RPGClassAdminCommand(adminArrowCraftingUI));
        getCommand("arrow").setExecutor(new ArrowCommand(arrowRecipeManager));

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