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

public class MerchantJob extends RPGJob {

    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public MerchantJob() {
        super("MERCHANT", "Thương Nhân");
        registerQuests();
    }

    private void registerQuests() {
        // DAILY QUESTS
        questPool.put("DAILY_MERCH_1", new AdvancedQuest("DAILY_MERCH_1", "Đôi Chân Không Mỏi", "Di chuyển 1000 block (Cưỡi thú hoặc đi bộ).", 1000, QuestAction.MERCHANT_MOVE, null));
        questPool.put("DAILY_MERCH_2", new AdvancedQuest("DAILY_MERCH_2", "Thương Vụ Nhỏ", "Bán thành công 30 vật phẩm tại NPC.", 30, QuestAction.MERCHANT_SELL, null));
        questPool.put("DAILY_MERCH_3", new AdvancedQuest("DAILY_MERCH_3", "Nhà Đầu Tư", "Mua thành công 10 vật phẩm tại NPC.", 10, QuestAction.MERCHANT_BUY, null));
        questPool.put("DAILY_MERCH_4", new AdvancedQuest("DAILY_MERCH_4", "Đầu Tư Tiêu Sản", "Tiêu hao tổng cộng 4096 Xu Đồng (1 Vàng).", 4096, QuestAction.MERCHANT_SPEND, null));
        questPool.put("DAILY_MERCH_5", new AdvancedQuest("DAILY_MERCH_5", "Dòng Tiền Mới", "Kiếm được 2048 Xu Đồng từ hệ thống.", 2048, QuestAction.MERCHANT_EARN, null));

        // WEEKLY QUESTS
        questPool.put("WEEKLY_MERCH_1", new AdvancedQuest("WEEKLY_MERCH_1", "Lưu Thông Hàng Hóa", "Di chuyển tổng cộng 4000 block.", 4000, QuestAction.MERCHANT_MOVE, null));
        questPool.put("WEEKLY_MERCH_2", new AdvancedQuest("WEEKLY_MERCH_2", "Đại Gia Vung Tiền", "Tiêu hao tổng cộng 40,960 Xu Đồng (10 Vàng).", 40960, QuestAction.MERCHANT_SPEND, null));
        questPool.put("WEEKLY_MERCH_3", new AdvancedQuest("WEEKLY_MERCH_3", "Lãi Mẹ Đẻ Lãi Con", "Nhận lãi suất Ngân hàng 5 lần.", 5, QuestAction.MERCHANT_INTEREST, null));
        questPool.put("WEEKLY_MERCH_4", new AdvancedQuest("WEEKLY_MERCH_4", "Bán Buôn Sỉ", "Bán thành công 500 vật phẩm tại NPC.", 500, QuestAction.MERCHANT_SELL, null));
        questPool.put("WEEKLY_MERCH_5", new AdvancedQuest("WEEKLY_MERCH_5", "Đổ Buôn Hàng Hóa", "Mua thành công 200 vật phẩm tại NPC.", 200, QuestAction.MERCHANT_BUY, null));
        questPool.put("WEEKLY_MERCH_6", new AdvancedQuest("WEEKLY_MERCH_6", "Chuyến Hàng Vận Tải", "Hoàn thành 1 chuyến giao thương (Yêu cầu có thú cưỡi).", 1, QuestAction.MERCHANT_DELIVER, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() { return questPool; }

    @Override
    public ItemStack getIcon(org.bukkit.entity.Player player) {
        ItemStack icon = new ItemStack(Material.EMERALD);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lNGHỀ THƯƠNG NHÂN");
            meta.setLore(getDescription(player));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int level = data.getLevel("MERCHANT");

        // Tính toán chỉ số theo Level
        double bargainDiscount = Math.min(level * 0.5, 25.0);
        double mountSpeed = Math.min(level * 0.6, 30.0);
        double bankInterest = Math.min(0.1 + ((level - 1) * (0.4 / 49.0)), 0.5);
        double copperDrop = Math.min(5.0 + (level * 0.5), 30.0);

        boolean hasUlt2 = data.hasUnlockedSkill("MERCHANT", "MERCH_ULT_2");
        if (hasUlt2) bargainDiscount = Math.min(bargainDiscount + 15.0, 40.0); // Cộng thêm 15% từ Ultimate

        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc thầy kinh tế, người nắm giữ mạch máu giao thương.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + level + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI CƠ BẢN:");
        lore.add("§8▪ §fNghệ Thuật Đàm Phán: §aGiảm " + String.format("%.1f", bargainDiscount) + "% §fgiá mua và tăng giá bán.");
        lore.add("§8▪ §fThuần Thục Thồ Hàng: §b+" + String.format("%.1f", mountSpeed) + "% §fTốc độ Thú cưỡi.");
        lore.add("§8▪ §fLãi Suất Tín Dụng: Nhận §e" + String.format("%.2f", bankInterest) + "% §fTiền lãi mỗi ngày.");
        lore.add("§8▪ §fMùi Tiền: §6" + String.format("%.1f", copperDrop) + "% §fTỉ lệ rớt tiền khi giết quái.");

        boolean hasUlt1 = data.hasUnlockedSkill("MERCHANT", "MERCH_ULT_1");
        boolean hasUlt3 = data.hasUnlockedSkill("MERCHANT", "MERCH_ULT_3");

        if (hasUlt1 || hasUlt2 || hasUlt3) {
            lore.add("");
            lore.add("§d§lKỸ NĂNG TỐI THƯỢNG ĐÃ MỞ:");
            if (hasUlt1) lore.add("§8▪ §eĐoàn Thương Uyển: §fBuff 15% chỉ số Ngựa & +9 ô Túi đồ.");
            if (hasUlt2) lore.add("§8▪ §eGiảm Giá Kịch Sàn: §fTăng vĩnh viễn 15% Nội tại Đàm Phán.");
            if (hasUlt3) lore.add("§8▪ §eĐặc Quyền Bạch Kim: §f0.1% rớt Đồng Bạch Kim khi giết quái.");
        }
        return lore;
    }

    // =========================================
    // SHOP THƯƠNG NHÂN (6x9)
    // =========================================
    @Override
    public String getShopGUITitle() { return "§l§6CHỢ ĐEN THƯƠNG GIA"; }

    @Override
    public void openShop(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, getShopGUITitle());
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = data.getLevel("MERCHANT");

        ItemStack border = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta boMeta = border.getItemMeta(); boMeta.setDisplayName(" "); border.setItemMeta(boMeta);

        // Fill Kính viền
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, border);
        }

        // Hàng 2 (Slot 10-15): 6 Vật phẩm đặc quyền (Mua bằng Chứng Nhận Thương Nhân)
        inv.setItem(10, createTokenItem(im.buildItem("misc_job_change_card", 1), "MERCH_JOB_CHANGE", data, 1, 150, true, 1));
        inv.setItem(11, createTokenItem(im.buildItem("misc_gold_scale", 1), "MERCH_SCALE", data, 10, 20, jobLevel >= 5, 5));
        inv.setItem(12, createTokenItem(im.buildItem("bag_cloth", 1), "MERCH_BAG_1", data, 3, 30, jobLevel >= 10, 10));
        inv.setItem(13, createTokenItem(im.buildItem("bag_leather", 1), "MERCH_BAG_2", data, 2, 50, jobLevel >= 20, 20));
        inv.setItem(14, createTokenItem(im.buildItem("bag_dragon_scale", 1), "MERCH_BAG_3", data, 1, 100, jobLevel >= 35, 35));
        inv.setItem(15, createTokenItem(im.buildItem("misc_stable_expansion", 1), "MERCH_STABLE", data, 1, 100, jobLevel >= 15, 15));
        inv.setItem(16, createTokenItem(im.buildItem("misc_mount_upgrade", 1), "MERCH_UPGRADE", data, 20, 30, jobLevel >= 1, 1));

        inv.setItem(19, createTokenItem(im.buildItem("misc_mount_retrieval", 1), "MERCH_MOUNT_RETRIEVE", data, 1, 30, jobLevel >= 25, 25));
        inv.setItem(20, createTokenItem(im.buildItem("misc_mount_upgrade_speed", 1), "MERCH_UPG_SPEED", data, 3, 20, jobLevel >= 20, 20));
        inv.setItem(21, createTokenItem(im.buildItem("misc_mount_upgrade_jump", 1), "MERCH_UPG_JUMP", data, 3, 20, jobLevel >= 20, 20));
        inv.setItem(22, createTokenItem(im.buildItem("misc_mount_upgrade_durability", 1), "MERCH_UPG_DURA", data, 3, 20, jobLevel >= 20, 20));
        inv.setItem(23, createTokenItem(im.buildItem("misc_mount_upgrade_cooldown", 1), "MERCH_UPG_CD", data, 3, 20, jobLevel >= 20, 20));
        inv.setItem(24, createTokenItem(im.buildItem("misc_mount_upgrade_storage", 1), "MERCH_UPG_STORAGE", data, 3, 20, jobLevel >= 20, 20));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(53, back);

        player.openInventory(inv);
    }

