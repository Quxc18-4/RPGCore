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

public class ChefJob extends RPGJob {

    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public ChefJob() {
        super("CHEF", "Đầu Bếp");
        registerQuests();
    }

    private void registerQuests() {
        questPool.put("DAILY_COOK_1", new AdvancedQuest("DAILY_COOK_1", "Khởi Nguồn Khói Lửa", "Nấu thành công 5 vạc súp bất kỳ.", 5, QuestAction.COOK_ANY, null));
        questPool.put("DAILY_COOK_2", new AdvancedQuest("DAILY_COOK_2", "Thời Đại Khó Khăn", "Nấu 1 vạc súp chỉ dùng đúng 1 nguyên liệu.", 1, QuestAction.COOK_SINGLE, null));
        questPool.put("DAILY_COOK_3", new AdvancedQuest("DAILY_COOK_3", "Lẩu Thập Cẩm", "Nấu 1 vạc súp no nê với 9 nguyên liệu.", 1, QuestAction.COOK_FULL, null));
        questPool.put("DAILY_COOK_4", new AdvancedQuest("DAILY_COOK_4", "Dạ Dày Không Đáy", "Tiêu thụ 10 món ăn bất kỳ.", 10, QuestAction.CONSUME_FOOD, null));
        questPool.put("DAILY_COOK_5", new AdvancedQuest("DAILY_COOK_5", "Khổ Tu", "Bị cộng điểm Tuyệt thực 3 lần.", 3, QuestAction.BOREDOM_GAIN, null));

        questPool.put("WEEKLY_COOK_1", new AdvancedQuest("WEEKLY_COOK_1", "Siêu Đầu Bếp", "Nấu thành công 30 vạc súp bất kỳ.", 30, QuestAction.COOK_ANY, null));
        questPool.put("WEEKLY_COOK_2", new AdvancedQuest("WEEKLY_COOK_2", "Bậc Thầy Gia Vị", "Tiêu thụ 50 món ăn bất kỳ.", 50, QuestAction.CONSUME_FOOD, null));
        questPool.put("WEEKLY_COOK_3", new AdvancedQuest("WEEKLY_COOK_3", "Khổ Tận Cam Lai", "Bị cộng điểm Tuyệt thực 15 lần.", 15, QuestAction.BOREDOM_GAIN, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() { return questPool; }

    @Override
    public ItemStack getIcon(org.bukkit.entity.Player player) {
        ItemStack icon = new ItemStack(Material.CAMPFIRE);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lNGHỀ ĐẦU BẾP");
            meta.setLore(getDescription(player));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int currentLevel = data.getLevel("CHEF");

        List<String> lore = new ArrayList<>();
        lore.add("§7Nghệ nhân kiểm soát hương vị và ma thuật thực phẩm.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + currentLevel + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI CƠ BẢN:");

        double buffMulti = (currentLevel / 50.0) * 30.0;
        double durationMulti = (currentLevel / 50.0) * 25.0;
        double freshMulti = (currentLevel / 50.0) * 50.0;
        double bonusChance = (currentLevel / 50.0) * 40.0;
        double hideChance = currentLevel * 1.5;

        lore.add("§8▪ §fCường hóa mọi Buff chỉ số: §a+" + String.format("%.1f", buffMulti) + "%");
        lore.add("§8▪ §fXác suất Nhân đôi thời gian No Căng: §6" + String.format("%.1f", durationMulti) + "%");
        lore.add("§8▪ §fXác suất Nhân đôi thời gian Bảo quản: §e" + String.format("%.1f", freshMulti) + "%");
        lore.add("§8▪ §fBàn tay hào phóng (Cộng thêm 0.5 điểm nguyên liệu): §b" + String.format("%.1f", bonusChance) + "%");
        //lore.add("§8▪ §fSát thủ bếp (Xác suất giấu Độc trong Lore): §c" + String.format("%.1f", hideChance) + "%");

        boolean hasStomach = data.hasUnlockedSkill("CHEF", "CHEF_IRON_STOMACH");
        boolean hasHands = data.hasUnlockedSkill("CHEF", "CHEF_MAGIC_HANDS");
        boolean hasTaste = data.hasUnlockedSkill("CHEF", "CHEF_ASCENDED_TASTE");

        if (hasStomach || hasHands || hasTaste) {
            lore.add("");
            lore.add("§d§lKỸ NĂNG TỐI THƯỢNG ĐÃ MỞ:");
            if (hasStomach) lore.add("§8▪ §eDạ Dày Thép: §fGiảm 50% Điểm ngấy & Thời gian tuyệt thực.");
            if (hasHands) lore.add("§8▪ §eBàn Tay Ma Thuật: §fTăng thêm 20% Buff và x2 Thời gian no.");
            if (hasTaste) lore.add("§8▪ §eThăng Hoa Vị Giác: §fMở rộng Vạc súp chứa tối đa 12 món.");
        }
        return lore;
    }

    // =========================================
    // CỬA HÀNG ĐẦU BẾP (VIỀN CAM - RUỘT ĐEN)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§2CHỢ THỰC PHẨM"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("CHEF");

        ItemStack orangeGlass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta oMeta = orangeGlass.getItemMeta(); oMeta.setDisplayName(" "); orangeGlass.setItemMeta(oMeta);

        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blMeta = blackGlass.getItemMeta(); blMeta.setDisplayName(" "); blackGlass.setItemMeta(blMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, orangeGlass);
            else inv.setItem(i, blackGlass);
        }

        // Hàng 2 (Slot 10-15) - Mua bằng Tinh Túy
        inv.setItem(10, createEssenceItem(im.buildItem("misc_job_change_card", 1), "CHEF_JOB_CARD", data, 1, 30, jobLevel >= 1, 1));
        inv.setItem(11, createEssenceItem(im.buildItem("potion_digestion", 1), "CHEF_POTION_DIGEST", data, 5, 10, jobLevel >= 10, 10));
        inv.setItem(12, createEssenceItem(im.buildItem("potion_anti_anorexia", 1), "CHEF_POTION_ANOREXIA", data, 2, 25, jobLevel >= 20, 20));
        inv.setItem(13, createEssenceItem(im.buildItem("spice_all_in_one", 1), "CHEF_SPICE_ALL", data, 10, 15, jobLevel >= 30, 30));
        inv.setItem(14, createEssenceItem(im.buildItem("misc_lunch_bag", 1), "CHEF_LUNCH_BAG", data, 5, 20, jobLevel >= 15, 15));
        inv.setItem(15, createEssenceItem(im.buildItem("misc_preservative_powder", 1), "CHEF_PRESERVATIVE", data, 3, 50, jobLevel >= 40, 40));

        // ==============================================
        // HÀNG 3 & 4: TỰ ĐỘNG XOAY TUA NGUYÊN LIỆU THÔNG MINH
        // ==============================================
        Calendar cal = Calendar.getInstance();
        long currentSeed = cal.get(Calendar.YEAR) * 100L + cal.get(Calendar.MONTH);
        Random shopRand = new Random(currentSeed);

        List<ItemManager.ItemData> magicAndRare = new ArrayList<>();
        List<ItemManager.ItemData> basicMeats = new ArrayList<>();
        List<ItemManager.ItemData> basicFishes = new ArrayList<>();
        List<ItemManager.ItemData> basicVegs = new ArrayList<>();
        List<ItemManager.ItemData> basicStarches = new ArrayList<>();

        for (String id : im.getAllIds()) {
            ItemManager.ItemData iData = im.getItemData(id);
            if (iData.points <= 0.0) continue; // Loại bỏ hạt giống, nông cụ (Chỉ lấy thức ăn)

            if (iData.category == ItemManager.Category.MAGIC || (iData.category == ItemManager.Category.VEG && iData.rarity == ItemManager.Rarity.RARE)) {
                magicAndRare.add(iData);
            } else if (iData.category == ItemManager.Category.MEAT) {
                basicMeats.add(iData);
            } else if (iData.category == ItemManager.Category.FISH) {
                basicFishes.add(iData);
            } else if (iData.category == ItemManager.Category.VEG) {
                basicVegs.add(iData);
            } else if (iData.category == ItemManager.Category.STARCH) {
                basicStarches.add(iData);
            }
        }

        Collections.shuffle(magicAndRare, shopRand);
        Collections.shuffle(basicMeats, shopRand);
        Collections.shuffle(basicFishes, shopRand);
        Collections.shuffle(basicVegs, shopRand);
        Collections.shuffle(basicStarches, shopRand);

        // Hàng 3 (Slot 19-25) - Magic & Rare (Mua bằng Tinh Túy từ 1 - 5)
        for (int i = 0; i < 7; i++) {
            if (i < magicAndRare.size()) {
                ItemManager.ItemData iData = magicAndRare.get(i);
                int cost = 1 + shopRand.nextInt(5);
                inv.setItem(19 + i, createEssenceItem(im.buildItem(iData.id, 1), "CHEF_ROT_" + iData.id, data, 10, cost, jobLevel >= 15, 15));
            }
        }

        // Hàng 4 (Slot 28-34) - Basic Items (Mỗi loại ít nhất 1 món, mua bằng Bạc từ 1 - 5)
        List<ItemManager.ItemData> selectedBasics = new ArrayList<>();
        if (!basicMeats.isEmpty()) selectedBasics.add(basicMeats.get(0));
        if (!basicFishes.isEmpty()) selectedBasics.add(basicFishes.get(0));
        if (!basicVegs.isEmpty()) selectedBasics.add(basicVegs.get(0));
        if (!basicStarches.isEmpty()) selectedBasics.add(basicStarches.get(0));

        List<ItemManager.ItemData> allBasicsPool = new ArrayList<>();
        if (basicMeats.size() > 1) allBasicsPool.addAll(basicMeats.subList(1, basicMeats.size()));
        if (basicFishes.size() > 1) allBasicsPool.addAll(basicFishes.subList(1, basicFishes.size()));
        if (basicVegs.size() > 1) allBasicsPool.addAll(basicVegs.subList(1, basicVegs.size()));
        if (basicStarches.size() > 1) allBasicsPool.addAll(basicStarches.subList(1, basicStarches.size()));
        Collections.shuffle(allBasicsPool, shopRand);

        while (selectedBasics.size() < 7 && !allBasicsPool.isEmpty()) {
            selectedBasics.add(allBasicsPool.remove(0));
        }

        for (int i = 0; i < selectedBasics.size(); i++) {
            ItemManager.ItemData iData = selectedBasics.get(i);
            int cost = 1 + shopRand.nextInt(5);
            inv.setItem(28 + i, createCoinItem(im.buildItem(iData.id, 1), "CHEF_ROT_" + iData.id, data, 64, cost * 64, jobLevel >= 1, 1));
        }

        // Hàng 5 (Slot 37-43) - Gia vị cơ bản
        inv.setItem(37, createCoinItem(im.buildItem("spice_salt", 1), "CHEF_SALT", data, 200, 1 * 64, jobLevel >= 1, 1));
        inv.setItem(38, createCoinItem(im.buildItem("spice_pepper", 1), "CHEF_PEPPER", data, 200, 1 * 64, jobLevel >= 1, 1));
        inv.setItem(39, createCoinItem(im.buildItem("spice_chili", 1), "CHEF_CHILI", data, 200, 2 * 64, jobLevel >= 5, 5));
        inv.setItem(40, createCoinItem(im.buildItem("spice_garlic", 1), "CHEF_GARLIC", data, 200, 2 * 64, jobLevel >= 5, 5));
        inv.setItem(41, createCoinItem(im.buildItem("spice_sour", 1), "CHEF_SOUR", data, 200, 3 * 64, jobLevel >= 10, 10));
        inv.setItem(42, createCoinItem(im.buildItem("spice_sugar", 1), "CHEF_SUGAR", data, 50, 8 * 64, jobLevel >= 15, 15));
        inv.setItem(43, createCoinItem(im.buildItem("spice_honey", 1), "CHEF_HONEY", data, 50, 10 * 64, jobLevel >= 20, 20));

        // Góc phải dưới: Nút thoát
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
            meta.setDisplayName("§c§l🔒 CHƯA M mở KHÓA");
            meta.setLore(java.util.Arrays.asList("§7Yêu cầu Cấp độ Đầu Bếp: §b" + reqLevel));
            item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName()));
            meta.setLore(java.util.Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§fGiá: §e" + essenceCost + " Tinh Túy Ẩm Thực");
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
            meta.setLore(java.util.Arrays.asList("§7Yêu cầu Cấp độ Đầu Bếp: §b" + reqLevel));
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
        cleanReward.setAmount(1);
        ItemMeta cleanMeta = cleanReward.getItemMeta();

        List<String> cleanLore = cleanMeta.getLore();
        if (cleanLore != null) {
            cleanLore.removeIf(l -> l.contains("Giá:") || l.contains("Giới hạn mua:") || l.contains("---"));
            cleanMeta.setLore(cleanLore);
        }
        cleanReward.setItemMeta(cleanMeta);

        if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int essenceCost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            ItemStack essenceTemplate = im.buildItem("misc_culinary_essence", 1);

            if (im.takeItem(player, essenceTemplate, essenceCost)) {
                data.addPurchase(buyId, 1);
                player.getInventory().addItem(cleanReward);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                openShop(player);
            } else {
                player.sendMessage("§cKhông đủ " + essenceCost + " Tinh Túy Ẩm Thực!");
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
    // CÂY KỸ NĂNG ĐẦU BẾP (CHUẨN 5 HÀNG)
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§dCÂY KỸ NĂNG ĐẦU BẾP"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (data.getLevel("CHEF") < 50) {
            player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!");
            player.closeInventory(); return;
        }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, getSkillTreeGUITitle());
        ItemStack glass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        inv.setItem(13, createSkillIcon("CHEF_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("CHEF")));

        inv.setItem(29, createSkillIcon("CHEF_IRON_STOMACH", Material.RABBIT_STEW, "§e§lDẠ DÀY THÉP", 3, data, "§fGiảm §a50% §ftỉ lệ nhận điểm ngấy.", "§fGiảm §a50% §fthời gian bị cấm ăn.", "§fĐồ ăn tăng thêm §a50% §fthời gian bảo quản."));
        inv.setItem(31, createSkillIcon("CHEF_MAGIC_HANDS", Material.ENCHANTED_GOLDEN_APPLE, "§6§lBÀN TAY MA THUẬT", 3, data, "§fCộng thêm §e20% §ftất cả buff (như Đường).", "§fTăng §e100% §fthời gian độ no nê."));
        inv.setItem(33, createSkillIcon("CHEF_ASCENDED_TASTE", Material.DRAGON_BREATH, "§d§lTHĂNG HOA VỊ GIÁC", 3, data, "§fSố lượng nguyên liệu tối đa trong vạc", "§ftăng từ 9 lên §d12 món§f."));

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

        if (!skillId.equals("CHEF_INFO")) {
            if (data.hasUnlockedSkill("CHEF", skillId)) {
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

        if (slot == 29) processSkillPurchase(player, data, "CHEF_IRON_STOMACH", 2);
        else if (slot == 31) processSkillPurchase(player, data, "CHEF_MAGIC_HANDS", 2);
        else if (slot == 33) processSkillPurchase(player, data, "CHEF_ASCENDED_TASTE", 2);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("CHEF", skillId)) return;
        if (data.getJobPoints("CHEF") >= cost) {
            data.addJobPoints("CHEF", -cost);
            data.unlockSkill("CHEF", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            openSkillTree(player);
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // =========================================
    // TRẢ THƯỞNG NHIỆM VỤ NGÀY / TUẦN ĐẦU BẾP
    // =========================================
    @Override
    public List<String> getDailyRewardLore(int milestone) {
        if (milestone == 1) return Arrays.asList("§d+1 Tinh Túy Ẩm Thực");
        if (milestone == 2) return Arrays.asList("§d+1 Tinh Túy Ẩm Thực");
        return Arrays.asList("§d+2 Tinh Túy Ẩm Thực", "§e+1 Gia Vị Tổng Hợp");
    }

    @Override
    public List<String> getWeeklyRewardLore() {
        return Arrays.asList("§d+10 Tinh Túy Ẩm Thực", "§e+1 Túi Cơm Hộp");
    }

    @Override
    public void giveDailyMilestoneReward(org.bukkit.entity.Player player, int milestone) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        if (milestone == 1) player.getInventory().addItem(im.buildItem("misc_culinary_essence", 1));
        else if (milestone == 2) player.getInventory().addItem(im.buildItem("misc_culinary_essence", 1));
        else if (milestone == 3) {
            player.getInventory().addItem(im.buildItem("misc_culinary_essence", 2));
            player.getInventory().addItem(im.buildItem("spice_all_in_one", 1));
        }
    }

    @Override
    public void giveWeeklyMilestoneReward(org.bukkit.entity.Player player) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        player.getInventory().addItem(im.buildItem("misc_culinary_essence", 10));
        player.getInventory().addItem(im.buildItem("misc_lunch_bag", 1));
    }
}