package vn.minecraft.rPGCore;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.auctionhouse.commands.AuctionCommand;
import vn.minecraft.rPGCore.auctionhouse.listeners.AuctionGUIListener;
import vn.minecraft.rPGCore.auctionhouse.managers.AuctionManager;
import vn.minecraft.rPGCore.auctionhouse.tasks.AuctionTaxTask;
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
import vn.minecraft.rPGCore.database.MongoManager;

public final class RPGCore extends JavaPlugin {

    private static RPGCore instance;
    private PlayerManager playerManager;
    private BossBarManager bossBarManager;

    private BoredomManager boredomManager;
    private FoodBuffManager foodBuffManager;
    private SkillManager skillManager;
    private ItemManager itemManager;
    private FishingManager fishingManager;
    private BaitManager baitManager;
    private CurrencyManager currencyManager;
    private BackpackManager backpackManager;
    private ShopManager shopManager; // [THÊM DÒNG NÀY]

    private MountManager mountManager;
    private MountDataManager mountDataManager;

    private ToxicityManager toxicityManager;
    private AlchemistBuffManager alchemistBuffManager;
    private NPCManager npcManager;
    private GlobalEconomyManager globalEconomyManager;

    private RewardManager rewardManager;
    private AuctionManager auctionManager;
    private AuctionGUIListener auctionGUIListener;

