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

public class FishermanJob extends RPGJob {

    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public FishermanJob() {
        super("FISHERMAN", "Ngư Dân");
        registerQuests();
    }

    private void registerQuests() {
        // DAILY
        questPool.put("DAILY_FISH_1", new AdvancedQuest("DAILY_FISH_1", "Thợ Săn Biển Cả", "Câu thành công 15 con cá bất kỳ.", 15, QuestAction.FISH_CATCH, null));
        questPool.put("DAILY_FISH_2", new AdvancedQuest("DAILY_FISH_2", "Đặc Sản Vùng Miền", "Câu thành công 5 con cá ở Vùng Biển (OCEAN).", 5, QuestAction.FISH_CATCH, args -> args.length > 0 && "OCEAN".equals(args[0])));
        questPool.put("DAILY_FISH_3", new AdvancedQuest("DAILY_FISH_3", "Nghệ Nhân Pha Chế", "Chế tạo mồi câu 3 lần.", 3, QuestAction.CRAFT_BAIT, null));
        questPool.put("DAILY_FISH_4", new AdvancedQuest("DAILY_FISH_4", "Tinh Hoa Biển Sâu", "Kiếm được 5 Nước Mắt Đại Dương.", 5, QuestAction.COLLECT_OCEAN_TEAR, null));
        questPool.put("DAILY_FISH_5", new AdvancedQuest("DAILY_FISH_5", "Sự Cố Nghề Nghiệp", "Câu thất bại (Đứt dây) 5 lần.", 5, QuestAction.FISH_FAIL, null));
        questPool.put("DAILY_FISH_6", new AdvancedQuest("DAILY_FISH_6", "Bảo Vệ Môi Trường", "Câu vớt được rác 10 lần.", 10, QuestAction.FISH_TRASH, null));
        questPool.put("DAILY_FISH_7", new AdvancedQuest("DAILY_FISH_7", "Thợ Lặn Trục Vớt", "Câu được 1 Rương Kho Báu.", 1, QuestAction.FISH_TREASURE, null));

        // WEEKLY
        questPool.put("WEEKLY_FISH_1", new AdvancedQuest("WEEKLY_FISH_1", "Huyền Thoại Đại Dương", "Câu thành công 100 con cá.", 100, QuestAction.FISH_CATCH, null));
        questPool.put("WEEKLY_FISH_2", new AdvancedQuest("WEEKLY_FISH_2", "Vượt Qua Giông Bão", "Câu thất bại 30 lần.", 30, QuestAction.FISH_FAIL, null));
        questPool.put("WEEKLY_FISH_3", new AdvancedQuest("WEEKLY_FISH_3", "Chiến Dịch Làm Sạch", "Câu vớt được rác 50 lần.", 50, QuestAction.FISH_TRASH, null));
        questPool.put("WEEKLY_FISH_4", new AdvancedQuest("WEEKLY_FISH_4", "Triệu Phú Ve Chai", "Câu được 10 Rương Kho Báu.", 10, QuestAction.FISH_TREASURE, null));
        questPool.put("WEEKLY_FISH_5", new AdvancedQuest("WEEKLY_FISH_5", "Công Thức Bí Truyền", "Chế tạo mồi câu có tối thiểu 9 Mồi Nền 10 lần.", 10, QuestAction.CRAFT_BAIT, args -> args.length > 0 && (int)args[0] >= 9));
        questPool.put("WEEKLY_FISH_6", new AdvancedQuest("WEEKLY_FISH_6", "Chinh Phục Thủy Quái", "Câu được 5 con cá kích cỡ Lớn (Large) trở lên.", 5, QuestAction.FISH_CATCH, args -> args.length > 1 && (boolean)args[1]));
    }

    @Override
    public Map<String, RPGQuest> getQuests() { return questPool; }

