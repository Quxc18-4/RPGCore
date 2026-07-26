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

public class AlchemistJob extends RPGJob {

    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public AlchemistJob() {
        super("ALCHEMIST", "Dược Sư");
        registerQuests();
    }

    private void registerQuests() {
        // ==========================================
        // DAILY QUESTS (NHIỆM VỤ NGÀY)
        // ==========================================
        questPool.put("DAILY_ALCH_1", new AdvancedQuest("DAILY_ALCH_1", "Thu Hoạch Đầu Mùa", "Thu thập thành công 10 cây Thảo dược bất kỳ ngoài tự nhiên.", 10, QuestAction.ALCH_HARVEST_ANY, null));
        questPool.put("DAILY_ALCH_2", new AdvancedQuest("DAILY_ALCH_2", "Dược Sư Kiên Trì", "Nấu thuốc thất bại (Ra thuốc [Kinh tởm]) 2 lần.", 2, QuestAction.BREW_FAIL, null));
        questPool.put("DAILY_ALCH_3", new AdvancedQuest("DAILY_ALCH_3", "Thử Nghiệm Dung Môi", "Tiến hành thực hiện nấu thuốc 5 lần (Bất kể thành bại).", 5, QuestAction.BREW_ANY, null));
        questPool.put("DAILY_ALCH_4", new AdvancedQuest("DAILY_ALCH_4", "Tay Nghề Thành Thục", "Nấu thuốc thành công xuất sắc 3 lần.", 3, QuestAction.BREW_SUCCESS, null));

        // ==========================================
        // WEEKLY QUESTS (NHIỆM VỤ TUẦN)
        // ==========================================
        questPool.put("WEEKLY_ALCH_1", new AdvancedQuest("WEEKLY_ALCH_1", "Thợ Săn Thảo Dược Quý", "Thu thập 15 cây Thảo dược có phẩm chất từ RARE trở lên.", 15, QuestAction.ALCH_HARVEST_RARE, null));
        questPool.put("WEEKLY_ALCH_2", new AdvancedQuest("WEEKLY_ALCH_2", "Đại Gia Tinh Chất", "Tìm thấy và tích lũy được 20 Tinh Chất Ma Lực.", 20, QuestAction.COLLECT_MANA_ESSENCE, null));
        questPool.put("WEEKLY_ALCH_3", new AdvancedQuest("WEEKLY_ALCH_3", "Đơn Hàng Hoàng Gia", "Chế tạo thành công 2 bình thuốc đạt phẩm chất [Hoàn hảo] hoặc [Cường hóa].", 2, QuestAction.BREW_PERFECT, null));
        questPool.put("WEEKLY_ALCH_4", new AdvancedQuest("WEEKLY_ALCH_4", "Bậc Thầy Nghiên Cứu", "Thực hiện nấu thuốc tổng cộng 25 lần (Bất kể thành bại).", 25, QuestAction.BREW_ANY, null));
        questPool.put("WEEKLY_ALCH_5", new AdvancedQuest("WEEKLY_ALCH_5", "Danh Y Đại Tài", "Nấu thuốc thành công đạt chuẩn thuộc tính dược liệu 15 lần.", 15, QuestAction.BREW_SUCCESS, null));
        questPool.put("WEEKLY_ALCH_6", new AdvancedQuest("WEEKLY_ALCH_6", "Học Hỏi Từ Sai Lầm", "Nấu thuốc thất bại làm nổ vạc tổng cộng 5 lần trong tuần.", 5, QuestAction.BREW_FAIL, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() {
        return questPool;
    }

    @Override
    public ItemStack getIcon(org.bukkit.entity.Player player) {
        ItemStack icon = new ItemStack(Material.BREWING_STAND);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§lNGHỀ DƯỢC SƯ");
            meta.setLore(getDescription(player));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int level = data.getLevel("ALCHEMIST");

        // Tính toán các chỉ số hiển thị động dựa trên Level Dược Sư
        double refundChance = Math.min(level * 0.4, 20.0);
        double statMultiplier = Math.min(100.0 + ((level - 1) * 1.0), 150.0);
        double successBonus = Math.min(level * 0.3, 15.0);
        double toxDecay = Math.min(level * 0.6, 30.0);
        double toxResist = Math.min(level * 0.3, 15.0);
        double harvestBonus = Math.min(level * 0.6, 30.0);

        List<String> lore = new ArrayList<>();
        lore.add("§7Chuyên gia pha chế dược liệu, bậc thầy");
        lore.add("§7kiểm soát độc tố và tinh hoa ma lực.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §d" + level + "§8/§d" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI CƠ BẢN:");
        lore.add("§8▪ §fGiác Quan Tụ Linh: §e" + String.format("%.1f", refundChance) + "% §fcơ hội hoàn trả nguyên liệu.");
        lore.add("§8▪ §fDung Môi Thuần Khiết: §a" + String.format("%.0f", statMultiplier) + "% §fHiệu quả Dược liệu & §a+" + String.format("%.1f", successBonus) + "% §fTỉ lệ thành công.");
        lore.add("§8▪ §fĐề Kháng Kháng Sinh: §b+" + String.format("%.1f", toxDecay) + "% §fTốc độ xả độc & §c-" + String.format("%.1f", toxResist) + "% §fĐộc nhận vào.");
        lore.add("§8▪ §fThu Hoạch Đại Trà: §6" + String.format("%.1f", harvestBonus) + "% §fTỉ lệ nhận thêm 1 Thảo dược khi hái.");

        boolean hasUlt1 = data.hasUnlockedSkill("ALCHEMIST", "ALCH_ULT_1");
        boolean hasUlt2 = data.hasUnlockedSkill("ALCHEMIST", "ALCH_ULT_2");
        boolean hasUlt3 = data.hasUnlockedSkill("ALCHEMIST", "ALCH_ULT_3");

        if (hasUlt1 || hasUlt2 || hasUlt3) {
            lore.add("");
            lore.add("§d§lKỸ NĂNG TỐI THƯỢNG ĐÃ MỞ:");
            if (hasUlt1) lore.add("§8▪ §eNhà Pha Chế Đại Tài: §f+10% Tỉ lệ thành công, 33% cơ hội thuốc x2 sức mạnh.");
            if (hasUlt2) lore.add("§8▪ §eKháng Thể Độc Tố: §f+70% Xả độc, 50% cơ hội miễn nhiễm Độc tố từ thuốc.");
            if (hasUlt3) lore.add("§8▪ §eMẹ Thiên Nhiên: §f+50% Tỉ lệ rớt thêm thuốc, 15% bạo kích rớt x3.");
        }
        return lore;
    }

    // =========================================
    // SHOP DƯỢC SƯ (Cấu trúc tương tự Merchant)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§5CỬA HÀNG DƯỢC SƯ"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("ALCHEMIST");

        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta boMeta = border.getItemMeta(); boMeta.setDisplayName(" "); border.setItemMeta(boMeta);
        for (int i = 0; i < 54; i++) { if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, border); }

        // --- HÀNG 2 (Slot 10-16): ĐẠO CỤ ĐẶC QUYỀN (Mua bằng Tinh Chất) ---
        inv.setItem(10, createEssenceItem(im.buildItem("misc_job_change_card", 1), "ALCH_CHANGE", data, 1, 100, true, 1));
        inv.setItem(11, createEssenceItem(im.buildItem("alch_thermal_flask_empty", 1), "ALCH_FLASK", data, 5, 20, jobLevel >= 5, 5));
        inv.setItem(12, createEssenceItem(im.buildItem("alch_silver_spoon", 1), "ALCH_SPOON", data, 3, 50, jobLevel >= 15, 15));
        inv.setItem(13, createEssenceItem(im.buildItem("alch_magnifying_glass", 1), "ALCH_GLASS", data, 2, 40, jobLevel >= 20, 20));
        inv.setItem(14, createEssenceItem(im.buildItem("alch_lost_recipe", 1), "ALCH_RECIPE", data, 5, 150, jobLevel >= 30, 30));
        inv.setItem(15, createEssenceItem(im.buildItem("catalyst_purity", 1), "ALCH_CAT_PURITY", data, 10, 30, jobLevel >= 10, 10));
        inv.setItem(16, createEssenceItem(im.buildItem("catalyst_chaos", 1), "ALCH_CAT_CHAOS", data, 5, 60, jobLevel >= 40, 40));

        // =========================================================
        // HÀNG 3 & HÀNG 4: XOAY TUA THẢO DƯỢC RANDOM THEO THÁNG
        // =========================================================
        Calendar cal = Calendar.getInstance();
        Random shopRand = new Random(cal.get(Calendar.YEAR) * 100L + cal.get(Calendar.MONTH));

        List<String> rareHerbs = Arrays.asList("herb_phoenix_tear", "herb_prism_orchid", "herb_frost_sprout", "herb_dark_lotus", "herb_ancient_blood_bud", "herb_magma_seed", "herb_immortal_bloom", "herb_sword_grass", "herb_earth_heart", "herb_abyss_algae", "herb_yggdrasil_sprout");
        List<String> commonHerbs = Arrays.asList("herb_blood_grass", "herb_vitality_shroom", "herb_swift_maple", "herb_muscle_root", "herb_ruby_petal", "herb_mana_shroom", "herb_glow_bloom", "herb_blue_fern", "herb_stone_root", "herb_water_lung_moss", "herb_dry_vine", "herb_wolf_sand_seed");

        Collections.shuffle(rareHerbs, shopRand);
        Collections.shuffle(commonHerbs, shopRand);

        // --- HÀNG 3 (Slot 19-25): Thảo dược Rare+ (Mua bằng Tinh chất) ---
        for (int i = 0; i < 7; i++) {
            if (i >= rareHerbs.size()) break;
            int cost = 2 + shopRand.nextInt(4); // Random giá từ 2 - 5 Tinh chất
            inv.setItem(19 + i, createEssenceItem(im.buildItem(rareHerbs.get(i), 1), "ALCH_ROT_RARE_" + i, data, 5, cost, jobLevel >= 15, 15));
        }

        // --- HÀNG 4 (Slot 28-34): Thảo dược Phổ thông (Mua bằng Xu Đồng) ---
        for (int i = 0; i < 7; i++) {
            if (i >= commonHerbs.size()) break;
            int cost = 1000 + shopRand.nextInt(2001); // Random giá từ 1000 - 3000 Xu Đồng
            inv.setItem(28 + i, createCoinItem(im.buildItem(commonHerbs.get(i), 1), "ALCH_ROT_COM_" + i, data, 50, cost, jobLevel >= 5, 5));
        }

        // --- HÀNG 5 (Slot 37-42): HÀNG VANILLA & NƯỚC NỀN (Mua bằng Xu Đồng) ---
        inv.setItem(37, createCoinItem(new ItemStack(Material.CAULDRON), "ALCH_VAN_CAULDRON", data, 5, 40960, true, 1));
        inv.setItem(38, createCoinItem(new ItemStack(Material.GLASS_BOTTLE), "ALCH_VAN_BOTTLE", data, 64, 10, true, 1));
        inv.setItem(39, createCoinItem(im.buildItem("water_sweet", 1), "ALCH_WATER_SWEET", data, 20, 8192, true, 1));
        inv.setItem(40, createCoinItem(im.buildItem("water_tasteless", 1), "ALCH_WATER_TASTELESS", data, 20, 8192, true, 1));
        inv.setItem(41, createCoinItem(im.buildItem("water_salty", 1), "ALCH_WATER_SALTY", data, 20, 8192, true, 1));
        inv.setItem(42, createCoinItem(im.buildItem("water_cave", 1), "ALCH_WATER_CAVE", data, 20, 8192, jobLevel >= 10, 10));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(53, back);

        player.openInventory(inv);
    }

    // =========================================================
    // HELPER: TẠO VẬT PHẨM MUA BẰNG TINH CHẤT
    // =========================================================
    private ItemStack createEssenceItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int tokenCost, boolean isUnlocked, int reqLevel) {
        if (baseItem == null) return new ItemStack(Material.AIR);
        ItemStack item = baseItem.clone(); ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);

        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA"); meta.setLore(Arrays.asList("§7Yêu cầu Cấp độ Dược Sư: §d" + reqLevel)); item.setType(Material.BARRIER);
        } else if (maxStock != 99 && maxStock != 999 && currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName())); meta.setLore(Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(""); lore.add("§8§m------------------------");
            lore.add("§fGiá: §b" + tokenCost + " Tinh Chất Ma Lực");
            if (maxStock < 90) lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");

            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, tokenCost);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING, purchaseId);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, maxStock);
            meta.setLore(lore);
        }
        item.setItemMeta(meta); return item;
    }

    // =========================================================
    // HELPER: TẠO VẬT PHẨM MUA BẰNG XU ĐỒNG
    // =========================================================
    private ItemStack createCoinItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int coinCost, boolean isUnlocked, int reqLevel) {
        if (baseItem == null) return new ItemStack(Material.AIR);
        ItemStack item = baseItem.clone(); ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);

        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA"); meta.setLore(Arrays.asList("§7Yêu cầu Cấp độ Dược Sư: §d" + reqLevel)); item.setType(Material.BARRIER);
        } else if (maxStock != 99 && maxStock != 999 && currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName())); meta.setLore(Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(""); lore.add("§8§m------------------------");
            lore.add("§fGiá: §6" + coinCost + " Xu Đồng");
            if (maxStock < 90) lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");

            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, coinCost);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_id"), org.bukkit.persistence.PersistentDataType.STRING, purchaseId);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_max"), org.bukkit.persistence.PersistentDataType.INTEGER, maxStock);
            meta.setLore(lore);
        }
        item.setItemMeta(meta); return item;
    }

    // =========================================================
    // XỬ LÝ CLICK MUA SẮM (Tự nhận diện 2 loại tiền tệ)
    // =========================================================
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

        if (maxStock != 99 && maxStock != 999 && data.getPurchasedAmount(buyId) >= maxStock) {
            player.sendMessage("§cĐã đạt giới hạn mua sản phẩm này trong tháng!"); return;
        }

        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        ItemStack cleanReward = clicked.clone(); cleanReward.setAmount(1);
        ItemMeta cleanMeta = cleanReward.getItemMeta();
        List<String> cleanLore = cleanMeta.getLore();
        if (cleanLore != null) { cleanLore.removeIf(l -> l.contains("Giá:") || l.contains("Giới hạn mua:") || l.contains("---")); cleanMeta.setLore(cleanLore); }
        cleanReward.setItemMeta(cleanMeta);

        boolean paymentSuccess = false;

        // KIỂM TRA MUA BẰNG TINH CHẤT
        if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int cost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_essence_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            ItemStack essenceCost = im.buildItem("essence_mana", 1);

            if (im.takeItem(player, essenceCost, cost)) {
                paymentSuccess = true;
            } else {
                player.sendMessage("§cKhông đủ " + cost + " Tinh Chất Ma Lực!");
            }
        }
        // KIỂM TRA MUA BẰNG TIỀN TỆ HỆ THỐNG
        else if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int cost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_coin_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);

            if (vn.minecraft.rPGCore.RPGCore.getInstance().getCurrencyManager().takePhysicalCurrency(player, cost)) {
                paymentSuccess = true;
            } else {
                player.sendMessage("§cBạn không có đủ " + cost + " Xu Đồng để mua vật phẩm này!");
            }
        }

        if (paymentSuccess) {
            data.addPurchase(buyId, 1);
            player.getInventory().addItem(cleanReward).values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
            openShop(player); // Load lại UI
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // =========================================
    // CÂY KỸ NĂNG DƯỢC SƯ
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§5CÂY KỸ NĂNG DƯỢC SƯ"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (data.getLevel("ALCHEMIST") < 50) {
            player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!");
            player.closeInventory();
            return;
        }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, getSkillTreeGUITitle());
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        inv.setItem(13, createSkillIcon("ALCH_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("ALCHEMIST")));
        inv.setItem(29, createSkillIcon("ALCH_ULT_1", Material.BREWING_STAND, "§e§lNHÀ PHA CHẾ ĐẠI TÀI", 3, data, "§fCộng thêm 10% Tỉ lệ thành công.", "§fKhi nấu thuốc có 33% cơ hội", "§fx2 sức mạnh dược liệu (Đạt 200%)."));
        inv.setItem(31, createSkillIcon("ALCH_ULT_2", Material.FERMENTED_SPIDER_EYE, "§c§lKHÁNG THỂ ĐỘC TỐ", 3, data, "§fTăng 70% tốc độ đào thải độc tố.", "§f50% cơ hội miễn nhiễm hoàn toàn", "§fđiểm độc khi uống thuốc."));
        inv.setItem(33, createSkillIcon("ALCH_ULT_3", Material.WHEAT_SEEDS, "§b§lMẸ THIÊN NHIÊN", 3, data, "§fCộng dồn nội tại: 80% cơ hội rớt", "§fthêm 1 thảo dược khi hái.", "§fCó 15% cơ hội bạo kích rớt x3."));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(44, back);
        player.openInventory(inv);
    }

    private ItemStack createSkillIcon(String skillId, Material mat, String name, int jpCost, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String... desc) {
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(); lore.add("§7Nội tại Kỹ năng Tối thượng"); lore.add("");
        for (String line : desc) lore.add("§8▪ " + line); lore.add("");
        if (!skillId.equals("ALCH_INFO")) {
            if (data.hasUnlockedSkill("ALCHEMIST", skillId)) lore.add("§a§l✔ ĐÃ KÍCH HOẠT");
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
        if (slot == 29) processSkillPurchase(player, data, "ALCH_ULT_1", 3);
        else if (slot == 31) processSkillPurchase(player, data, "ALCH_ULT_2", 3);
        else if (slot == 33) processSkillPurchase(player, data, "ALCH_ULT_3", 3);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("ALCHEMIST", skillId)) return;
        if (data.getJobPoints("ALCHEMIST") >= cost) {
            data.addJobPoints("ALCHEMIST", -cost); data.unlockSkill("ALCHEMIST", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); openSkillTree(player);
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!"); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // =========================================
    // HỆ THỐNG TRẢ THƯỞNG NHIỆM VỤ DƯỢC SƯ
    // =========================================
    @Override
    public List<String> getDailyRewardLore(int milestone) {
        if (milestone == 1) return Arrays.asList("§b+1 Tinh Chất Ma Lực");
        if (milestone == 2) return Arrays.asList("§b+1 Tinh Chất Ma Lực");
        return Arrays.asList("§b+2 Tinh Chất Ma Lực");
    }

    @Override
    public List<String> getWeeklyRewardLore() {
        return Arrays.asList("§b+15 Tinh Chất Ma Lực");
    }

    @Override
    public void giveDailyMilestoneReward(org.bukkit.entity.Player player, int milestone) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        int essenceCount = milestone == 1 ? 1 : (milestone == 2 ? 1 : 2);

        ItemStack essence = im.buildItem("essence_mana", essenceCount);
        if (essence != null) player.getInventory().addItem(essence);

//        if (milestone == 3) {
//            vn.minecraft.rPGCore.RPGCore.getInstance().getCurrencyManager().givePhysicalCurrency(player, 20480); // 5 Bạc = 20480 Đồng
//        }
    }

    @Override
    public void giveWeeklyMilestoneReward(org.bukkit.entity.Player player) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        ItemStack essence = im.buildItem("essence_mana", 15);
        if (essence != null) player.getInventory().addItem(essence);

        //vn.minecraft.rPGCore.RPGCore.getInstance().getCurrencyManager().givePhysicalCurrency(player, 122880); // 30 Bạc = 122880 Đồng
    }

    // =====================================================================
    // CÁC HÀM HELPER TIỆN ÍCH DÀNH CHO BREWING ENGINE & TOXICITY MANAGER
    // =====================================================================

    public static boolean hasUltimate(org.bukkit.entity.Player player, String ultId) {
        return vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player).hasUnlockedSkill("ALCHEMIST", ultId);
    }

    /** Trả về hệ số nhân stats dược liệu: Mặc định 0.5 (Người thường), 1.0 -> 1.5 (Alchemist), 2.0 (Nếu nổ bạo kích Ult 1) */
    public static double getStatMultiplier(org.bukkit.entity.Player player, boolean[] isCritEnhanced) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("ALCHEMIST")) return 0.5;

        int level = data.getLevel("ALCHEMIST");
        double multiplier = 1.0 + ((level - 1) * 0.01);

        // Kích hoạt Ultimate 1: 33% cơ hội x2 sức mạnh (Đạt 200%)
        if (hasUltimate(player, "ALCH_ULT_1") && Math.random() <= 0.33) {
            multiplier = 2.0;
            isCritEnhanced[0] = true;
        }
        return Math.min(multiplier, 2.0);
    }

    /** Trả về Tỉ lệ thành công cộng thêm trên Vạc pha chế (Tối đa 15% + 10% Ult 1) */
    public static double getSuccessRateBonus(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("ALCHEMIST")) return 0.0;

        int level = data.getLevel("ALCHEMIST");
        double bonus = level * 0.3;

        if (hasUltimate(player, "ALCH_ULT_1")) bonus += 10.0;
        return bonus;
    }

    /** Trả về % tăng tốc độ xả độc tố (Tối đa 30% + 70% Ult 2) */
    public static double getToxDecayBonus(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("ALCHEMIST")) return 0.0;

        int level = data.getLevel("ALCHEMIST");
        double bonus = level * 0.6;

        if (hasUltimate(player, "ALCH_ULT_2")) bonus += 70.0;
        return bonus;
    }

    /** Trả về % điểm độc được giảm bớt khi nạp vào cơ thể (Tối đa 15%) */
    public static double getToxResistance(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("ALCHEMIST")) return 0.0;
        return Math.min(data.getLevel("ALCHEMIST") * 0.3, 15.0);
    }
}