    @Override
    public void onEnable() {
        instance = this;
        MongoManager.init(this);
        AuthManager authManager = new AuthManager(this);
        ClassManager classManager = new ClassManager(this);
        classManager.loadClasses();

        playerManager = new PlayerManager(this, classManager);
        bossBarManager = new BossBarManager();
        ClassGUI classGUI = new ClassGUI();
        AttrGUI attrGUI = new AttrGUI(playerManager);
        this.skillManager = new SkillManager(this);
        SkillUIManager skillUI = new SkillUIManager(playerManager, skillManager);
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI();
        TrinketManager trinketManager = new TrinketManager();
        this.itemManager = new ItemManager(this);
        TrinketForgeGUI trinketForgeGUI = new TrinketForgeGUI(this.itemManager);

        FixedTrinketManager.getInstance().loadAll();
        FixedEquipmentManager.getInstance().loadAll();

        CookingEngine cookingEngine = new CookingEngine();
        CookingManager cookingManager = new CookingManager(this, this.itemManager, cookingEngine);

        this.boredomManager = new BoredomManager(this);
        this.foodBuffManager = new FoodBuffManager(this);

        this.toxicityManager = new ToxicityManager(this);
        this.alchemistBuffManager = new AlchemistBuffManager(this);
        BrewingManager brewingManager = new BrewingManager(this, this.itemManager);

        this.currencyManager = new CurrencyManager(this);
        this.globalEconomyManager = new GlobalEconomyManager(this);
        this.shopManager = new ShopManager(this, currencyManager); // [SỬA LẠI DÒNG NÀY]
        BankManager bankManager = new BankManager(currencyManager);
        this.npcManager = new vn.minecraft.rPGCore.managers.NPCManager(this);

        ArrowRecipeManager arrowRecipeManager = new ArrowRecipeManager(this);
        ArrowCraftingUI arrowCraftingUI = new ArrowCraftingUI(playerManager, arrowRecipeManager);
        AdminArrowCraftingUI adminArrowCraftingUI = new AdminArrowCraftingUI(arrowRecipeManager);
        ElementRotationGUI elementRotationGUI = new ElementRotationGUI();
        CropManager cropManager = new CropManager(this, this.itemManager);

        this.fishingManager = new FishingManager(this, this.itemManager);
        this.baitManager = new BaitManager(this, this.itemManager);

        this.mountManager = new MountManager(this);
        this.mountDataManager = new MountDataManager(this, this.mountManager);

        this.backpackManager = new BackpackManager(this);
        this.rewardManager = new RewardManager(this);
        this.auctionManager = new AuctionManager(this);
        this.auctionGUIListener = new AuctionGUIListener(this);

        JobRegister.registerAllJobs();
        JobLeaderboard.updateLeaderboards();

        RepairGUI repairGUI = new RepairGUI();

        MountStableGUI mountStableGUI = new MountStableGUI(this);
        MountStorageGUI mountStorageGUI = new MountStorageGUI();

        getServer().getPluginManager().registerEvents(new AuthListener(authManager), this);
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

        getServer().getPluginManager().registerEvents(new EquipmentMenuGUI(), this);
        getServer().getPluginManager().registerEvents(new EquipmentStrongboxGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.ui.EquipmentCategoryGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.ui.EquipmentRecipeCraftGUI(), this);

        getServer().getPluginManager().registerEvents(trinketManager, this);
        getServer().getPluginManager().registerEvents(trinketForgeGUI, this);
        getServer().getPluginManager().registerEvents(cookingManager, this);
        getServer().getPluginManager().registerEvents(new FoodListener(this, this.boredomManager, this.foodBuffManager), this);
        getServer().getPluginManager().registerEvents(new PotionListener(this, brewingManager), this);
        getServer().getPluginManager().registerEvents(new BankListener(this, bankManager, currencyManager, playerManager), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, currencyManager), this);
        getServer().getPluginManager().registerEvents(elementRotationGUI, this);
        getServer().getPluginManager().registerEvents(new ConsumableListener(this, this.itemManager, this.boredomManager, cookingManager), this);
        getServer().getPluginManager().registerEvents(new JobManager.JobDataListener(), this);
        getServer().getPluginManager().registerEvents(new JobSelectionGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.QuestBoardGUI(), this);
        getServer().getPluginManager().registerEvents(new QuestListener(), this);
        getServer().getPluginManager().registerEvents(new CropProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.JobShopGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.JobSkillTreeGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.jobs.gui.JobLeaderboardGUI(), this);
        getServer().getPluginManager().registerEvents(cropManager, this);
        getServer().getPluginManager().registerEvents(new FishingListener(this.fishingManager, this.itemManager), this);
        getServer().getPluginManager().registerEvents(new BaitCrafterGUI(this.itemManager, this.baitManager), this);
        getServer().getPluginManager().registerEvents(new MerchantListener(this), this);
        getServer().getPluginManager().registerEvents(new HerbHarvestListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(new AlchemistToolListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(new LostRecipeListener(), this);
        getServer().getPluginManager().registerEvents(this.mountDataManager, this);
        getServer().getPluginManager().registerEvents(mountStableGUI, this);
        getServer().getPluginManager().registerEvents(mountStorageGUI, this);
        getServer().getPluginManager().registerEvents(new MountListener(this.mountManager, mountStorageGUI), this);
        getServer().getPluginManager().registerEvents(new WaterFetchListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(new TrinketMenuGUI(), this);
        getServer().getPluginManager().registerEvents(new TrinketRandomCraftGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.ui.TrinketRecipeCraftGUI(), this);
        getServer().getPluginManager().registerEvents(new vn.minecraft.rPGCore.skills.bard.BardCoreGUI(), this);
        getServer().getPluginManager().registerEvents(new RewardListener(this), this);
        getServer().getPluginManager().registerEvents(this.auctionGUIListener, this);

        new RegenTask(playerManager, bossBarManager).runTaskTimer(this, 0L, 20L);

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

        AuthCommand authCmd = new AuthCommand(authManager);
        getCommand("login").setExecutor(authCmd);
        getCommand("register").setExecutor(authCmd);
        getCommand("forgotpw").setExecutor(authCmd);

        RewardCommand rewardCmd = new RewardCommand(this);
        getCommand("kit").setExecutor(rewardCmd);
        getCommand("kit").setTabCompleter(rewardCmd);      // THÊM DÒNG NÀY
        getCommand("adminkit").setExecutor(rewardCmd);
        getCommand("adminkit").setTabCompleter(rewardCmd); // THÊM DÒNG NÀY
        getCommand("diemdanh").setExecutor(rewardCmd); // THÊM DÒNG NÀY

        AuctionCommand ahCmd = new AuctionCommand(this);
        getCommand("ah").setExecutor(ahCmd);
        getCommand("ahc").setExecutor(ahCmd);

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
            this.getCommand("spawnnpc").setExecutor(new SpawnNPCCommand(this));
        }
        if (this.getCommand("dummy") != null) {
            this.getCommand("dummy").setExecutor(new DummyCommand());
        }
        if (this.getCommand("h") != null) {
            this.getCommand("h").setExecutor(new vn.minecraft.rPGCore.mounts.commands.MountCommand(this.mountManager, mountStableGUI));
        } else {
            getLogger().warning("Khong the tim thay lenh /h trong plugin.yml!");
        }
        if (this.getCommand("tforge") != null) {
            this.getCommand("tforge").setExecutor(trinketForgeGUI);
        }

        vn.minecraft.rPGCore.classes.impl.bard.BardGlobalManager.startEngine();

        getLogger().info("RPGCore đã được bật thành công!");

        // ==========================================
        // [ĐÃ FIX]: CƠ CHẾ AN TOÀN KHI DÙNG LỆNH /reload
        // Đuổi toàn bộ người chơi đang online ra sảnh để họ Join và Login lại từ đầu
        // Chống tuyệt đối lỗi bất đồng bộ Dữ Liệu và vượt tường lửa Auth!
        // ==========================================
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer("§c§lHỆ THỐNG CẬP NHẬT\n\n§fServer vừa được tải lại (Reload).\n§fVui lòng kết nối lại để đảm bảo an toàn dữ liệu!");
            }
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                vn.minecraft.rPGCore.jobs.core.JobLeaderboard::updateLeaderboards,
                144000L, 144000L);

