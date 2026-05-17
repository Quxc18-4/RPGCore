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

import java.util.ArrayList;
import java.util.List;

public class QuestBoardGUI implements Listener {

    public static final String GUI_NAME = "§l§8BẢNG NHIỆM VỤ";

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, GUI_NAME);
        PlayerJobData data = JobManager.getPlayerData(player);
        RPGJob currentJob = JobManager.getPlayerJob(player);

        if (currentJob == null) return;
        java.util.Map<String, RPGQuest> allQuests = currentJob.getQuests();

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        // 1. NHIỆM VỤ NGÀY
        List<String> dailyQuests = data.getActiveDailyQuests();
        int[] dailySlots = {10, 19, 28};
        for (int i = 0; i < 3; i++) {
            if (dailyQuests != null && i < dailyQuests.size()) {
                RPGQuest quest = allQuests.get(dailyQuests.get(i));
                if (quest != null) inv.setItem(dailySlots[i], createQuestIcon(quest, data, false));
            }
        }

        // 2. RƯƠNG QUÀ MỐC NGÀY (CẬP NHẬT PHẦN THƯỞNG ĐỘNG)
        int milestone = data.getDailyMilestone();
        List<String> r1 = new ArrayList<>(); r1.add("§a+100 EXP Nghề"); r1.addAll(currentJob.getDailyRewardLore(1));
        List<String> r2 = new ArrayList<>(); r2.add("§a+75 EXP Nghề"); r2.addAll(currentJob.getDailyRewardLore(2));
        List<String> r3 = new ArrayList<>(); r3.add("§a+125 EXP Nghề"); r3.addAll(currentJob.getDailyRewardLore(3));

        inv.setItem(12, createChestIcon("MỐC 1", 1, milestone, data.isQuestClaimed("DAILY_MILESTONE_1"), r1.toArray(new String[0])));
        inv.setItem(21, createChestIcon("MỐC 2", 2, milestone, data.isQuestClaimed("DAILY_MILESTONE_2"), r2.toArray(new String[0])));
        inv.setItem(30, createChestIcon("MỐC 3", 3, milestone, data.isQuestClaimed("DAILY_MILESTONE_3"), r3.toArray(new String[0])));

        // 3. NHIỆM VỤ TUẦN
        List<String> weeklyQuests = data.getActiveWeeklyQuests();
        int[] weeklySlots = {14, 23, 32};
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

        // 4. RƯƠNG TUẦN SIÊU CẤP
        List<String> rWeek = new ArrayList<>(); rWeek.add("§a+2000 EXP Nghề"); rWeek.addAll(currentJob.getWeeklyRewardLore());
        inv.setItem(25, createWeeklyChest(allWeeklyDone, data.isQuestClaimed("WEEKLY_MILESTONE_FINAL"), rWeek.toArray(new String[0])));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI"); back.setItemMeta(bMeta);
        inv.setItem(44, back);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
    }

    private static ItemStack createQuestIcon(RPGQuest quest, PlayerJobData data, boolean isWeekly) {
        ItemStack item = new ItemStack(isWeekly ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        int progress = data.getQuestProgress(quest.getId());
        int goal = quest.getGoal();

        meta.setDisplayName((isWeekly ? "§d§l[TUẦN] " : "§6§l[NGÀY] ") + quest.getName());
        List<String> lore = new ArrayList<>();
        lore.add("§7" + quest.getDescription());
        lore.add(" ");
        if (progress >= goal) lore.add("§a§l✔ ĐÃ HOÀN THÀNH");
        else lore.add("§fTiến độ: §e" + progress + "§8/§e" + goal);

        meta.setLore(lore); item.setItemMeta(meta); return item;
    }

    private static ItemStack createChestIcon(String name, int reqMilestone, int currentMilestone, boolean claimed, String... rewards) {
        Material mat = claimed ? Material.MINECART : (currentMilestone >= reqMilestone ? Material.CHEST_MINECART : Material.HOPPER_MINECART);
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lTHƯỞNG " + name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Yêu cầu: " + reqMilestone + " Nhiệm vụ"); lore.add(" ");
        for (String r : rewards) lore.add("§8▪ " + r);
        lore.add(" ");
        if (claimed) lore.add("§aĐã nhận thưởng!");
        else if (currentMilestone >= reqMilestone) lore.add("§e§lCLICK ĐỂ NHẬN!");
        else lore.add("§cChưa đủ điều kiện.");
        meta.setLore(lore); item.setItemMeta(meta); return item;
    }

    private static ItemStack createWeeklyChest(boolean allDone, boolean claimed, String... rewards) {
        Material mat = claimed ? Material.MINECART : (allDone ? Material.ENDER_CHEST : Material.HOPPER_MINECART);
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lRƯƠNG THỬ THÁCH TUẦN");
        List<String> lore = new ArrayList<>();
        lore.add("§7Hoàn thành cả 3 nhiệm vụ Tuần để mở khóa."); lore.add(" ");
        for (String r : rewards) lore.add("§8▪ " + r);
        lore.add(" ");
        if (claimed) lore.add("§aĐã nhận thưởng!");
        else if (allDone) lore.add("§e§lCLICK ĐỂ NHẬN!");
        else lore.add("§cChưa hoàn thành đủ 3 nhiệm vụ.");
        meta.setLore(lore); item.setItemMeta(meta); return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        PlayerJobData data = JobManager.getPlayerData(player);

        // [FIX LỖI HERE]: KHAI BÁO BIẾN CURRENT JOB
        RPGJob currentJob = JobManager.getPlayerJob(player);
        if (currentJob == null) return;

        if (slot == 44) {
            vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI.openStatusMenu(player);
            return;
        }

        if (slot == 12 && data.getDailyMilestone() >= 1 && !data.isQuestClaimed("DAILY_MILESTONE_1")) {
            data.setQuestClaimed("DAILY_MILESTONE_1", true);
            JobManager.addExp(player, 100);
            currentJob.giveDailyMilestoneReward(player, 1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            openGUI(player);
        }
        else if (slot == 21 && data.getDailyMilestone() >= 2 && !data.isQuestClaimed("DAILY_MILESTONE_2")) {
            data.setQuestClaimed("DAILY_MILESTONE_2", true);
            JobManager.addExp(player, 75);
            currentJob.giveDailyMilestoneReward(player, 2);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            openGUI(player);
        }
        else if (slot == 30 && data.getDailyMilestone() >= 3 && !data.isQuestClaimed("DAILY_MILESTONE_3")) {
            data.setQuestClaimed("DAILY_MILESTONE_3", true);
            JobManager.addExp(player, 125);
            currentJob.giveDailyMilestoneReward(player, 3);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            openGUI(player);
        }
        else if (slot == 25 && !data.isQuestClaimed("WEEKLY_MILESTONE_FINAL")) {
            boolean allDone = true;
            for (String qId : data.getActiveWeeklyQuests()) {
                RPGQuest q = currentJob.getQuests().get(qId);
                if (data.getQuestProgress(qId) < q.getGoal()) allDone = false;
            }
            if (allDone) {
                data.setQuestClaimed("WEEKLY_MILESTONE_FINAL", true);
                JobManager.addExp(player, 2000);
                currentJob.giveWeeklyMilestoneReward(player);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                openGUI(player);
            }
        }
    }
}