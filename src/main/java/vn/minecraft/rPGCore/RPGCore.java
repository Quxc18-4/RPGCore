package vn.minecraft.rPGCore;

import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.commands.*;
import vn.minecraft.rPGCore.listeners.ArcherListener;
import vn.minecraft.rPGCore.listeners.ComboListener;
import vn.minecraft.rPGCore.listeners.EquipmentListener; // Import thêm
import vn.minecraft.rPGCore.listeners.PlayerListener;
import vn.minecraft.rPGCore.listeners.ClassGUIListener;
import vn.minecraft.rPGCore.managers.ArrowRecipeManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.skills.SkillManager;
import vn.minecraft.rPGCore.ui.*;
import vn.minecraft.rPGCore.player.tasks.RegenTask;

public final class RPGCore extends JavaPlugin {

    private static RPGCore instance;
    private PlayerManager playerManager;
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {
        instance = this;
        ClassManager classManager = new ClassManager(this);
        classManager.loadClasses();

        playerManager = new PlayerManager(this, classManager);
        bossBarManager = new BossBarManager();
        ClassGUI classGUI = new ClassGUI();
        AttrGUI attrGUI = new AttrGUI(playerManager);
        SkillManager skillManager = new SkillManager(this);
        SkillUIManager skillUI = new SkillUIManager(playerManager, skillManager);

        ArrowRecipeManager arrowRecipeManager = new ArrowRecipeManager(this);
        ArrowCraftingUI arrowCraftingUI = new ArrowCraftingUI(playerManager, arrowRecipeManager);
        AdminArrowCraftingUI adminArrowCraftingUI = new AdminArrowCraftingUI(arrowRecipeManager);

        // ==========================================
        // KHỞI TẠO GUI SỬA CHỮA (MỚI)
        // ==========================================
        RepairGUI repairGUI = new RepairGUI();

        // 2. Đăng ký Listener
        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, bossBarManager, classManager), this);
        getServer().getPluginManager().registerEvents(new ClassGUIListener(playerManager, bossBarManager, classManager), this);
        getServer().getPluginManager().registerEvents(new ArcherListener(this, playerManager, arrowCraftingUI), this);

        // ĐĂNG KÝ LISTENER KIỂM TRA MẶC ĐỒ & CLASS
        getServer().getPluginManager().registerEvents(new EquipmentListener(playerManager), this);

        // Đăng ký GUI Sửa chữa
        getServer().getPluginManager().registerEvents(repairGUI, this);

        getServer().getPluginManager().registerEvents(attrGUI, this);
        getServer().getPluginManager().registerEvents(new ComboListener(this, playerManager, skillManager), this);
        getServer().getPluginManager().registerEvents(skillUI, this);
        getServer().getPluginManager().registerEvents(arrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(adminArrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.listeners.DurabilityListener(playerManager), this);

        // 3. Chạy Task
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

        // Đăng ký lệnh Lấy dụng cụ sửa chữa
        if (this.getCommand("repairadmin") != null) {
            this.getCommand("repairadmin").setExecutor(new RepairAdminCommand());
        }

        if (this.getCommand("eq") != null) {
            this.getCommand("eq").setExecutor(new EquipmentCommand());
        } else {
            getLogger().warning("Khong the tim thay lenh /eq trong plugin.yml!");
        }

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
        if (playerManager != null) {
            playerManager.saveAllPlayers();
        }
        getLogger().info("RPGCore da tat va luu du lieu an toan.");
    }

    public static RPGCore getInstance() {
        return instance;
    }
}