    @Override
    public ItemStack getIcon(org.bukkit.entity.Player player) {
        ItemStack icon = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lNGHỀ NGƯ DÂN");
            meta.setLore(getDescription(player));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int level = data.getLevel("FISHERMAN");

        double ultMult = data.hasUnlockedSkill("FISHERMAN", "FISH_ULT_1") ? 1.2 : 1.0;

        double autoCatch = (level * 0.5) * ultMult;
        int tolerance = (int)((level * 10) * ultMult);
        double saveBait = (level * 0.5) * ultMult;
        double saveDura = (level * 0.25) * ultMult;
        double bigFish = (level * 0.5) * ultMult;
        double treasure = (level * 0.3) * ultMult;
        double craftBuff = (level * 0.6) * ultMult;
        int craftExtra = level >= 50 ? 2 : (level >= 25 ? 1 : 0);

        List<String> lore = new ArrayList<>();
        lore.add("§7Chúa tể biển cả và nghệ nhân pha chế mồi câu.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + level + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI CƠ BẢN:");
        lore.add("§8▪ §fDung sai nhịp điệu (Dễ bấm): §e+" + tolerance + "ms");
        lore.add("§8▪ §fXác suất câu thẳng (Bỏ qua Minigame): §a" + String.format("%.1f", autoCatch) + "%");
        lore.add("§8▪ §fXác suất không hao Mồi: §d" + String.format("%.1f", saveBait) + "% §f| Độ bền: §d" + String.format("%.1f", saveDura) + "%");
        lore.add("§8▪ §fTăng tỉ lệ Thủy quái: §c+" + String.format("%.1f", bigFish) + "% §f| Rương: §6+" + String.format("%.1f", treasure) + "%");
        lore.add("§8▪ §fBuff chỉ số ủ Mồi: §b+" + String.format("%.1f", craftBuff) + "% §f| Tăng sản lượng: §b+" + craftExtra);

        boolean hasUlt1 = data.hasUnlockedSkill("FISHERMAN", "FISH_ULT_1");
        boolean hasUlt2 = data.hasUnlockedSkill("FISHERMAN", "FISH_ULT_2");
        boolean hasUlt3 = data.hasUnlockedSkill("FISHERMAN", "FISH_ULT_3");

        if (hasUlt1 || hasUlt2 || hasUlt3) {
            lore.add("");
            lore.add("§d§lKỸ NĂNG TỐI THƯỢNG ĐÃ MỞ:");
            if (hasUlt1) lore.add("§8▪ §eĐại Dương Phù Hộ: §fTăng 20% sức mạnh mọi Nội tại Ngư Dân.");
            if (hasUlt2) lore.add("§8▪ §eCông Thức Cổ Đại: §fMở khóa 3 ô Phụ gia ở đáy Thùng Ủ Mồi.");
            if (hasUlt3) lore.add("§8▪ §eÝ Chí Kiên Cường: §fVô hạn mạng khi chơi Minigame kéo cá.");
        }
        return lore;
    }

    // =========================================
    // SHOP NGƯ DÂN (6x9)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§1CHỢ HẢI SẢN"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("FISHERMAN");

        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta boMeta = border.getItemMeta(); boMeta.setDisplayName(" "); border.setItemMeta(boMeta);

        // Fill Hàng 1 và 6
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        inv.setItem(9, border); inv.setItem(17, border);

        // Hàng 2: Túi cần câu & Mồi Buff Premium
        inv.setItem(10, createTearItem(im.buildItem("bag_fishing_rod_uncommon", 1), "FISH_ROD_UNC", data, 2, 20, jobLevel >= 10, 10));
        inv.setItem(11, createTearItem(im.buildItem("bag_fishing_rod_rare", 1), "FISH_ROD_RAR", data, 1, 50, jobLevel >= 25, 25));
        inv.setItem(12, createTearItem(im.buildItem("bag_fishing_rod_epic", 1), "FISH_ROD_EPI", data, 1, 150, jobLevel >= 40, 40));

        inv.setItem(13, createTearItem(im.buildItem("bait_buff_monster_blood", 1), "FISH_BUFF_P1", data, 5, 20, jobLevel >= 20, 20));
        inv.setItem(14, createTearItem(im.buildItem("bait_buff_golden_lure", 1), "FISH_BUFF_P2", data, 5, 20, jobLevel >= 20, 20));
        inv.setItem(15, createTearItem(im.buildItem("bait_buff_pirate_coin", 1), "FISH_BUFF_P3", data, 5, 20, jobLevel >= 20, 20));
        inv.setItem(16, createTearItem(im.buildItem("bait_buff_lightning", 1), "FISH_BUFF_P4", data, 5, 25, jobLevel >= 30, 30));

        // Hàng 3 & 4: Xoay tua random
        Calendar cal = Calendar.getInstance();
        Random shopRand = new Random(cal.get(Calendar.YEAR) * 100L + cal.get(Calendar.MONTH));

        List<String> allBuffs = Arrays.asList("bait_buff_pure_water", "bait_buff_sonar", "bait_buff_magnet", "bait_buff_metronome", "bait_buff_phoenix", "bait_buff_lazy_charm", "bait_buff_wind_glyph", "bait_buff_ancient_bone", "bait_buff_glowing_oil", "bait_buff_mutant_lilypad", "bait_buff_coral_powder", "bait_buff_freshwater_crystal", "bait_buff_spider_venom", "bait_buff_mermaid_tear", "bait_buff_swamp_gas");
        List<String> allBases = Arrays.asList("bait_base_earthworm", "bait_base_minnow", "bait_base_shiny_pebble", "bait_base_sludge", "bait_base_corn", "bait_base_sticky_sap", "bait_base_sandworm", "bait_base_leech", "bait_base_blindworm", "bait_base_swamp_snail", "bait_base_squid_meat", "bait_base_loach", "bait_base_blind_crab", "bait_base_small_frog", "bait_base_broken_shell", "bait_base_rusty_coin", "bait_base_stalactite", "bait_base_dead_kelp", "bait_base_moss", "bait_base_mold");

