package vn.minecraft.rPGCore.jobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.jobs.core.RPGJob;

import java.util.ArrayList;
import java.util.List;

public class JobSelectionGUI implements Listener {

    public static final String GUI_NAME = "§l§8TRUNG TÂM NGHỀ NGHIỆP";

    // Hàm gọi mở GUI (Sẽ được gọi khi gõ lệnh /job)
    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        // Trang trí viền kính
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }

        PlayerJobData data = JobManager.getPlayerData(player);
        int slot = 10;

        // Render các nghề đang có
        for (RPGJob job : JobManager.getAllJobs()) {
            if (slot > 16) break; // Tránh tràn GUI

            int jobLevel = data.getLevel(job.getId());
            ItemStack icon = job.getIcon(jobLevel).clone();
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();

            lore.add(" ");
            lore.add("§8§m------------------------");

            if (data.getJobId().equalsIgnoreCase(job.getId())) {
                lore.add("§a§l[ĐANG CHỌN]");
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add("§eNhấp để chọn/đổi sang nghề này!");
                if (!data.getJobId().equals("NONE")) {
                    lore.add("§cChú ý: Đổi nghề sẽ làm nghề hiện tại");
                    lore.add("§cbị giảm 5 cấp (Trừ khi có Thẻ Đổi Nghề)");
                } else {
                    lore.add("§a(Miễn phí lần đầu chọn nghề)");
                }
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // [MỚI]: Xử lý chặn click và mở Bảng nhiệm vụ cho GUI "HỒ SƠ NGHỀ NGHIỆP"
        if (event.getView().getTitle().equals("§l§8HỒ SƠ NGHỀ NGHIỆP")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 11) { // Bấm vào nút Bảng Nhiệm Vụ
                vn.minecraft.rPGCore.jobs.gui.QuestBoardGUI.openGUI((Player) event.getWhoClicked());
            }
            return;
        }
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= 27) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Tìm xem người chơi click vào nghề nào dựa trên Tên hiển thị
        RPGJob targetJob = null;
        for (RPGJob job : JobManager.getAllJobs()) {
            if (clicked.getItemMeta().getDisplayName().equals(job.getIcon(1).getItemMeta().getDisplayName())) {
                targetJob = job;
                break;
            }
        }

        if (targetJob == null) return;

        PlayerJobData data = JobManager.getPlayerData(player);
        String oldJobId = data.getJobId();
        String newJobId = targetJob.getId();

        // 1. Nếu click vào nghề đang làm
        if (oldJobId.equalsIgnoreCase(newJobId)) {
            player.sendMessage("§cBạn đang làm nghề này rồi!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 2. Nếu đã có nghề và muốn ĐỔI NGHỀ (Check Thẻ)
        if (!oldJobId.equals("NONE")) {
            boolean hasCard = removeJobChangeCard(player);

            if (hasCard) {
                player.sendMessage("§a✨ Đã sử dụng 1x Thẻ Đổi Nghề! Nghề §e" + JobManager.getPlayerJob(player).getName() + " §ađược giữ nguyên cấp độ.");
            } else {
                int oldLevel = data.getLevel(oldJobId);
                int penalizedLevel = Math.max(1, oldLevel - 5); // Phạt 3 cấp, tối thiểu là cấp 1
                data.setLevel(oldJobId, penalizedLevel);
                player.sendMessage("§cBạn đã đổi nghề mà không có Thẻ! Nghề cũ bị giảm xuống cấp §e" + penalizedLevel + "§c.");
            }
        } else {
            // 3. Lần đầu chọn nghề
            player.sendMessage("§aChúc mừng bạn đã chính thức gia nhập nghề §e" + targetJob.getName() + "§a!");
        }

        // Cập nhật nghề mới và đóng GUI
        data.setJobId(newJobId);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.closeInventory();
    }

    // Hàm tiện ích: Tìm và trừ 1 Thẻ Đổi Nghề (Đã tối ưu 1 dòng)
    private boolean removeJobChangeCard(Player player) {
        // Lấy ItemManager từ hệ thống
        vn.minecraft.rPGCore.managers.ItemManager itemManager = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
        if (itemManager == null) return false;

        // Tạo template thẻ đổi nghề từ ID
        org.bukkit.inventory.ItemStack cardTemplate = itemManager.buildItem("misc_job_change_card", 1);
        if (cardTemplate == null) return false;

        // Uỷ quyền toàn bộ việc đếm, check ID và trừ đồ cho ItemManager
        return itemManager.takeItem(player, cardTemplate, 1);
    }

    // ===============================================
    // [MỚI]: GIAO DIỆN HỒ SƠ NGHỀ NGHIỆP & NHIỆM VỤ
    // ===============================================
    public static void openStatusMenu(Player player) {
        PlayerJobData data = JobManager.getPlayerData(player);
        RPGJob currentJob = JobManager.getPlayerJob(player);

        if (currentJob == null) {
            player.sendMessage("§cBạn chưa có nghề! Hãy click vào một nghề để học.");
            openGUI(player); // Mở lại menu chọn nghề
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§l§8HỒ SƠ NGHỀ NGHIỆP");
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // 1. Ô TRUNG TÂM (SLOT 13) - ICON NGHỀ & THANH KINH NGHIỆM
        ItemStack icon = currentJob.getIcon(data.getLevel()).clone();
        ItemMeta iconMeta = icon.getItemMeta();
        List<String> iconLore = iconMeta.hasLore() ? iconMeta.getLore() : new ArrayList<>();

        double currentExp = data.getExp();
        double requiredExp = JobManager.getRequiredJobExp(data.getLevel());

        iconLore.add(" ");
        iconLore.add("§fTiến độ thăng cấp: §e" + (int)currentExp + "§8/§e" + (int)requiredExp + " EXP");
        iconLore.add("   " + getProgressBar(currentExp, requiredExp, 20));
        iconLore.add(" ");
        iconLore.add("§fMạt Thợ Rèn đang có: §6" + data.getBlacksmithScraps());
        iconMeta.setLore(iconLore);
        icon.setItemMeta(iconMeta);
        inv.setItem(13, icon);

        // ==========================================
        // 2. NÚT MỞ BẢNG NHIỆM VỤ (SLOT 11)
        // ==========================================
        ItemStack questBoard = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta qbMeta = questBoard.getItemMeta();
        qbMeta.setDisplayName("§e§lBẢNG NHIỆM VỤ (COMMISSIONS)");
        qbMeta.setLore(java.util.Arrays.asList(
                "§7Nhấn vào để mở Bảng nhiệm vụ.",
                "§7Hoàn thành các mốc để nhận phần thưởng lớn!"
        ));
        questBoard.setItemMeta(qbMeta);
        inv.setItem(11, questBoard);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    private static ItemStack createQuestIcon(vn.minecraft.rPGCore.jobs.quests.RPGQuest quest, PlayerJobData data, boolean isWeekly) {
        ItemStack item = new ItemStack(isWeekly ? Material.ENCHANTED_BOOK : Material.WRITTEN_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isWeekly ? "§d§lNHIỆM VỤ TUẦN: " : "§6§lNHIỆM VỤ NGÀY: ") + quest.getName());

        int progress = data.getQuestProgress(quest.getId());
        int goal = quest.getGoal();
        boolean claimed = data.isQuestClaimed(quest.getId());

        List<String> lore = new ArrayList<>();
        lore.add("§7" + quest.getDescription());
        lore.add(" ");

        if (claimed) {
            lore.add("§a§lTrạng thái: ĐÃ HOÀN THÀNH");
            lore.add("§8Tiến độ: " + goal + "/" + goal);
        } else {
            lore.add("§e§lTrạng thái: ĐANG TIẾN HÀNH");
            lore.add("§fTiến độ: §e" + Math.min(progress, goal) + "§8/§e" + goal);
            lore.add("   " + getProgressBar(progress, goal, 15));
        }

        lore.add(" ");
        lore.add("§fPhần thưởng dự kiến:");
        lore.add("§8▪ §a+" + quest.getDynamicExpReward(data.getLevel()) + " EXP Nghề");
        lore.add("§8▪ §6+" + quest.getDynamicScrapReward(data.getLevel()) + " Mạt Thợ Rèn");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String getProgressBar(double current, double max, int barSize) {
        int completed = (int) ((current / max) * barSize);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < barSize; i++) {
            if (i < completed) bar.append("§a■");
            else bar.append("§7■");
        }
        bar.append("§8]");
        return bar.toString();
    }
}