    private ItemStack createTokenItem(ItemStack baseItem, String purchaseId, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, int maxStock, int tokenCost, boolean isUnlocked, int reqLevel) {
        ItemStack item = baseItem.clone(); ItemMeta meta = item.getItemMeta();
        int currentBought = data.getPurchasedAmount(purchaseId);
        if (!isUnlocked) {
            meta.setDisplayName("§c§l🔒 CHƯA MỞ KHÓA"); meta.setLore(Arrays.asList("§7Yêu cầu Cấp độ Thương Nhân: §b" + reqLevel)); item.setType(Material.BARRIER);
        } else if (currentBought >= maxStock) {
            meta.setDisplayName("§8§m" + org.bukkit.ChatColor.stripColor(meta.getDisplayName())); meta.setLore(Arrays.asList("§c§lĐÃ HẾT HÀNG THÁNG NÀY"));
        } else {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(""); lore.add("§8§m------------------------"); lore.add("§fGiá: §e" + tokenCost + " Chứng Nhận Thương Nhân"); lore.add("§fGiới hạn mua: §a" + currentBought + "§8/§a" + maxStock + " §7(Tháng này)");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_token_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, tokenCost);
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
        cleanReward.setAmount(1);

        ItemMeta cleanMeta = cleanReward.getItemMeta();
        List<String> cleanLore = cleanMeta.getLore();
        if (cleanLore != null) { cleanLore.removeIf(l -> l.contains("Giá:") || l.contains("Giới hạn mua:") || l.contains("---")); cleanMeta.setLore(cleanLore); }
        cleanReward.setItemMeta(cleanMeta);

        if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_token_cost"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int tokenCost = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "buy_token_cost"), org.bukkit.persistence.PersistentDataType.INTEGER);
            ItemStack tokenTemplate = im.buildItem("misc_merchant_token", 1);

            if (im.takeItem(player, tokenTemplate, tokenCost)) {
                data.addPurchase(buyId, 1); player.getInventory().addItem(cleanReward);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                openShop(player);
            } else {
                player.sendMessage("§cKhông đủ " + tokenCost + " Chứng Nhận Thương Nhân!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    // =========================================
    // CÂY KỸ NĂNG THƯƠNG NHÂN
    // =========================================
    @Override
    public String getSkillTreeGUITitle() { return "§l§6CÂY KỸ NĂNG THƯƠNG NHÂN"; }

    @Override
    public void openSkillTree(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (data.getLevel("MERCHANT") < 50) { player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!"); player.closeInventory(); return; }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, getSkillTreeGUITitle());
        ItemStack glass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        inv.setItem(13, createSkillIcon("MERCH_INFO", Material.BOOK, "§d§lTHÔNG TIN ĐIỂM JP", 0, data, "§fSố JP hiện có: §d" + data.getJobPoints("MERCHANT")));
        inv.setItem(29, createSkillIcon("MERCH_ULT_1", Material.CHEST_MINECART, "§e§lĐOÀN THƯƠNG UYỂN", 3, data, "§fBuff 15% mọi chỉ số cho Thú Cưỡi.", "§fMở khóa thêm 9 ô túi đồ thú cưỡi."));
        inv.setItem(31, createSkillIcon("MERCH_ULT_2", Material.GOLD_INGOT, "§c§lGIẢM GIÁ KỊCH SÀN", 3, data, "§fTăng vĩnh viễn thêm 15%", "§fvào Nội tại Nghệ Thuật Đàm Phán."));
        inv.setItem(33, createSkillIcon("MERCH_ULT_3", Material.DIAMOND, "§b§lĐẶC QUYỀN BẠCH KIM", 3, data, "§fMọi đòn kết liễu quái vật có", "§f0.1% rơi ra 1 Đồng Bạch Kim."));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(44, back);
        player.openInventory(inv);
    }

    private ItemStack createSkillIcon(String skillId, Material mat, String name, int jpCost, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String... desc) {
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(); lore.add("§7Nội tại Kỹ năng Tối thượng"); lore.add("");
        for (String line : desc) lore.add("§8▪ " + line); lore.add("");
        if (!skillId.equals("MERCH_INFO")) {
            if (data.hasUnlockedSkill("MERCHANT", skillId)) lore.add("§a§l✔ ĐÃ KÍCH HOẠT");
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
        if (slot == 29) processSkillPurchase(player, data, "MERCH_ULT_1", 3);
        else if (slot == 31) processSkillPurchase(player, data, "MERCH_ULT_2", 3);
        else if (slot == 33) processSkillPurchase(player, data, "MERCH_ULT_3", 3);
    }

    private void processSkillPurchase(org.bukkit.entity.Player player, vn.minecraft.rPGCore.jobs.core.PlayerJobData data, String skillId, int cost) {
        if (data.hasUnlockedSkill("MERCHANT", skillId)) return;
        if (data.getJobPoints("MERCHANT") >= cost) {
            data.addJobPoints("MERCHANT", -cost); data.unlockSkill("MERCHANT", skillId);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); openSkillTree(player);
        } else {
            player.sendMessage("§cBạn không có đủ " + cost + " JP để học kỹ năng này!"); player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // ==========================================
    // CÁC HÀM TRỢ GIÚP LOGIC CHO NGHỀ THƯƠNG NHÂN
    // ==========================================
    public static double getBargainDiscount(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("MERCHANT")) return 0.0;
        int level = data.getLevel("MERCHANT");
        if (level <= 0) return 0.0;

        double baseDiscount = level * 0.005; // 0.5% mỗi cấp
        if (hasUltimate(player, "MERCH_ULT_2")) baseDiscount += 0.15;
        return Math.min(baseDiscount, 0.40); // Max 40%
    }

    public static double getMountSpeedBuff(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("MERCHANT")) return 0.0;
        return Math.min(data.getLevel("MERCHANT") * 0.006, 0.30);
    }

    public static double getBankInterestRate(org.bukkit.entity.Player player) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        if (!data.getJobId().equals("MERCHANT")) return 0.0;
        int level = data.getLevel("MERCHANT");
        if (level <= 0) return 0.0;
        return Math.min(0.001 + ((level - 1) * (0.004 / 49.0)), 0.005);
    }

    public static double getCopperDropChance(int level) { return Math.min(5.0 + (level * 0.5), 30.0); }
    public static double getSilverDropChance(int level) { return Math.min(0.5 + (level * 0.09), 5.0); }

    public static boolean hasUltimate(org.bukkit.entity.Player player, String ultId) {
        return vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player).hasUnlockedSkill("MERCHANT", ultId);
    }

    public static boolean triggerPlatinumTouch(org.bukkit.entity.Player player) {
        if (!hasUltimate(player, "MERCH_ULT_3")) return false;
        return (Math.random() * 100.0) <= 0.1;
    }

    @Override
    public List<String> getDailyRewardLore(int milestone) {
        if (milestone == 1) return java.util.Arrays.asList("§e+1 Chứng Nhận Thương Nhân");
        if (milestone == 2) return java.util.Arrays.asList("§e+1 Chứng Nhận Thương Nhân");
        return java.util.Arrays.asList("§e+2 Chứng Nhận Thương Nhân", "§6+10 Xu Vàng");
    }

    @Override
    public List<String> getWeeklyRewardLore() {
        return Arrays.asList("§e+10 Chứng Nhận Thương Nhân", "§6+50 Xu Vàng");
    }

    @Override
    public void giveDailyMilestoneReward(org.bukkit.entity.Player player, int milestone) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        if (milestone == 1 || milestone == 2) {
            player.getInventory().addItem(im.buildItem("misc_merchant_token", 1));
        } else if (milestone == 3) {
            player.getInventory().addItem(im.buildItem("misc_merchant_token", 2));
            vn.minecraft.rPGCore.RPGCore.getInstance().getCurrencyManager().givePhysicalCurrency(player, 40960); // 10 Vàng = 40960 Đồng
        }
    }

    @Override
    public void giveWeeklyMilestoneReward(org.bukkit.entity.Player player) {
        ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        player.getInventory().addItem(im.buildItem("misc_merchant_token", 10));
        vn.minecraft.rPGCore.RPGCore.getInstance().getCurrencyManager().givePhysicalCurrency(player, 204800); // 50 Vàng = 204800 Đồng
    }
}