        Collections.shuffle(allBuffs, shopRand);
        Collections.shuffle(allBases, shopRand);

        for (int i = 0; i < 7; i++) {
            int cost = 2 + shopRand.nextInt(6);
            inv.setItem(19 + i, createTearItem(im.buildItem(allBuffs.get(i), 1), "FISH_ROT_BUFF_" + i, data, 10, cost, jobLevel >= 5, 5));
        }

        for (int i = 0; i < 7; i++) {
            int cost = 10 + shopRand.nextInt(15);
            inv.setItem(28 + i, createCoinItem(im.buildItem(allBases.get(i), 1), "FISH_ROT_BASE_" + i, data, 100, cost * 64, jobLevel >= 1, 1));
        }

        // Hàng 5: Item Vanilla bằng tiền
        inv.setItem(37, createCoinItem(new ItemStack(Material.FISHING_ROD), "FISH_VANILLA_ROD", data, 999, 10 * 64, jobLevel >= 1, 1));
        inv.setItem(38, createCoinItem(new ItemStack(Material.COMPOSTER), "FISH_COMPOSTER", data, 5, 50 * 64, jobLevel >= 1, 1));
        inv.setItem(39, createCoinItem(new ItemStack(Material.OAK_CHEST_BOAT), "FISH_BOAT", data, 10, 25 * 64, jobLevel >= 1, 1));
        inv.setItem(40, createCoinItem(new ItemStack(Material.SPYGLASS), "FISH_SPYGLASS", data, 1, 100 * 64, jobLevel >= 10, 10));
        inv.setItem(41, createCoinItem(new ItemStack(Material.COMPASS), "FISH_COMPASS", data, 1, 30 * 64, jobLevel >= 1, 1));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(53, back);

