package vn.minecraft.rPGCore.jobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.jobs.core.RPGJob;
import vn.minecraft.rPGCore.jobs.quests.RPGQuest;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.List;

public class QuestBoardGUI implements Listener {

    public static final String GUI_NAME = "§l§8BẢNG NHIỆM VỤ";

    public static void openGUI(Player player) {
        // [ĐÃ SỬA]: Chỉnh GUI xuống còn 5 hàng (45 ô)
        Inventory inv = Bukkit.createInventory(null, 45, GUI_NAME);
        PlayerJobData data = JobManager.getPlayerData(player);
        RPGJob currentJob = JobManager.getPlayerJob(player);

        if (currentJob == null) return;
        java.util.Map<String, RPGQuest> allQuests = currentJob.getQuests();

        // Nền viền GUI
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        // ==========================================
        // 1. RENDER NHIỆM VỤ NGÀY (Cột 1)
        // ==========================================
        List<String> dailyQuests = data.getActiveDailyQuests();
        int[] dailySlots = {10, 19, 28}; // [ĐÃ SỬA] Layout dọc theo hình
        for (int i = 0; i < 3; i++) {
            if (dailyQuests != null && i < dailyQuests.size()) {
                RPGQuest quest = allQuests.get(dailyQuests.get(i));
                if (quest != null) inv.setItem(dailySlots[i], createQuestIcon(quest, data, false));
            }
        }

        // ==========================================
        // 2. RENDER RƯƠNG QUÀ MỐC NGÀY (Cột 3)
        // ==========================================
        int milestone = data.getDailyMilestone();
        inv.setItem(12, createChestIcon("MỐC 1", 1, milestone, data.isQuestClaimed("DAILY_MILESTONE_1"), "§a+100 EXP Nghề", "§6+1 Mạt Thợ Rèn")); // [ĐÃ SỬA] Slot 12
        inv.setItem(21, createChestIcon("MỐC 2", 2, milestone, data.isQuestClaimed("DAILY_MILESTONE_2"), "§a+75 EXP Nghề")); // [ĐÃ SỬA] Slot 21
        inv.setItem(30, createChestIcon("MỐC 3", 3, milestone, data.isQuestClaimed("DAILY_MILESTONE_3"), "§a+125 EXP Nghề", "§6+2 Mạt Thợ Rèn", "§b+10 Đá Nâng Cấp")); // [ĐÃ SỬA] Slot 30

        // ==========================================
        // 3. RENDER NHIỆM VỤ TUẦN (Cột 5)
        // ==========================================
        List<String> weeklyQuests = data.getActiveWeeklyQuests();
        int[] weeklySlots = {14, 23, 32}; // [ĐÃ SỬA] Layout dọc theo hình
        boolean allWeeklyDone = true;
        for (int i = 0; i < 3; i++) {
            if (weeklyQuests != null && i < weeklyQuests.size()) {
                RPGQuest quest = allQuests.get(weeklyQuests.get(i));
                if (quest != null) {
                    inv.setItem(weeklySlots[i], createQuestIcon(quest, data, true));
                    if (data.getQuestProgress(quest.getId()) < quest.getGoal()) allWeeklyDone = false;
                } else allWeeklyDone = false;
            } else allWeeklyDone = false;
        }

        // ==========================================
        // 4. RENDER RƯƠNG TUẦN SIÊU CẤP (Cột 7)
        // ==========================================
        inv.setItem(25, createWeeklyChest(allWeeklyDone, data.isQuestClaimed("WEEKLY_MILESTONE_FINAL"))); // [ĐÃ SỬA] Slot 25 (Giữa)

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
    }

