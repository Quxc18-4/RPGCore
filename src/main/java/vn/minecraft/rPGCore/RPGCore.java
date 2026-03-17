package vn.minecraft.rPGCore;

import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.commands.*;
import vn.minecraft.rPGCore.listeners.ComboListener;
import vn.minecraft.rPGCore.listeners.EquipmentListener;
import vn.minecraft.rPGCore.listeners.PlayerListener;
import vn.minecraft.rPGCore.listeners.ClassGUIListener;
import vn.minecraft.rPGCore.managers.*;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.skills.SkillManager;
import vn.minecraft.rPGCore.ui.*;
import vn.minecraft.rPGCore.player.tasks.RegenTask;

public final class RPGCore extends JavaPlugin {

    private static RPGCore instance;
    private PlayerManager playerManager;
    private BossBarManager bossBarManager;

    private BoredomManager boredomManager;
    private FoodBuffManager foodBuffManager;

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
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI();

        IngredientManager ingredientManager = new IngredientManager(this);
        CookingManager cookingManager = new CookingManager(this, ingredientManager);

        this.boredomManager = new BoredomManager(this);
        this.foodBuffManager = new FoodBuffManager(this);

        // [MỚI]: KHỞI TẠO MANAGER CHO NGÂN HÀNG & CỬA HÀNG
        CurrencyManager currencyManager = new CurrencyManager(this);
        ShopManager shopManager = new ShopManager(this, currencyManager);
        // [MỚI]: KHỞI TẠO BANK MANAGER
        BankManager bankManager = new BankManager(currencyManager);

        ArrowRecipeManager arrowRecipeManager = new ArrowRecipeManager(this);
        ArrowCraftingUI arrowCraftingUI = new ArrowCraftingUI(playerManager, arrowRecipeManager);
        AdminArrowCraftingUI adminArrowCraftingUI = new AdminArrowCraftingUI(arrowRecipeManager);

        RepairGUI repairGUI = new RepairGUI();

        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, bossBarManager), this);
        getServer().getPluginManager().registerEvents(new ClassGUIListener(playerManager, bossBarManager, classManager), this);
        getServer().getPluginManager().registerEvents(new EquipmentListener(playerManager), this);
        getServer().getPluginManager().registerEvents(repairGUI, this);
        getServer().getPluginManager().registerEvents(attrGUI, this);
        getServer().getPluginManager().registerEvents(new ComboListener(this, playerManager, skillManager), this);
        getServer().getPluginManager().registerEvents(skillUI, this);
        getServer().getPluginManager().registerEvents(arrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(adminArrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.listeners.DurabilityListener(playerManager), this);
        getServer().getPluginManager().registerEvents(blacksmithGUI, this);
        getServer().getPluginManager().registerEvents(cookingManager, this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.listeners.FoodListener(this, this.boredomManager, this.foodBuffManager), this);
        // [MỚI]: ĐĂNG KÝ BANK LISTENER
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.listeners.BankListener(this, bankManager, currencyManager, playerManager), this);
        // [MỚI]: ĐĂNG KÝ SỰ KIỆN CHO CỬA HÀNG (SHOP)
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.listeners.ShopListener(shopManager, currencyManager, playerManager), this);

        new RegenTask(playerManager, bossBarManager).runTaskTimer(this, 0L, 20L);

        getCommand("class").setExecutor(new ClassCommand(classManager, classGUI));
        getCommand("rpgadmin").setExecutor(new RPGAdminCommand(playerManager));
        getCommand("profile").setExecutor(new ProfileCommand(playerManager));
        getCommand("attr").setExecutor(new AttrCommand(attrGUI));
        getCommand("skill").setExecutor(new SkillCommand(skillUI));
        getCommand("binds").setExecutor(new BindsCommand(skillUI));
        getCommand("rpgclassadmin").setExecutor(new RPGClassAdminCommand(adminArrowCraftingUI));
        getCommand("arrow").setExecutor(new ArrowCommand(arrowRecipeManager));
        getCommand("ig").setExecutor(new ItemAdminCommand(ingredientManager));

        // [MỚI]: ĐĂNG KÝ LỆNH NGÂN HÀNG (/bal)
        if (this.getCommand("bal") != null) {
            this.getCommand("bal").setExecutor(new BalanceCommand(this, playerManager, currencyManager));
        } else {
            getLogger().warning("Khong the tim thay lenh /bal trong plugin.yml!");
        }

        if (this.getCommand("repairadmin") != null) {
            this.getCommand("repairadmin").setExecutor(new RepairAdminCommand());
        }
        if (this.getCommand("eq") != null) {
            this.getCommand("eq").setExecutor(new EquipmentCommand());
        } else {
            getLogger().warning("Khong the tim thay lenh /eq trong plugin.yml!");
        }
        if (this.getCommand("spawnnpc") != null) {
            this.getCommand("spawnnpc").setExecutor(new SpawnNPCCommand());
        }
        // ĐĂNG KÝ LỆNH DUMMY
        if (this.getCommand("dummy") != null) {
            this.getCommand("dummy").setExecutor(new vn.minecraft.rPGCore.commands.DummyCommand());
        }

        getLogger().info("RPGCore đã được bật thành công!");

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

    // [MỚI THÊM]: Mở cửa cho các Manager khác lấy dữ liệu người chơi
    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public FoodBuffManager getFoodBuffManager() {
        return this.foodBuffManager;
    }

    public BoredomManager getBoredomManager() {
        return this.boredomManager;
    }
}