package vn.minecraft.rPGCore.jobs.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.jobs.core.RPGJob;
import vn.minecraft.rPGCore.jobs.quests.AdvancedQuest;
import vn.minecraft.rPGCore.jobs.quests.QuestAction;
import vn.minecraft.rPGCore.jobs.quests.RPGQuest;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.*;

public class FarmerJob extends RPGJob {

    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public FarmerJob() {
        super("FARMER", "Nông Dân");
        registerQuests();
    }

    private void registerQuests() {
        questPool.put("DAILY_FARM_1", new AdvancedQuest("DAILY_FARM_1", "Gieo Mầm Sự Sống", "Gieo thành công 15 hạt giống bất kỳ.", 15, QuestAction.PLANT_SEED, null));
        questPool.put("DAILY_FARM_2", new AdvancedQuest("DAILY_FARM_2", "Quả Ngọt Đầu Mùa", "Thu hoạch thành công 10 nông sản.", 10, QuestAction.HARVEST_CROP, null));
        questPool.put("DAILY_FARM_3", new AdvancedQuest("DAILY_FARM_3", "Thu Thập Tinh Chất", "Tìm được 3 Tinh Chất Tự Nhiên.", 3, QuestAction.COLLECT_NATURE_ESSENCE, null));
        questPool.put("DAILY_FARM_4", new AdvancedQuest("DAILY_FARM_4", "Nông Dân Cần Mẫn", "Thu hoạch được 3 Nông sản Đột Biến (1.5x).", 3, QuestAction.HARVEST_MUTATION, null));
        questPool.put("DAILY_FARM_5", new AdvancedQuest("DAILY_FARM_5", "Kỹ Thuật Canh Tác", "Sử dụng Phân Bón Hảo Hạng 1 lần.", 1, QuestAction.USE_FERTILIZER, null));
        questPool.put("DAILY_FARM_6", new AdvancedQuest("DAILY_FARM_6", "Bậc Thầy Thu Hồi", "Thu hồi lại hạt giống 3 lần.", 3, QuestAction.RECOVER_SEED, null));

        questPool.put("WEEKLY_FARM_1", new AdvancedQuest("WEEKLY_FARM_1", "Đại Điền Trang", "Thu hoạch thành công 100 nông sản.", 100, QuestAction.HARVEST_CROP, null));
        questPool.put("WEEKLY_FARM_2", new AdvancedQuest("WEEKLY_FARM_2", "Kẻ Nắm Giữ Phép Thuật", "Thu hoạch 5 Nông sản Ma Thuật.", 5, QuestAction.HARVEST_MAGIC_CROP, null));
        questPool.put("WEEKLY_FARM_3", new AdvancedQuest("WEEKLY_FARM_3", "Vụ Mùa Khổng Lồ", "Gieo thành công 200 hạt giống.", 200, QuestAction.PLANT_SEED, null));
        questPool.put("WEEKLY_FARM_4", new AdvancedQuest("WEEKLY_FARM_4", "Tiết Kiệm Tối Đa", "Thu hồi lại 100 hạt giống.", 100, QuestAction.RECOVER_SEED, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() { return questPool; }

    @Override
    public ItemStack getIcon(org.bukkit.entity.Player player) {
        ItemStack icon = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lNGHỀ NÔNG DÂN");
            meta.setLore(getDescription(player));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int currentLevel = data.getLevel("FARMER");

        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc thầy gieo trồng và kiểm soát sinh học.");
        lore.add("§7Độc quyền gieo trồng Hạt Giống Ma Thuật.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + currentLevel + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI CƠ BẢN:");

        double seedRecovery = (0.7 + (0.3 * currentLevel / 50.0)) * 100;
        double uncommonChance = (0.4 + (0.3 * currentLevel / 50.0)) * 100;
        double rareChance = (0.15 + (0.15 * currentLevel / 50.0)) * 100;
        double multiFruit1 = (0.2 + (0.2 * currentLevel / 50.0)) * 100;
        double multiFruit2 = (0.1 + (0.1 * currentLevel / 50.0)) * 100;

        lore.add("§8▪ §fTỉ lệ hoàn trả Hạt giống: §a" + String.format("%.1f", seedRecovery) + "%");
        lore.add("§8▪ §fTỉ lệ Nông sản Đột Biến (1.5x): §e" + String.format("%.1f", uncommonChance) + "%");
        lore.add("§8▪ §fTỉ lệ Nông sản Khổng Lồ (2.0x): §6" + String.format("%.1f", rareChance) + "%");
        lore.add("§8▪ §fXác suất x2 Sản lượng (Cây thường): §b" + String.format("%.1f", multiFruit1) + "%");
        lore.add("§8▪ §fXác suất x2 Sản lượng (Cây đa vụ): §d" + String.format("%.1f", multiFruit2) + "%");

        boolean hasUlt1 = data.hasUnlockedSkill("FARMER", "FARMER_ULT_1");
        boolean hasUlt2 = data.hasUnlockedSkill("FARMER", "FARMER_ULT_2");
        boolean hasUlt3 = data.hasUnlockedSkill("FARMER", "FARMER_ULT_3");

        if (hasUlt1 || hasUlt2 || hasUlt3) {
            lore.add("");
            lore.add("§d§lKỸ NĂNG TỐI THƯỢNG ĐÃ MỞ:");
            if (hasUlt1) lore.add("§8▪ §ePhép Màu Vụ Mùa: §fNhân đôi (x2) số lần hái quả Đa Vụ.");
            if (hasUlt2) lore.add("§8▪ §eThuật Thúc Sinh: §fGiảm 25% thời gian sinh trưởng (Còn 3 Tiếng).");
            if (hasUlt3) lore.add("§8▪ §eVụ Mùa Bội Thu: §f100% rớt Nông sản 1.5x, Tăng cường x2 Sản lượng.");
        }
        return lore;
    }

    // =========================================
    // CỬA HÀNG NÔNG DÂN (VIỀN CAM - RUỘT ĐEN)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§2TRẠM NÔNG NGHIỆP"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("FARMER");

        ItemStack orangeGlass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta oMeta = orangeGlass.getItemMeta(); oMeta.setDisplayName(" "); orangeGlass.setItemMeta(oMeta);

        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blMeta = blackGlass.getItemMeta(); blMeta.setDisplayName(" "); blackGlass.setItemMeta(blMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, orangeGlass);
            else inv.setItem(i, blackGlass);
        }

        // ==============================================
        // HÀNG 2: MUA BẰNG TINH CHẤT TỰ NHIÊN
        // ==============================================
        inv.setItem(10, createEssenceItem(im.buildItem("misc_job_change_card", 1), "FARM_JOB_CARD", data, 1, 150, jobLevel >= 1, 1));
        inv.setItem(11, createEssenceItem(im.buildItem("bag_seeds_common", 1), "FARM_BAG_COMMON", data, 5, 5, jobLevel >= 1, 1));
        inv.setItem(12, createEssenceItem(im.buildItem("bag_seeds_magic", 1), "FARM_BAG_MAGIC", data, 3, 25, jobLevel >= 25, 25));
        inv.setItem(13, createEssenceItem(im.buildItem("misc_watering_can", 1), "FARM_WATER_CAN", data, 10, 5, jobLevel >= 10, 10));
        inv.setItem(14, createEssenceItem(im.buildItem("misc_fertilizer", 1), "FARM_FERTILIZER", data, 5, 15, jobLevel >= 20, 20));
        inv.setItem(15, createEssenceItem(im.buildItem("armor_farmer_hat", 1), "FARM_HAT", data, 1, 50, jobLevel >= 30, 30));
        inv.setItem(16, createEssenceItem(im.buildItem("misc_seed_pouch", 1), "FARM_SEED_POUCH", data, 2, 30, jobLevel >= 15, 15));

        // ==============================================
        // HÀNG 3: HẠT MA THUẬT (MUA BẰNG TINH CHẤT THEO SỐ VỤ)
        // 1 Vụ = 5 TC, 3 Vụ = 10 TC, 5 Vụ = 20 TC
        // ==============================================
        inv.setItem(19, createEssenceItem(im.buildItem("seed_magic_golden_carrot", 1), "FARM_SEED_M1", data, 20, 5, jobLevel >= 1, 1));
        inv.setItem(20, createEssenceItem(im.buildItem("seed_magic_flame_beetroot", 1), "FARM_SEED_M2", data, 20, 5, jobLevel >= 1, 1));
        inv.setItem(21, createEssenceItem(im.buildItem("seed_magic_golden_melon", 1), "FARM_SEED_M3", data, 10, 10, jobLevel >= 1, 1));
        inv.setItem(22, createEssenceItem(im.buildItem("seed_magic_glowing_mushroom", 1), "FARM_SEED_M4", data, 10, 5, jobLevel >= 1, 1));
        inv.setItem(23, createEssenceItem(im.buildItem("seed_magic_star_wheat", 1), "FARM_SEED_M5", data, 20, 5, jobLevel >= 1, 1));
        inv.setItem(24, createEssenceItem(im.buildItem("seed_magic_soul_sweet_berry", 1), "FARM_SEED_M6", data, 10, 20, jobLevel >= 1, 1));
        inv.setItem(25, createEssenceItem(im.buildItem("seed_magic_dragon_cocoa", 1), "FARM_SEED_M7", data, 10, 10, jobLevel >= 1, 1));

        // ==============================================
        // HÀNG 4: HẠT THƯỜNG (MUA BẰNG BẠC THEO SỐ VỤ -> NHÂN 64)
        // 1 Vụ = 10 Bạc, 3 Vụ = 15 Bạc, 5 Vụ = 20 Bạc
        // ==============================================
        inv.setItem(28, createCoinItem(im.buildItem("seed_wheat", 1), "FARM_SEED_1", data, 100, 10 * 64, jobLevel >= 1, 1));
        inv.setItem(29, createCoinItem(im.buildItem("seed_carrot", 1), "FARM_SEED_2", data, 100, 10 * 64, jobLevel >= 1, 1));
        inv.setItem(30, createCoinItem(im.buildItem("seed_potato", 1), "FARM_SEED_3", data, 100, 10 * 64, jobLevel >= 1, 1));
        inv.setItem(31, createCoinItem(im.buildItem("seed_melon", 1), "FARM_SEED_4", data, 50, 15 * 64, jobLevel >= 1, 1));
        inv.setItem(32, createCoinItem(im.buildItem("seed_sweet_berry", 1), "FARM_SEED_5", data, 50, 20 * 64, jobLevel >= 1, 1));
        inv.setItem(33, createCoinItem(im.buildItem("seed_kelp", 1), "FARM_SEED_6", data, 50, 15 * 64, jobLevel >= 1, 1));
        inv.setItem(34, createCoinItem(im.buildItem("seed_cocoa", 1), "FARM_SEED_7", data, 50, 15 * 64, jobLevel >= 1, 1));

        // ==============================================
        // HÀNG 5: NÔNG CỤ VÀ VẬT TƯ (BẰNG BẠC -> NHÂN 64)
        // ==============================================
        inv.setItem(37, createCoinItem(im.buildItem("tool_harvest_hoe", 1), "FARM_TOOL_HOE", data, 10, 10 * 64, jobLevel >= 1, 1)); // 200 Bạc

        ItemStack waterBucket = new ItemStack(Material.WATER_BUCKET);
        ItemMeta wbMeta = waterBucket.getItemMeta(); wbMeta.setDisplayName("§bXô Nước"); waterBucket.setItemMeta(wbMeta);
        inv.setItem(40, createCoinItem(waterBucket, "FARM_MAT_WATER", data, 20, 1 * 64, jobLevel >= 1, 1)); // 50 Bạc

        ItemStack oakLog = new ItemStack(Material.OAK_LOG, 64);
        ItemMeta olMeta = oakLog.getItemMeta(); olMeta.setDisplayName("§6Khúc Gỗ Sồi (x64)"); oakLog.setItemMeta(olMeta);
        inv.setItem(43, createCoinItem(oakLog, "FARM_MAT_WOOD", data, 10, 3 * 64, jobLevel >= 1, 1)); // 5 Bạc

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(53, back);

        player.openInventory(inv);
    }

    private ItemStack createEssenceItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int essenceCost, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);

        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA");
            meta.setLore(java.util.Arrays.asList("§7Yêu cầu Cấp độ Nông Dân: §b" + reqLevel));
            item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName()));
            meta.setLore(java.util.Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§fGiá: §a" + essenceCost + " Tinh Chất Tự Nhiên");
            lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, essenceCost);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING, purchaseId);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, maxStock);
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCoinItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int coinCost, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);

        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA");
            meta.setLore(java.util.Arrays.asList("§7Yêu cầu Cấp độ Nông Dân: §b" + reqLevel));
            item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName()));
            meta.setLore(java.util.Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§8§m------------------------");
            vn.minecraft.rPGCore.managers.CurrencyManager cm = new vn.minecraft.rPGCore.managers.CurrencyManager(vn.minecraft.rPGCore.RPGCore.getInstance());
            lore.add("§fGiá: " + cm.formatCurrency(coinCost));
            lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, coinCost);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING, purchaseId);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, maxStock);
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleShopClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int slot = event.getRawSlot();

        if (slot == 53) { vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI.openStatusMenu(player); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();

        String buyId = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING);
        if (buyId == null) return;
        int maxStock = meta.getPersistentDataContainer().getOrDefault(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, 0);

        if (data.getPurchasedAmount(buyId) >= maxStock) {
            player.sendMessage("§cĐã đạt giới hạn mua trong tháng này!");
            return;
        }

        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        ItemStack cleanReward = clicked.clone();

        // Cân nhắc số lượng của Item (Xô nước là 1, Gỗ là 64)
        if (buyId.equals("FARM_MAT_WOOD")) cleanReward.setAmount(64);
        else cleanReward.setAmount(1);

        ItemMeta cleanMeta = cleanReward.getItemMeta();
        List<String> cleanLore = cleanMeta.getLore();
        if (cleanLore != null) {
            cleanLore.removeIf(l -> l.contains("Giá:") || l.contains("Giới hạn mua:") || l.contains("---"));
            cleanMeta.setLore(cleanLore);
        }
        cleanReward.setItemMeta(cleanMeta);

        if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int essenceCost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            ItemStack essenceTemplate = im.buildItem("misc_nature_essence", 1);

            if (im.takeItem(player, essenceTemplate, essenceCost)) {
                data.addPurchase(buyId, 1);
                player.getInventory().addItem(cleanReward);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                openShop(player);
            } else {
                player.sendMessage("§cKhông đủ " + essenceCost + " Tinh Chất Tự Nhiên!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
        else if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int coinCost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            vn.minecraft.rPGCore.managers.CurrencyManager cm = new vn.minecraft.rPGCore.managers.CurrencyManager(vn.minecraft.rPGCore.RPGCore.getInstance());

            if (cm.takePhysicalCurrency(player, coinCost)) {
                data.addPurchase(buyId, 1);
                player.getInventory().addItem(cleanReward);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                openShop(player);
            } else {
                player.sendMessage("§cKhông đủ " + cm.formatCurrency(coinCost) + " trong túi đồ!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    // =========================================
    // CÂY KỸ NĂNG NÔNG DÂN (CHUẨN 5 HÀNG)
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§dCÂY KỸ NĂNG NÔNG DÂN"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (data.getLevel("FARMER") < 50) {
            player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!");
            player.closeInventory(); return;
        }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, getSkillTreeGUITitle());
        ItemStack glass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        inv.setItem(13, createSkillIcon("FARM_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("FARMER")));

        inv.setItem(29, createSkillIcon("FARMER_ULT_1", Material.MELON, "§e§lPHÉP MÀU VỤ MÙA", 3, data, "§fTất cả cây Đa Vụ Mùa do bạn trồng", "§fsẽ được nhân đôi (x2) số lần hái quả."));
        inv.setItem(31, createSkillIcon("FARMER_ULT_2", Material.CLOCK, "§6§lTHUẬT THÚC SINH", 3, data, "§fGiảm 25% thời gian sinh trưởng của cây.", "§fCây chỉ cần 3 Tiếng thay vì 4 Tiếng."));
        inv.setItem(33, createSkillIcon("FARMER_ULT_3", Material.GOLDEN_CARROT, "§d§lVỤ MÙA BỘI THU", 3, data, "§fKhóa cứng 100% nổ Nông sản Đột Biến (1.5x).", "§fTăng cực mạnh tỉ lệ nổ Khổng Lồ (2.0x)", "§fvà sản lượng Cây trái sum xuê."));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(44, back);

        player.openInventory(inv);
    }

    private ItemStack createSkillIcon(String skillId, Material mat, String name, int jpCost, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String... desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Nội tại Kỹ năng Tối thượng");
        lore.add("");
        for (String line : desc) lore.add("§8▪ " + line);
        lore.add("");

        if (!skillId.equals("FARM_INFO")) {
            if (data.hasUnlockedSkill("FARMER", skillId)) {
                lore.add("§a§l✔ ĐÃ KÍCH HOẠT VÀ ĐANG HOẠT ĐỘNG");
            } else {
                lore.add("§e§lCLICK ĐỂ HỌC KỸ NĂNG NÀY!");
                lore.add("§fTiêu hao: §d" + jpCost + " JP");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleSkillTreeClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int slot = event.getRawSlot();

        if (slot == 44) { vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI.openStatusMenu(player); return; }

        if (slot == 29) processSkillPurchase(player, data, "FARMER_ULT_1", 5);
        else if (slot == 31) processSkillPurchase(player, data, "FARMER_ULT_2", 5);
        else if (slot == 33) processSkillPurchase(player, data, "FARMER_ULT_3", 5);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("FARMER", skillId)) return;
        if (data.getJobPoints("FARMER") >= cost) {
            data.addJobPoints("FARMER", -cost);
            data.unlockSkill("FARMER", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            openSkillTree(player);
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // =========================================
    // TRẢ THƯỞNG NHIỆM VỤ NGÀY / TUẦN
    // =========================================
    @Override
    public List<String> getDailyRewardLore(int milestone) {
        if (milestone == 1) return Arrays.asList("§a+1 Tinh Chất Tự Nhiên");
        if (milestone == 2) return Arrays.asList("§a+2 Tinh Chất Tự Nhiên");
        return Arrays.asList("§a+3 Tinh Chất Tự Nhiên", "§d+1 Túi Hạt Giống Thường");
    }

    @Override
    public List<String> getWeeklyRewardLore() {
        return Arrays.asList("§a+10 Tinh Chất Tự Nhiên", "§d+2 Túi Hạt Ma Thuật");
    }

    @Override
    public void giveDailyMilestoneReward(org.bukkit.entity.Player player, int milestone) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();

        if (milestone == 1) {
            player.getInventory().addItem(im.buildItem("misc_nature_essence", 1));
        }
        else if (milestone == 2) {
            player.getInventory().addItem(im.buildItem("misc_nature_essence", 2));
        }
        else if (milestone == 3) {
            player.getInventory().addItem(im.buildItem("misc_nature_essence", 3));
            player.getInventory().addItem(im.buildItem("bag_seeds_common", 1));
        }
    }

    @Override
    public void giveWeeklyMilestoneReward(org.bukkit.entity.Player player) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();

        player.getInventory().addItem(im.buildItem("misc_nature_essence", 10));
        player.getInventory().addItem(im.buildItem("bag_seeds_magic", 2));
    }
}