    private static ItemStack createQuestIcon(RPGQuest quest, PlayerJobData data, boolean isWeekly) {
        // [ĐÃ FIX LỖI "ORIGINAL"]: Đổi WRITTEN_BOOK thành BOOK thông thường
        ItemStack item = new ItemStack(isWeekly ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        // Ẩn các cờ rác mặc định của Sách
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        int progress = data.getQuestProgress(quest.getId());
        int goal = quest.getGoal();

        meta.setDisplayName((isWeekly ? "§d§l[TUẦN] " : "§6§l[NGÀY] ") + quest.getName());
        List<String> lore = new ArrayList<>();
        lore.add("§7" + quest.getDescription());
        lore.add(" ");
        if (progress >= goal) {
            lore.add("§a§l✔ ĐÃ HOÀN THÀNH");
        } else {
            lore.add("§fTiến độ: §e" + progress + "§8/§e" + goal);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createChestIcon(String name, int reqMilestone, int currentMilestone, boolean claimed, String... rewards) {
        Material mat = claimed ? Material.MINECART : (currentMilestone >= reqMilestone ? Material.CHEST_MINECART : Material.HOPPER_MINECART);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lTHƯỞNG " + name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Yêu cầu: " + reqMilestone + " Nhiệm vụ");
        lore.add(" ");
        for (String r : rewards) lore.add("§8▪ " + r);
        lore.add(" ");
        if (claimed) lore.add("§aĐã nhận thưởng!");
        else if (currentMilestone >= reqMilestone) lore.add("§e§lCLICK ĐỂ NHẬN!");
        else lore.add("§cChưa đủ điều kiện.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createWeeklyChest(boolean allDone, boolean claimed) {
        Material mat = claimed ? Material.MINECART : (allDone ? Material.ENDER_CHEST : Material.HOPPER_MINECART);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lRƯƠNG THỬ THÁCH TUẦN");
        List<String> lore = new ArrayList<>();
        lore.add("§7Hoàn thành cả 3 nhiệm vụ Tuần để mở khóa.");
        lore.add(" ");
        lore.add("§8▪ §a+2000 EXP Nghề");
        lore.add("§8▪ §6+20 Mạt Thợ Rèn");
        lore.add("§8▪ §b+50 Đá Nâng Cấp");
        lore.add(" ");
        if (claimed) lore.add("§aĐã nhận thưởng!");
        else if (allDone) lore.add("§e§lCLICK ĐỂ NHẬN!");
        else lore.add("§cChưa hoàn thành đủ 3 nhiệm vụ.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        PlayerJobData data = JobManager.getPlayerData(player);
        ItemManager im = RPGCore.getInstance().getItemManager();

        // Nhận Mốc 1 (Cập nhật đúng Slot 12)
        if (slot == 12 && data.getDailyMilestone() >= 1 && !data.isQuestClaimed("DAILY_MILESTONE_1")) {
            data.setQuestClaimed("DAILY_MILESTONE_1", true);
            JobManager.addExp(player, 100);
            player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 1));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            openGUI(player);
        }
        // Nhận Mốc 2 (Cập nhật đúng Slot 21)
        else if (slot == 21 && data.getDailyMilestone() >= 2 && !data.isQuestClaimed("DAILY_MILESTONE_2")) {
            data.setQuestClaimed("DAILY_MILESTONE_2", true);
            JobManager.addExp(player, 75);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            openGUI(player);
        }
        // Nhận Mốc 3 (Cập nhật đúng Slot 30)
        else if (slot == 30 && data.getDailyMilestone() >= 3 && !data.isQuestClaimed("DAILY_MILESTONE_3")) {
            data.setQuestClaimed("DAILY_MILESTONE_3", true);
            JobManager.addExp(player, 125);
            player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 2));
            player.getInventory().addItem(vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 10)); // Đá nâng cấp
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            openGUI(player);
        }
        // Nhận Rương Tuần (Cập nhật đúng Slot 25)
        else if (slot == 25 && !data.isQuestClaimed("WEEKLY_MILESTONE_FINAL")) {
            // Kiểm tra tiến độ 3 quest tuần
            boolean allDone = true;
            for (String qId : data.getActiveWeeklyQuests()) {
                RPGQuest q = JobManager.getPlayerJob(player).getQuests().get(qId);
                if (data.getQuestProgress(qId) < q.getGoal()) allDone = false;
            }
            if (allDone) {
                data.setQuestClaimed("WEEKLY_MILESTONE_FINAL", true);
                JobManager.addExp(player, 2000);
                player.getInventory().addItem(im.buildItem("misc_blacksmith_scrap", 20));
                player.getInventory().addItem(vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 50));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                openGUI(player);
            }
        }
    }
}