        player.openInventory(inv);
    }

    private ItemStack createTearItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int tearCost, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone(); ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);
        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA"); meta.setLore(Arrays.asList("§7Yêu cầu Cấp độ Ngư Dân: §b" + reqLevel)); item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName())); meta.setLore(Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(""); lore.add("§8§m------------------------"); lore.add("§fGiá: §b" + tearCost + " Nước Mắt Đại Dương"); lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_tear_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, tearCost);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING, purchaseId);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, maxStock);
            meta.setLore(lore);
        }
        item.setItemMeta(meta); return item;
    }

    private ItemStack createCoinItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int coinCost, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone(); ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);
        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA"); meta.setLore(Arrays.asList("§7Yêu cầu Cấp độ Ngư Dân: §b" + reqLevel)); item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock && maxStock != 999) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName())); meta.setLore(Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(""); lore.add("§8§m------------------------");
            vn.minecraft.rPGCore.managers.CurrencyManager cm = new vn.minecraft.rPGCore.managers.CurrencyManager(vn.minecraft.rPGCore.RPGCore.getInstance());
            lore.add("§fGiá: " + cm.formatCurrency(coinCost));
            if (maxStock != 999) lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, coinCost);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING, purchaseId);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, maxStock);
            meta.setLore(lore);
        }
        item.setItemMeta(meta); return item;
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

        if (maxStock != 999 && data.getPurchasedAmount(buyId) >= maxStock) {
            player.sendMessage("§cĐã đạt giới hạn mua trong tháng này!"); return;
        }

        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        ItemStack cleanReward = clicked.clone();
        if (buyId.startsWith("FISH_ROT_BASE_")) cleanReward.setAmount(64); // Nền mua 1 ra 64
        else cleanReward.setAmount(1);

        ItemMeta cleanMeta = cleanReward.getItemMeta();
        List<String> cleanLore = cleanMeta.getLore();
        if (cleanLore != null) { cleanLore.removeIf(l -> l.contains("Giá:") || l.contains("Giới hạn mua:") || l.contains("---")); cleanMeta.setLore(cleanLore); }
        cleanReward.setItemMeta(cleanMeta);

        if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_tear_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int tearCost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_tear_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            ItemStack tearTemplate = im.buildItem("misc_ocean_tear", 1);

            if (im.takeItem(player, tearTemplate, tearCost)) {
                data.addPurchase(buyId, 1); player.getInventory().addItem(cleanReward); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f); openShop(player);
            } else { player.sendMessage("§cKhông đủ " + tearCost + " Nước Mắt Đại Dương!"); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f); }
        }
        else if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int coinCost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            vn.minecraft.rPGCore.managers.CurrencyManager cm = new vn.minecraft.rPGCore.managers.CurrencyManager(vn.minecraft.rPGCore.RPGCore.getInstance());

            if (cm.takePhysicalCurrency(player, coinCost)) {
                data.addPurchase(buyId, 1); player.getInventory().addItem(cleanReward); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f); openShop(player);
            } else { player.sendMessage("§cKhông đủ " + cm.formatCurrency(coinCost) + " trong túi đồ!"); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f); }
        }
    }

    // =========================================
    // CÂY KỸ NĂNG NGƯ DÂN
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§3CÂY KỸ NĂNG NGƯ DÂN"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (data.getLevel("FISHERMAN") < 50) { player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!"); player.closeInventory(); return; }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, getSkillTreeGUITitle());
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        inv.setItem(13, createSkillIcon("FISH_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("FISHERMAN")));
        inv.setItem(29, createSkillIcon("FISH_ULT_1", Material.HEART_OF_THE_SEA, "§e§lĐẠI DƯƠNG PHÙ HỘ", 3, data, "§fTăng §a20% §fsức mạnh của TOÀN BỘ", "§fNội tại Ngư Dân đang sở hữu."));
        inv.setItem(31, createSkillIcon("FISH_ULT_2", Material.BREWING_STAND, "§6§lCÔNG THỨC CỔ ĐẠI", 3, data, "§fPhá vỡ giới hạn Thùng Ủ Mồi.", "§fMở khóa 3 ô Phụ gia đặc biệt ở đáy."));
        inv.setItem(33, createSkillIcon("FISH_ULT_3", Material.TOTEM_OF_UNDYING, "§d§lÝ CHÍ KIÊN CƯỜNG", 3, data, "§fCâu trượt không bị trừ Mạng.", "§fVô hạn lần thử trong Minigame."));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(44, back);
        player.openInventory(inv);
    }

    private ItemStack createSkillIcon(String skillId, Material mat, String name, int jpCost, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String... desc) {
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(); lore.add("§7Nội tại Kỹ năng Tối thượng"); lore.add("");
        for (String line : desc) lore.add("§8▪ " + line); lore.add("");
        if (!skillId.equals("FISH_INFO")) {
            if (data.hasUnlockedSkill("FISHERMAN", skillId)) lore.add("§a§l✔ ĐÃ KÍCH HOẠT");
            else { lore.add("§e§lCLICK ĐỂ HỌC KỸ NĂNG NÀY!"); lore.add("§fTiêu hao: §d" + jpCost + " JP"); }
        }
        meta.setLore(lore); item.setItemMeta(meta); return item;
    }

    @Override
    public void handleSkillTreeClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int slot = event.getRawSlot();
        if (slot == 44) { vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI.openStatusMenu(player); return; }
        if (slot == 29) processSkillPurchase(player, data, "FISH_ULT_1", 3);
        else if (slot == 31) processSkillPurchase(player, data, "FISH_ULT_2", 3);
        else if (slot == 33) processSkillPurchase(player, data, "FISH_ULT_3", 3);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("FISHERMAN", skillId)) return;
        if (data.getJobPoints("FISHERMAN") >= cost) {
            data.addJobPoints("FISHERMAN", -cost); data.unlockSkill("FISHERMAN", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); openSkillTree(player);
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!"); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @Override
    public List<String> getDailyRewardLore(int milestone) {
        if (milestone == 1) return Arrays.asList("§b+1 Nước Mắt Đại Dương");
        if (milestone == 2) return Arrays.asList("§b+2 Nước Mắt Đại Dương");
        return Arrays.asList("§b+3 Nước Mắt Đại Dương", "§d+1 Túi Cần Câu Tốt (Uncommon)");
    }

    @Override
    public List<String> getWeeklyRewardLore() {
        return Arrays.asList("§b+10 Nước Mắt Đại Dương", "§d+1 Túi Cần Câu Hiếm (Rare)");
    }

    @Override
    public void giveDailyMilestoneReward(org.bukkit.entity.Player player, int milestone) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        if (milestone == 1) player.getInventory().addItem(im.buildItem("misc_ocean_tear", 1));
        else if (milestone == 2) player.getInventory().addItem(im.buildItem("misc_ocean_tear", 2));
        else if (milestone == 3) {
            player.getInventory().addItem(im.buildItem("misc_ocean_tear", 3));
            player.getInventory().addItem(im.buildItem("bag_fishing_rod_uncommon", 1));
        }
    }

    @Override
    public void giveWeeklyMilestoneReward(org.bukkit.entity.Player player) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        player.getInventory().addItem(im.buildItem("misc_ocean_tear", 10));
        player.getInventory().addItem(im.buildItem("bag_fishing_rod_rare", 1));
    }
}