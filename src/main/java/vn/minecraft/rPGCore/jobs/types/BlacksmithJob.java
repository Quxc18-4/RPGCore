package vn.minecraft.rPGCore.jobs.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.jobs.core.RPGJob;
import vn.minecraft.rPGCore.jobs.quests.AdvancedQuest;
import vn.minecraft.rPGCore.jobs.quests.QuestAction;
import vn.minecraft.rPGCore.jobs.quests.RPGQuest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlacksmithJob extends RPGJob {

    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public BlacksmithJob() {
        super("BLACKSMITH", "Thợ Rèn");
        registerQuests();
    }

    private void registerQuests() {
        questPool.put("DAILY_FORGE_1", new AdvancedQuest("DAILY_FORGE_1", "Khởi Động Lò Rèn", "Thực hiện rèn trang bị 15 lần.", 15, QuestAction.FORGE_ANY, null));
        questPool.put("DAILY_FORGE_2", new AdvancedQuest("DAILY_FORGE_2", "Bàn Tay Ma Thuật", "Đập đồ thành công 5 lần.", 5, QuestAction.FORGE_SUCCESS, null));
        questPool.put("DAILY_FORGE_3", new AdvancedQuest("DAILY_FORGE_3", "Kiên Trì Nhẫn Nại", "Đập đồ thất bại 10 lần.", 10, QuestAction.FORGE_FAIL, null));
        questPool.put("DAILY_FORGE_4", new AdvancedQuest("DAILY_FORGE_4", "Đại Gia Nguyên Liệu", "Tiêu hao 20 Đá Nâng Cấp.", 20, QuestAction.CONSUME_UPGRADE_STONE, null));
        questPool.put("DAILY_FORGE_5", new AdvancedQuest("DAILY_FORGE_5", "Thần Tài Gõ Cửa", "Sử dụng tính năng Rèn Bậc Thầy 1 lần.", 1, QuestAction.USE_MASTER_FORGE, null));
        questPool.put("DAILY_FORGE_6", new AdvancedQuest("DAILY_FORGE_6", "Cảm Giác Thăng Hoa", "Đạt chuỗi đập đồ thành công liên tiếp 3 lần.", 1, QuestAction.FORGE_STREAK, args -> {
            if (args.length > 0 && args[0] instanceof Integer) return (Integer) args[0] >= 3;
            return false;
        }));
        questPool.put("WEEKLY_MASTER_1", new AdvancedQuest("WEEKLY_MASTER_1", "Đột Phá Giới Hạn", "Nâng cấp thành công 3 trang bị đang ở mức +9 trở lên.", 3, QuestAction.FORGE_SUCCESS, args -> {
            if (args.length > 0 && args[0] instanceof Integer) return (Integer) args[0] >= 9;
            return false;
        }));
        questPool.put("WEEKLY_MASTER_2", new AdvancedQuest("WEEKLY_MASTER_2", "Huyền Thoại Rèn Đúc", "Sử dụng tính năng Rèn Bậc Thầy 8 lần.", 8, QuestAction.USE_MASTER_FORGE, null));
        questPool.put("WEEKLY_MASTER_3", new AdvancedQuest("WEEKLY_MASTER_3", "Tiêu Hao Khổng Lồ", "Tiêu hao 300 Đá Nâng Cấp.", 300, QuestAction.CONSUME_UPGRADE_STONE, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() { return questPool; }

    @Override
    public ItemStack getIcon(org.bukkit.entity.Player player) {
        ItemStack icon = new ItemStack(Material.ANVIL);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lNGHỀ THỢ RÈN");
            meta.setLore(getDescription(player));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int currentLevel = data.getLevel("BLACKSMITH");

        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc thầy về thao tác kim loại và nhiệt độ.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + currentLevel + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI NGHỀ CƠ BẢN:");

        double minBonus = getForgeSuccessBonus(currentLevel, 14);
        double maxBonus = getForgeSuccessBonus(currentLevel, 1);
        lore.add("§8▪ §fNội tại Rèn: §aCộng thẳng " + String.format("%.1f", minBonus) + "% -> " + String.format("%.1f", maxBonus) + "% §7(Tùy bậc đồ)");
        lore.add("§8▪ §fTỉ lệ nhận thêm nâng cấp Dòng dõi: §6" + String.format("%.1f", (50.0 + getLineageStarBonus(currentLevel) * 100)) + "%");

        double scrapChance = getScrapRefundChance(currentLevel);
        if (data.hasUnlockedSkill("BLACKSMITH", "BS_HAND_OF_GOD")) scrapChance += 20.0;
        lore.add("§8▪ §fTỉ lệ hoàn trả Mạt Thợ Rèn khi thất bại: §c" + String.format("%.1f", scrapChance) + "%" + (data.hasUnlockedSkill("BLACKSMITH", "BS_HAND_OF_GOD") ? " §d(+20% Kỹ Năng)" : ""));
        lore.add("§8▪ §fXác suất không tiêu hao Đá khi rèn: §b" + String.format("%.1f", getStoneSaveChance(currentLevel)) + "%");

        boolean hasGod = data.hasUnlockedSkill("BLACKSMITH", "BS_HAND_OF_GOD");
        boolean hasSaver = data.hasUnlockedSkill("BLACKSMITH", "BS_MASTER_SAVER");
        boolean hasCharm = data.hasUnlockedSkill("BLACKSMITH", "BS_GUARDIAN_CHARM");
        if (hasGod || hasSaver || hasCharm) {
            lore.add("");
            lore.add("§d§lKỸ NĂNG TỐI THƯỢNG ĐÃ MỞ:");
            if (hasGod) lore.add("§8▪ §eBàn Tay Của Chúa: §f+20% tỉ lệ hoàn mạt thợ rèn.");
            if (hasSaver) lore.add("§8▪ §eBậc Thầy Tiết Kiệm: §fGiảm 20% Mạt khi Rèn Bậc Thầy.");
            if (hasCharm) lore.add("§8▪ §eBùa Hộ Mệnh: §f15% không mất Búa May Mắn/Bùa Bảo Vệ.");
        }

        return lore;
    }

    @Override
    public double getForgeSuccessBonus(int jobLevel, int itemLevel) {
        double power = Math.min(jobLevel, maxLevel) / (double) maxLevel;
        if (itemLevel <= 5) return 10.0 * power;
        else if (itemLevel <= 10) return 5.0 * power;
        else return 1.0 * power;
    }

    @Override
    public double getLineageStarBonus(int level) { return (level * 0.6) / 100.0; }
    @Override
    public double getScrapRefundChance(int level) { return level * 0.6; }
    @Override
    public double getStoneSaveChance(int level) { return level * 0.5; }

    // =========================================
    // CỬA HÀNG THỢ RÈN (VIỀN CAM - RUỘT ĐEN)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§8SHOP THỢ RÈN"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("BLACKSMITH");

        ItemStack orangeGlass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta oMeta = orangeGlass.getItemMeta(); oMeta.setDisplayName(" "); orangeGlass.setItemMeta(oMeta);

        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blMeta = blackGlass.getItemMeta(); blMeta.setDisplayName(" "); blackGlass.setItemMeta(blMeta);

        // Đổ kính viền cam, ruột đen
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, orangeGlass);
            else inv.setItem(i, blackGlass);
        }

        // Đặt vật phẩm vào khu vực trung tâm
        inv.setItem(10, createShopItem(vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 10), "BS_STONE_1", data, 10, "§e5 Mạt", jobLevel >= 1, 1));
        inv.setItem(11, createShopItem(im.buildItem("misc_protection_charm", 1), "BS_CHARM", data, 5, "§e30 Mạt", jobLevel >= 10, 10));
        inv.setItem(12, createShopItem(im.buildItem("misc_lucky_hammer", 1), "BS_LUCKY_HAMMER", data, 3, "§e50 Mạt", jobLevel >= 20, 20));
        inv.setItem(13, createShopItem(im.buildItem("misc_job_change_card", 1), "BS_JOB_CARD", data, 1, "§e200 Mạt", jobLevel >= 1, 1));

        inv.setItem(19, createShopItem(im.buildItem("bag_weapon_common", 1), "BS_W_COM", data, 5, "§e20 Mạt", jobLevel >= 5, 5));
        inv.setItem(20, createShopItem(im.buildItem("bag_weapon_uncommon", 1), "BS_W_UNC", data, 3, "§e50 Mạt", jobLevel >= 15, 15));
        inv.setItem(21, createShopItem(im.buildItem("bag_weapon_rare", 1), "BS_W_RAR", data, 2, "§e120 Mạt", jobLevel >= 25, 25));
        inv.setItem(22, createShopItem(im.buildItem("bag_weapon_epic", 1), "BS_W_EPI", data, 1, "§e300 Mạt", jobLevel >= 40, 40));

        inv.setItem(28, createShopItem(im.buildItem("bag_armor_common", 1), "BS_A_COM", data, 5, "§e20 Mạt", jobLevel >= 5, 5));
        inv.setItem(29, createShopItem(im.buildItem("bag_armor_uncommon", 1), "BS_A_UNC", data, 3, "§e50 Mạt", jobLevel >= 15, 15));
        inv.setItem(30, createShopItem(im.buildItem("bag_armor_rare", 1), "BS_A_RAR", data, 2, "§e120 Mạt", jobLevel >= 25, 25));
        inv.setItem(31, createShopItem(im.buildItem("bag_armor_epic", 1), "BS_A_EPI", data, 1, "§e300 Mạt", jobLevel >= 40, 40));

        inv.setItem(37, createShopItem(new ItemStack(Material.ANVIL), "BS_ANVIL", data, 9999, "§a10 Xu Bạc", jobLevel >= 1, 1));
        inv.setItem(38, createShopItem(new ItemStack(Material.COAL, 64), "BS_COAL", data, 9999, "§a2 Xu Bạc", jobLevel >= 5, 5));
        inv.setItem(39, createShopItem(new ItemStack(Material.DIAMOND_PICKAXE), "BS_DIA_PICKAXE", data, 1, "§a20 Xu Bạc", jobLevel >= 15, 15));

        // NÚT QUAY LẠI (Dời sang Góc phải dưới Slot 53)
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(53, back);

        player.openInventory(inv);
    }

    private ItemStack createShopItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, String price, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);

        if (!isUnlocked) {
            meta.setDisplayName("§c§lCHƯA MỞ KHÓA");
            meta.setLore(java.util.Arrays.asList("§7Yêu cầu Cấp độ Thợ Rèn: §b" + reqLevel));
            item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock && maxStock != 9999) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName()));
            meta.setLore(java.util.Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(""); lore.add("§8§m------------------------");
            lore.add("§fGiá: " + price);
            if (maxStock != 9999) lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
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
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();

        // Check nút back ở Slot 53
        if (slot == 53) { vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI.openStatusMenu(player); return; }

        if (slot == 10) processScrapPurchase(player, data, "BS_STONE_1", 10, 5, vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 10));
        else if (slot == 11) processScrapPurchase(player, data, "BS_CHARM", 5, 30, im.buildItem("misc_protection_charm", 1));
        else if (slot == 12) processScrapPurchase(player, data, "BS_LUCKY_HAMMER", 3, 50, im.buildItem("misc_lucky_hammer", 1));
        else if (slot == 13) processScrapPurchase(player, data, "BS_JOB_CARD", 1, 200, im.buildItem("misc_job_change_card", 1));

        else if (slot == 19) processScrapPurchase(player, data, "BS_W_COM", 5, 20, im.buildItem("bag_weapon_common", 1));
        else if (slot == 20) processScrapPurchase(player, data, "BS_W_UNC", 3, 50, im.buildItem("bag_weapon_uncommon", 1));
        else if (slot == 21) processScrapPurchase(player, data, "BS_W_RAR", 2, 120, im.buildItem("bag_weapon_rare", 1));
        else if (slot == 22) processScrapPurchase(player, data, "BS_W_EPI", 1, 300, im.buildItem("bag_weapon_epic", 1));

        else if (slot == 28) processScrapPurchase(player, data, "BS_A_COM", 5, 20, im.buildItem("bag_armor_common", 1));
        else if (slot == 29) processScrapPurchase(player, data, "BS_A_UNC", 3, 50, im.buildItem("bag_armor_uncommon", 1));
        else if (slot == 30) processScrapPurchase(player, data, "BS_A_RAR", 2, 120, im.buildItem("bag_armor_rare", 1));
        else if (slot == 31) processScrapPurchase(player, data, "BS_A_EPI", 1, 300, im.buildItem("bag_armor_epic", 1));

        else if (slot == 37) processMoneyPurchase(player, data, "BS_ANVIL", 9999, 10 * 64, new ItemStack(Material.ANVIL));
        else if (slot == 38) processMoneyPurchase(player, data, "BS_COAL", 9999, 2 * 64, new ItemStack(Material.COAL, 64));
        else if (slot == 39) processMoneyPurchase(player, data, "BS_DIA_PICKAXE", 1, 200 * 64, new ItemStack(Material.DIAMOND_PICKAXE));
    }

    private void processScrapPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String id, int maxStock, int price, ItemStack reward) {
        if (maxStock != 9999 && data.getPurchasedAmount(id) >= maxStock) { player.sendMessage("§cĐã đạt giới hạn mua trong tháng này!"); return; }
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        org.bukkit.inventory.ItemStack scrapTemplate = im.buildItem("misc_blacksmith_scrap", 1);
        if (scrapTemplate != null && im.takeItem(player, scrapTemplate, price)) {
            data.addPurchase(id, 1); player.getInventory().addItem(reward);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f); openShop(player);
        } else {
            player.sendMessage("§cKhông đủ " + price + " Mạt Thợ Rèn trong túi đồ!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void processMoneyPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String id, int maxStock, long price, ItemStack reward) {
        if (maxStock != 9999 && data.getPurchasedAmount(id) >= maxStock) { player.sendMessage("§cĐã đạt giới hạn mua trong tháng này!"); return; }
        vn.minecraft.rPGCore.managers.CurrencyManager cm = new vn.minecraft.rPGCore.managers.CurrencyManager(vn.minecraft.rPGCore.RPGCore.getInstance());
        if (cm.takePhysicalCurrency(player, price)) {
            data.addPurchase(id, 1); player.getInventory().addItem(reward);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f); openShop(player);
        } else {
            player.sendMessage("§cKhông đủ tiền trong túi đồ!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // =========================================
    // CÂY KỸ NĂNG THỢ RÈN (QUY CHUẨN ĐẸP 5 HÀNG)
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§dCÂY KỸ NĂNG THỢ RÈN"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (data.getLevel("BLACKSMITH") < 50) {
            player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!");
            player.closeInventory(); return;
        }

        // Đổi thành 45 ô (5 hàng x 9 ô)
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, getSkillTreeGUITitle());
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        // Hàng 2: Sách thông tin đặt ở Slot 13 (Chính giữa hàng 2)
        inv.setItem(13, createSkillIcon("BS_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("BLACKSMITH")));

        // Hàng 4: 3 Kỹ năng tối thượng chia đều ở các ô 29, 31, 33
        inv.setItem(29, createSkillIcon("BS_HAND_OF_GOD", Material.ENCHANTED_GOLDEN_APPLE, "§e§lBÀN TAY NGHỆ NHÂN", 2, data, "§fCó §a20% xác suất §fnhận lại", "§fMạt Thợ Rèn khi đập đồ thất bại."));
        inv.setItem(31, createSkillIcon("BS_MASTER_SAVER", Material.GOLD_INGOT, "§6§lBẬC THẦY TIẾT KIỆM", 2, data, "§fGiảm §e20% §fchi phí Mạt Thợ Rèn", "§fkhi sử dụng nút Rèn Bậc Thầy."));
        inv.setItem(33, createSkillIcon("BS_GUARDIAN_CHARM", Material.TOTEM_OF_UNDYING, "§d§lBÙA HỘ MỆNH", 2, data, "§fCó §a15% xác suất §fkhông tiêu hao", "§fBúa May Mắn và Bùa Bảo Vệ khi rèn."));

        // Hàng 5: Nút quay lại chuyển sang Góc phải dưới (Slot 44)
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(44, back);

        player.openInventory(inv);
    }

    private ItemStack createSkillIcon(String skillId, Material mat, String name, int jpCost, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String... desc) {
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(); lore.add("§7Nội tại Kỹ năng Tối thượng"); lore.add("");
        for (String line : desc) lore.add("§8▪ " + line);
        lore.add("");

        if (!skillId.equals("BS_INFO")) {
            if (data.hasUnlockedSkill("BLACKSMITH", skillId)) {
                lore.add("§a§l✔ ĐÃ KÍCH HOẠT VÀ ĐANG HOẠT ĐỘNG");
            } else {
                lore.add("§e§lCLICK ĐỂ HỌC KỸ NĂNG NÀY!"); lore.add("§fTiêu hao: §d" + jpCost + " JP");
            }
        }
        meta.setLore(lore); item.setItemMeta(meta); return item;
    }

    @Override
    public void handleSkillTreeClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int slot = event.getRawSlot();

        // Check nút back ở Slot 44
        if (slot == 44) { vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI.openStatusMenu(player); return; }

        if (slot == 29) processSkillPurchase(player, data, "BS_HAND_OF_GOD", 2);
        else if (slot == 31) processSkillPurchase(player, data, "BS_MASTER_SAVER", 2);
        else if (slot == 33) processSkillPurchase(player, data, "BS_GUARDIAN_CHARM", 2);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("BLACKSMITH", skillId)) return;
        if (data.getJobPoints("BLACKSMITH") >= cost) {
            data.addJobPoints("BLACKSMITH", -cost); data.unlockSkill("BLACKSMITH", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); openSkillTree(player);
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @Override
    public List<String> getDailyRewardLore(int milestone) {
        if (milestone == 1) return java.util.Arrays.asList("§e+1 Mạt Thợ Rèn");
        if (milestone == 2) return java.util.Arrays.asList("§e+1 Mạt Thợ Rèn");
        return java.util.Arrays.asList("§e+2 Mạt Thợ Rèn", "§a+10 Đá Nâng Cấp");
    }

    @Override
    public List<String> getWeeklyRewardLore() { return java.util.Arrays.asList("§e+20 Mạt Thợ Rèn"); }

    @Override
    public void giveDailyMilestoneReward(org.bukkit.entity.Player player, int milestone) {
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        if (milestone == 1) player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 1));
        else if (milestone == 2) player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 1));
        else if (milestone == 3) {
            player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 2));
            player.getInventory().addItem(vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 10));
        }
    }

    @Override
    public void giveWeeklyMilestoneReward(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 20));
    }
}