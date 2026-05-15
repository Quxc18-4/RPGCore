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

    // Kho chứa nhiệm vụ của Thợ Rèn
    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public BlacksmithJob() {
        super("BLACKSMITH", "Thợ Rèn");
        registerQuests(); // Đăng ký nhiệm vụ ngay khi khởi tạo
    }

    private void registerQuests() {
        // =========================================
        // KHO NHIỆM VỤ NGÀY (DAILY POOL)
        // =========================================
        questPool.put("DAILY_FORGE_1", new AdvancedQuest("DAILY_FORGE_1", "Khởi Động Lò Rèn", "Thực hiện rèn trang bị 15 lần.", 15, QuestAction.FORGE_ANY, null));

        questPool.put("DAILY_FORGE_2", new AdvancedQuest("DAILY_FORGE_2", "Bàn Tay Ma Thuật", "Đập đồ thành công 5 lần.", 5, QuestAction.FORGE_SUCCESS, null));

        questPool.put("DAILY_FORGE_3", new AdvancedQuest("DAILY_FORGE_3", "Kiên Trì Nhẫn Nại", "Đập đồ thất bại 10 lần.", 10, QuestAction.FORGE_FAIL, null));

        questPool.put("DAILY_FORGE_4", new AdvancedQuest("DAILY_FORGE_4", "Đại Gia Nguyên Liệu", "Tiêu hao 20 Đá Nâng Cấp.", 20, QuestAction.CONSUME_UPGRADE_STONE, null));

        questPool.put("DAILY_FORGE_5", new AdvancedQuest("DAILY_FORGE_5", "Thần Tài Gõ Cửa", "Sử dụng tính năng Rèn Bậc Thầy 1 lần.", 1, QuestAction.USE_MASTER_FORGE, null));

        questPool.put("DAILY_FORGE_6", new AdvancedQuest("DAILY_FORGE_6", "Cảm Giác Thăng Hoa", "Đạt chuỗi đập đồ thành công liên tiếp 3 lần.", 1, QuestAction.FORGE_STREAK, args -> {
            if (args.length > 0 && args[0] instanceof Integer) return (Integer) args[0] >= 3;
            return false;
        }));

        // =========================================
        // KHO NHIỆM VỤ TUẦN (WEEKLY POOL)
        // =========================================
        questPool.put("WEEKLY_MASTER_1", new AdvancedQuest("WEEKLY_MASTER_1", "Đột Phá Giới Hạn", "Nâng cấp thành công 3 trang bị đang ở mức +9 trở lên.", 3, QuestAction.FORGE_SUCCESS, args -> {
            if (args.length > 0 && args[0] instanceof Integer) return (Integer) args[0] >= 9;
            return false;
        }));

        questPool.put("WEEKLY_MASTER_2", new AdvancedQuest("WEEKLY_MASTER_2", "Huyền Thoại Rèn Đúc", "Sử dụng tính năng Rèn Bậc Thầy 8 lần.", 8, QuestAction.USE_MASTER_FORGE, null));

        questPool.put("WEEKLY_MASTER_3", new AdvancedQuest("WEEKLY_MASTER_3", "Tiêu Hao Khổng Lồ", "Tiêu hao 300 Đá Nâng Cấp.", 300, QuestAction.CONSUME_UPGRADE_STONE, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() {
        return questPool;
    }

    @Override
    public ItemStack getIcon(int currentLevel) {
        ItemStack icon = new ItemStack(Material.ANVIL);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lNGHỀ THỢ RÈN");
            meta.setLore(getDescription(currentLevel));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(int currentLevel) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc thầy về thao tác kim loại và nhiệt độ.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + currentLevel + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI NGHỀ:");
        lore.add("§8▪ §fTỉ lệ thành công khi rèn: §a+" + String.format("%.1f", getForgeSuccessBonus(currentLevel)) + "%");
        lore.add("§8▪ §fTỉ lệ nhận thêm nâng cấp Dòng dõi: §6" + String.format("%.1f", (50.0 + getLineageStarBonus(currentLevel) * 100)) + "%");
        lore.add("§8▪ §fTỉ lệ hoàn trả Mạt Thợ Rèn khi thất bại: §c" + String.format("%.1f", getScrapRefundChance(currentLevel)) + "%");
        lore.add("§8▪ §fXác suất không tiêu hao Đá khi rèn trang bị: §b" + String.format("%.1f", getStoneSaveChance(currentLevel)) + "%");
        return lore;
    }

    @Override
    public double getForgeSuccessBonus(int level) { return level * 0.2; }

    @Override
    public double getLineageStarBonus(int level) { return (level * 0.6) / 100.0; }

    @Override
    public double getScrapRefundChance(int level) { return level * 0.6; }

    @Override
    public double getStoneSaveChance(int level) { return level * 0.5; }

    // =========================================
    // TRIỂN KHAI CỬA HÀNG NGHỀ (SHOP CÓ GIỚI HẠN)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§8SHOP THỢ RÈN"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("BLACKSMITH");

        ItemStack glass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // --- HÀNG 1: ĐỒ TIỆN ÍCH THỢ RÈN ---
        inv.setItem(10, createShopItem(vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 10), "BS_STONE_1", data, 10, "§e5 Mạt", jobLevel >= 1, 1));
        inv.setItem(11, createShopItem(im.buildItem("misc_protection_charm", 1), "BS_CHARM", data, 5, "§e30 Mạt", jobLevel >= 10, 10));
        inv.setItem(12, createShopItem(im.buildItem("misc_lucky_hammer", 1), "BS_LUCKY_HAMMER", data, 3, "§e50 Mạt", jobLevel >= 20, 20));
        inv.setItem(13, createShopItem(im.buildItem("misc_job_change_card", 1), "BS_JOB_CARD", data, 1, "§e200 Mạt", jobLevel >= 1, 1));

        // --- HÀNG 2: TÚI VŨ KHÍ (Yêu cầu cấp nghề 5, 15, 25, 40) ---
        inv.setItem(19, createShopItem(im.buildItem("bag_weapon_common", 1), "BS_W_COM", data, 5, "§e20 Mạt", jobLevel >= 5, 5));
        inv.setItem(20, createShopItem(im.buildItem("bag_weapon_uncommon", 1), "BS_W_UNC", data, 3, "§e50 Mạt", jobLevel >= 15, 15));
        inv.setItem(21, createShopItem(im.buildItem("bag_weapon_rare", 1), "BS_W_RAR", data, 2, "§e120 Mạt", jobLevel >= 25, 25));
        inv.setItem(22, createShopItem(im.buildItem("bag_weapon_epic", 1), "BS_W_EPI", data, 1, "§e300 Mạt", jobLevel >= 40, 40));

        // --- HÀNG 3: TÚI GIÁP (Yêu cầu cấp nghề 5, 15, 25, 40) ---
        inv.setItem(28, createShopItem(im.buildItem("bag_armor_common", 1), "BS_A_COM", data, 5, "§e20 Mạt", jobLevel >= 5, 5));
        inv.setItem(29, createShopItem(im.buildItem("bag_armor_uncommon", 1), "BS_A_UNC", data, 3, "§e50 Mạt", jobLevel >= 15, 15));
        inv.setItem(30, createShopItem(im.buildItem("bag_armor_rare", 1), "BS_A_RAR", data, 2, "§e120 Mạt", jobLevel >= 25, 25));
        inv.setItem(31, createShopItem(im.buildItem("bag_armor_epic", 1), "BS_A_EPI", data, 1, "§e300 Mạt", jobLevel >= 40, 40));

        // --- HÀNG 4: HÀNG VANILLA MUA BẰNG XU ---
        inv.setItem(37, createShopItem(new ItemStack(Material.ANVIL), "BS_ANVIL", data, 9999, "§a500 Xu", jobLevel >= 1, 1));
        inv.setItem(38, createShopItem(new ItemStack(Material.COAL, 64), "BS_COAL", data, 9999, "§a1000 Xu", jobLevel >= 5, 5));
        inv.setItem(39, createShopItem(new ItemStack(Material.DIAMOND_PICKAXE), "BS_DIA_PICKAXE", data, 1, "§a5000 Xu", jobLevel >= 15, 15));

        player.openInventory(inv);
    }

    private ItemStack createShopItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, String price, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();

        int currentBought = data.getPurchasedAmount(purchaseId);

        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA");
            meta.setLore(java.util.Arrays.asList("§7Yêu cầu Cấp độ Thợ Rèn: §b" + reqLevel));
            item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock && maxStock != 9999) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName()));
            meta.setLore(java.util.Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY", "§7Hãy quay lại vào đầu tháng sau!"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§fGiá: " + price);
            if (maxStock != 9999) {
                lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
            } else {
                lore.add("§fGiới hạn mua: §aKhông giới hạn");
            }
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

        // Xử lý Click Hàng 1
        if (slot == 10) processScrapPurchase(player, data, "BS_STONE_1", 10, 5, vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 10));
        else if (slot == 11) processScrapPurchase(player, data, "BS_CHARM", 5, 30, im.buildItem("misc_protection_charm", 1));
        else if (slot == 12) processScrapPurchase(player, data, "BS_LUCKY_HAMMER", 3, 50, im.buildItem("misc_lucky_hammer", 1));
        else if (slot == 13) processScrapPurchase(player, data, "BS_JOB_CARD", 1, 200, im.buildItem("misc_job_change_card", 1));

            // Xử lý Click Hàng 2 (Vũ khí)
        else if (slot == 19) processScrapPurchase(player, data, "BS_W_COM", 5, 20, im.buildItem("bag_weapon_common", 1));
        else if (slot == 20) processScrapPurchase(player, data, "BS_W_UNC", 3, 50, im.buildItem("bag_weapon_uncommon", 1));
        else if (slot == 21) processScrapPurchase(player, data, "BS_W_RAR", 2, 120, im.buildItem("bag_weapon_rare", 1));
        else if (slot == 22) processScrapPurchase(player, data, "BS_W_EPI", 1, 300, im.buildItem("bag_weapon_epic", 1));

            // Xử lý Click Hàng 3 (Giáp)
        else if (slot == 28) processScrapPurchase(player, data, "BS_A_COM", 5, 20, im.buildItem("bag_armor_common", 1));
        else if (slot == 29) processScrapPurchase(player, data, "BS_A_UNC", 3, 50, im.buildItem("bag_armor_uncommon", 1));
        else if (slot == 30) processScrapPurchase(player, data, "BS_A_RAR", 2, 120, im.buildItem("bag_armor_rare", 1));
        else if (slot == 31) processScrapPurchase(player, data, "BS_A_EPI", 1, 300, im.buildItem("bag_armor_epic", 1));

            // Xử lý Click Hàng 4 (Xu Vật Lý)
        else if (slot == 37) processMoneyPurchase(player, data, "BS_ANVIL", 9999, 500, new ItemStack(Material.ANVIL));
        else if (slot == 38) processMoneyPurchase(player, data, "BS_COAL", 9999, 1000, new ItemStack(Material.COAL, 64));
        else if (slot == 39) processMoneyPurchase(player, data, "BS_DIA_PICKAXE", 1, 5000, new ItemStack(Material.DIAMOND_PICKAXE));
    }

    private void processScrapPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String id, int maxStock, int price, ItemStack reward) {
        if (maxStock != 9999 && data.getPurchasedAmount(id) >= maxStock) {
            player.sendMessage("§cĐã đạt giới hạn mua trong tháng này!");
            return;
        }

        // [SỬA]: Sử dụng ItemManager để tìm và trừ Mạt Thợ Rèn VẬT LÝ trong túi đồ
        vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        org.bukkit.inventory.ItemStack scrapTemplate = im.buildItem("misc_blacksmith_scrap", 1);

        // Kiểm tra và trừ đồ
        if (scrapTemplate != null && im.takeItem(player, scrapTemplate, price)) {
            data.addPurchase(id, 1);
            player.getInventory().addItem(reward);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            openShop(player); // Tải lại UI
        } else {
            player.sendMessage("§cKhông đủ " + price + " Mạt Thợ Rèn trong túi đồ!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void processMoneyPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String id, int maxStock, long price, ItemStack reward) {
        if (maxStock != 9999 && data.getPurchasedAmount(id) >= maxStock) {
            player.sendMessage("§cĐã đạt giới hạn mua trong tháng này!");
            return;
        }

        // Gọi hệ thống Tiền tệ nội bộ của Server
        vn.minecraft.rPGCore.managers.CurrencyManager cm = new vn.minecraft.rPGCore.managers.CurrencyManager(vn.minecraft.rPGCore.RPGCore.getInstance());

        // Cố gắng trừ tiền vật lý trong túi đồ
        if (cm.takePhysicalCurrency(player, price)) {
            data.addPurchase(id, 1);
            player.getInventory().addItem(reward);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendMessage("§a[Shop] §fMua thành công vật phẩm với giá " + cm.formatCurrency(price) + "!");
            openShop(player); // Tải lại UI
        } else {
            player.sendMessage("§cKhông đủ " + cm.formatCurrency(price) + " trong túi đồ!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // =========================================
    // TRIỂN KHAI CÂY KỸ NĂNG (SKILL TREE)
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§dCÂY KỸ NĂNG THỢ RÈN"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, getSkillTreeGUITitle());

        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(4, createSkillIcon("BS_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("BLACKSMITH")));
        inv.setItem(11, createSkillIcon("BS_HAND_OF_GOD", Material.ENCHANTED_GOLDEN_APPLE, "§e§lBÀN TAY CỦA CHÚA", 2, data, "§fTăng thêm §a+5% §ftỉ lệ thành công", "§fkhi rèn bất kỳ trang bị nào."));
        inv.setItem(15, createSkillIcon("BS_MASTER_SAVER", Material.GOLD_INGOT, "§6§lBẬC THẦY TIẾT KIỆM", 1, data, "§fGiảm §e20% §fchi phí Mạt Thợ Rèn", "§fkhi sử dụng nút Rèn Bậc Thầy."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
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

        if (skillId.equals("BS_INFO")) {
            // Không làm gì thêm cho nút thông tin
        } else if (data.hasUnlockedSkill("BLACKSMITH", skillId)) {
            if (data.getLevel("BLACKSMITH") >= 50) {
                lore.add("§a§l✔ ĐÃ KÍCH HOẠT VÀ ĐANG HOẠT ĐỘNG");
            } else {
                lore.add("§c§l✖ BỊ KHÓA TẠM THỜI");
                lore.add("§c(Yêu cầu đạt lại Cấp 50 để kích hoạt)");
            }
        } else {
            lore.add("§e§lCLICK ĐỂ HỌC KỸ NĂNG NÀY!");
            lore.add("§fTiêu hao: §d" + jpCost + " JP");
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

        if (slot == 11) processSkillPurchase(player, data, "BS_HAND_OF_GOD", 2);
        else if (slot == 15) processSkillPurchase(player, data, "BS_MASTER_SAVER", 1);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("BLACKSMITH", skillId)) {
            player.sendMessage("§cBạn đã học kỹ năng này rồi!");
            return;
        }
        if (data.getJobPoints("BLACKSMITH") >= cost) {
            data.addJobPoints("BLACKSMITH", -cost);
            data.unlockSkill("BLACKSMITH", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendMessage("§d✨ Đã mở khóa thành công Kỹ Năng Tối Thượng!");
            openSkillTree(player); // Tải lại GUI
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
}