        // ==========================================
        // HỆ THỐNG AUTO-SAVE (MỖI 5 PHÚT)
        // ==========================================
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (this.mountDataManager != null) {
                this.mountDataManager.saveAll(true, false);
            }
            if (this.playerManager != null) {
                this.playerManager.saveAllPlayers(true);
            }
            vn.minecraft.rPGCore.jobs.core.JobManager.saveAll(true);

            getLogger().info("§a[Auto-Save] Đã lưu dữ liệu người chơi và thú cưỡi thành công!");
        }, 6000L, 6000L);
        new AuctionTaxTask(this).runTaskTimer(this, 20L * 60 * 30, 20L * 60 * 30);
    }

    @Override
    public void onDisable() {
        if (playerManager != null) {
            playerManager.saveAllPlayers(false);
        }

        vn.minecraft.rPGCore.jobs.core.JobManager.saveAll(false);

        if (this.mountDataManager != null) {
            this.mountDataManager.saveAll(false, true);
        }
        if (this.mountManager != null) {
            this.mountManager.despawnAllActiveMounts();
        }

        getLogger().info("RPGCore da tat va luu du lieu an toan.");

        if (MongoManager.getInstance() != null) {
            MongoManager.getInstance().close();
        }
    }

    public static RPGCore getInstance() { return instance; }
    public PlayerManager getPlayerManager() { return this.playerManager; }
    public FoodBuffManager getFoodBuffManager() { return this.foodBuffManager; }
    public BoredomManager getBoredomManager() { return this.boredomManager; }
    public SkillManager getSkillManager() { return this.skillManager; }
    public BossBarManager getBossBarManager() { return this.bossBarManager; }
    public ItemManager getItemManager() { return this.itemManager; }
    public FishingManager getFishingManager() { return fishingManager; }
    public BaitManager getBaitManager() { return baitManager; }
    public MountManager getMountManager() { return this.mountManager; }
    public MountDataManager getMountDataManager() { return this.mountDataManager; }
    public CurrencyManager getCurrencyManager() { return this.currencyManager; }
    public BackpackManager getBackpackManager() { return backpackManager; }
    public ToxicityManager getToxicityManager() { return this.toxicityManager; }
    public AlchemistBuffManager getAlchemistBuffManager() { return this.alchemistBuffManager; }
    public NPCManager getNpcManager() { return this.npcManager; }
    public GlobalEconomyManager getGlobalEconomyManager() {
        return this.globalEconomyManager;
    }
    public RewardManager getRewardManager() { return rewardManager; }
    public ShopManager getShopManager() { return this.shopManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public AuctionGUIListener getAuctionGUIListener() {
        return this.auctionGUIListener;
    }
}