package vn.minecraft.rPGCore;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.commands.*;
import vn.minecraft.rPGCore.cooking.CookingEngine;
import vn.minecraft.rPGCore.jobs.core.JobLeaderboard;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.JobRegister;
import vn.minecraft.rPGCore.jobs.events.*;
import vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI;
import vn.minecraft.rPGCore.listeners.*;
import vn.minecraft.rPGCore.managers.*;
import vn.minecraft.rPGCore.mounts.listeners.MountListener;
import vn.minecraft.rPGCore.mounts.managers.MountDataManager;
import vn.minecraft.rPGCore.mounts.managers.MountManager;
import vn.minecraft.rPGCore.mounts.ui.MountStableGUI;
import vn.minecraft.rPGCore.mounts.ui.MountStorageGUI;
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
    private SkillManager skillManager; // [THÊM DÒNG NÀY VÀO ĐÂY]
    private ItemManager itemManager; // [THÊM DÒNG NÀY]
    private FishingManager fishingManager;
    private BaitManager baitManager;
    private CurrencyManager currencyManager;
    private BackpackManager backpackManager;

    // [HỆ THỐNG THÚ CƯỠI]
    private MountManager mountManager;
    private MountDataManager mountDataManager;

    private ToxicityManager toxicityManager;
    private AlchemistBuffManager alchemistBuffManager;

    @Override
    public void onEnable() {
        instance = this;
        ClassManager classManager = new ClassManager(this);
        classManager.loadClasses();

        playerManager = new PlayerManager(this, classManager);
        bossBarManager = new BossBarManager();
        ClassGUI classGUI = new ClassGUI();
        AttrGUI attrGUI = new AttrGUI(playerManager);
        this.skillManager = new SkillManager(this);
        SkillUIManager skillUI = new SkillUIManager(playerManager, skillManager);
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI();
        // ==========================================
        // [MỚI]: KHỞI TẠO HỆ THỐNG TRANG SỨC (TRINKET)
        // ==========================================
        TrinketManager trinketManager = new TrinketManager();
        TrinketForgeGUI trinketForgeGUI = new TrinketForgeGUI(this.itemManager);

        this.itemManager = new ItemManager(this);

        // Khởi tạo Engine và Cooking Manager ĐÚNG THỨ TỰ
        vn.minecraft.rPGCore.cooking.CookingEngine cookingEngine = new vn.minecraft.rPGCore.cooking.CookingEngine();
        CookingManager cookingManager = new CookingManager(this, this.itemManager, cookingEngine);

        this.boredomManager = new BoredomManager(this);
        this.foodBuffManager = new FoodBuffManager(this);

        // [MỚI]: KHỞI TẠO HỆ THỐNG DƯỢC SƯ
        this.toxicityManager = new ToxicityManager(this);
        this.alchemistBuffManager = new AlchemistBuffManager(this);
        BrewingManager brewingManager = new BrewingManager(this, this.itemManager);

        //SchemaManager.loadSchemas();

        // [MỚI]: KHỞI TẠO MANAGER CHO NGÂN HÀNG & CỬA HÀNG
        this.currencyManager = new CurrencyManager(this);
        ShopManager shopManager = new ShopManager(this, currencyManager);
        // [MỚI]: KHỞI TẠO BANK MANAGER
        BankManager bankManager = new BankManager(currencyManager);

        ArrowRecipeManager arrowRecipeManager = new ArrowRecipeManager(this);
        ArrowCraftingUI arrowCraftingUI = new ArrowCraftingUI(playerManager, arrowRecipeManager);
        AdminArrowCraftingUI adminArrowCraftingUI = new AdminArrowCraftingUI(arrowRecipeManager);
        ElementRotationGUI elementRotationGUI = new ElementRotationGUI();
        CropManager cropManager = new CropManager(this, this.itemManager);

        this.fishingManager = new FishingManager(this, this.itemManager);
        // [MỚI]: Khởi tạo BaitManager
        this.baitManager = new BaitManager(this, this.itemManager);

        this.mountManager = new MountManager(this);
        this.mountDataManager = new MountDataManager(this, this.mountManager);

        this.backpackManager = new BackpackManager(this);

        JobRegister.registerAllJobs();
        JobLeaderboard.updateLeaderboards();

        RepairGUI repairGUI = new RepairGUI();

        MountStableGUI mountStableGUI = new MountStableGUI(this);
        MountStorageGUI mountStorageGUI = new MountStorageGUI();

        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, bossBarManager), this);
        getServer().getPluginManager().registerEvents(new ClassGUIListener(playerManager, bossBarManager, classManager), this);
        getServer().getPluginManager().registerEvents(new EquipmentListener(playerManager), this);
        getServer().getPluginManager().registerEvents(repairGUI, this);
        getServer().getPluginManager().registerEvents(attrGUI, this);
        getServer().getPluginManager().registerEvents(new ComboListener(this, playerManager, skillManager), this);
        getServer().getPluginManager().registerEvents(skillUI, this);
        getServer().getPluginManager().registerEvents(arrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(adminArrowCraftingUI, this);
        getServer().getPluginManager().registerEvents(new DurabilityListener(playerManager), this);
        getServer().getPluginManager().registerEvents(blacksmithGUI, this);
        // Đăng ký Listener Trang Sức
        getServer().getPluginManager().registerEvents(trinketManager, this);
        getServer().getPluginManager().registerEvents(trinketForgeGUI, this);
        getServer().getPluginManager().registerEvents(cookingManager, this);
        getServer().getPluginManager().registerEvents(new FoodListener(this, this.boredomManager, this.foodBuffManager), this);
        // ĐĂNG KÝ LISTENER DƯỢC SƯ
        //getServer().getPluginManager().registerEvents(brewingManager, this);
        getServer().getPluginManager().registerEvents(new PotionListener(this, brewingManager), this);
        // [MỚI]: ĐĂNG KÝ BANK LISTENER
        getServer().getPluginManager().registerEvents(new BankListener(this, bankManager, currencyManager, playerManager), this);
        // [MỚI]: ĐĂNG KÝ SỰ KIỆN CHO CỬA HÀNG (SHOP)
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, currencyManager, playerManager), this);
        getServer().getPluginManager().registerEvents(elementRotationGUI, this);
        // Đăng ký Listener cho Hệ thống Vật phẩm tiêu hao (Bình máu, Tinh thể...)
        getServer().getPluginManager().registerEvents(new ConsumableListener(this.itemManager, this.boredomManager, cookingManager), this);
        getServer().getPluginManager().registerEvents(new JobManager.JobDataListener(), this);
        getServer().getPluginManager().registerEvents(new JobSelectionGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.QuestBoardGUI(), this);
        getServer().getPluginManager().registerEvents(new QuestListener(), this);
        getServer().getPluginManager().registerEvents(new CropProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.JobShopGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.JobSkillTreeGUI(), this);
        // Đăng ký Listener cho GUI Xếp hạng
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.JobLeaderboardGUI(), this);
        getServer().getPluginManager().registerEvents(cropManager, this);
        getServer().getPluginManager().registerEvents(new FishingListener(this.fishingManager, this.itemManager), this);
        getServer().getPluginManager().registerEvents(new BaitCrafterGUI(this.itemManager, this.baitManager), this);
        getServer().getPluginManager().registerEvents(new MerchantListener(this), this);
        // [MỚI CHÈN]: Đăng ký bộ thu hoạch thảo dược tự nhiên
        getServer().getPluginManager().registerEvents(new HerbHarvestListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(new AlchemistToolListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(new LostRecipeListener(), this);
        // Đăng ký Listener
        getServer().getPluginManager().registerEvents(this.mountDataManager, this); // Bắt sự kiện Join/Quit để load JSON
        getServer().getPluginManager().registerEvents(mountStableGUI, this);
        getServer().getPluginManager().registerEvents(mountStorageGUI, this);
        getServer().getPluginManager().registerEvents(new MountListener(this.mountManager, mountStorageGUI), this);
        getServer().getPluginManager().registerEvents(new WaterFetchListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(new TrinketMenuGUI(), this);
        getServer().getPluginManager().registerEvents(new TrinketRandomCraftGUI(), this);

        new RegenTask(playerManager, bossBarManager).runTaskTimer(this, 0L, 20L);

        // Chạy bộ đếm Reset Nhiệm vụ mỗi 5 phút (6000 ticks).
        // Nếu bạn muốn đúng 1 tiếng thì đổi số 6000 thành 72000 nhé!
        new vn.minecraft.rPGCore.jobs.core.JobResetTask().runTaskTimerAsynchronously(this, 6000L, 6000L);

        getCommand("class").setExecutor(new ClassCommand(classManager, classGUI));
        getCommand("rpgadmin").setExecutor(new RPGAdminCommand(playerManager));
        getCommand("profile").setExecutor(new ProfileCommand(playerManager));
        getCommand("attr").setExecutor(new AttrCommand(attrGUI));
        getCommand("skill").setExecutor(new SkillCommand(skillUI));
        getCommand("binds").setExecutor(new BindsCommand(skillUI));
        getCommand("rpgclassadmin").setExecutor(new RPGClassAdminCommand(adminArrowCraftingUI));
        getCommand("arrow").setExecutor(new ArrowCommand(arrowRecipeManager));
        getCommand("ig").setExecutor(new ItemAdminCommand(this.itemManager));
        getCommand("job").setExecutor(new vn.minecraft.rPGCore.jobs.commands.JobCommand());

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
            this.getCommand("dummy").setExecutor(new DummyCommand());
        }

        // Đăng ký lệnh /h
        if (this.getCommand("h") != null) {
            this.getCommand("h").setExecutor(new vn.minecraft.rPGCore.mounts.commands.MountCommand(this.mountManager, mountStableGUI));
        } else {
            getLogger().warning("Khong the tim thay lenh /h trong plugin.yml!");
        }

        if (this.getCommand("tforge") != null) {
            this.getCommand("tforge").setExecutor(trinketForgeGUI);
        }

        getLogger().info("RPGCore đã được bật thành công!");

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (playerManager.getRPGPlayer(player.getUniqueId()) == null) {
                vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = playerManager.loadPlayer(player);
                bossBarManager.createBars(player, rpgPlayer);
            }
        }

        // [Dự phòng]: Load Data cho những người đang online (Nếu dùng lệnh /reload)
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            vn.minecraft.rPGCore.jobs.core.JobManager.loadPlayerData(p.getUniqueId());
        }

        // Setup lặp lại mỗi 2 giờ (2 giờ = 120 phút = 7200 giây = 144,000 Ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                vn.minecraft.rPGCore.jobs.core.JobLeaderboard::updateLeaderboards,
                144000L, 144000L);
    }

    @Override
    public void onDisable() {
        if (playerManager != null) {
            playerManager.saveAllPlayers();
        }
        // [MỚI]: Lưu toàn bộ dữ liệu Nghề Phụ
        vn.minecraft.rPGCore.jobs.core.JobManager.saveAll();

        // =========================================================
        // [HỆ THỐNG THÚ CƯỠI]: LƯU JSON VÀ THU HỒI NGỰA KHI TẮT SERVER
        // =========================================================
        if (this.mountDataManager != null) {
            this.mountDataManager.saveAll();
        }
        if (this.mountManager != null) {
            this.mountManager.despawnAllActiveMounts();
        }
        // =========================================================

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

    public SkillManager getSkillManager() { return this.skillManager; }

    public vn.minecraft.rPGCore.ui.BossBarManager getBossBarManager() {
        return this.bossBarManager;
    }

    public ItemManager getItemManager() {
        return this.itemManager;
    }

    // 4. THÊM HÀM GETTER NÀY NỮA
    public FishingManager getFishingManager() {
        return fishingManager;
    }
    // [MỚI]: Thêm Getter cho BaitManager
    public BaitManager getBaitManager() {
        return baitManager;
    }

    // Thêm xuống cuối cùng của file RPGCore.java
    public vn.minecraft.rPGCore.mounts.managers.MountManager getMountManager() {
        return this.mountManager;
    }

    public vn.minecraft.rPGCore.mounts.managers.MountDataManager getMountDataManager() {
        return this.mountDataManager;
    }
    public CurrencyManager getCurrencyManager() {
        return this.currencyManager;
    }
    // Thêm hàm Getter xuống dưới cùng:
    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    public vn.minecraft.rPGCore.managers.ToxicityManager getToxicityManager() {
        return this.toxicityManager;
    }

    public vn.minecraft.rPGCore.managers.AlchemistBuffManager getAlchemistBuffManager() {
        return this.alchemistBuffManager;
    }
}