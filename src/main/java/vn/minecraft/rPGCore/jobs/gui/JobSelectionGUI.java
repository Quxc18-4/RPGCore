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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.jobs.core.RPGJob;

import java.util.ArrayList;
import java.util.Arrays;
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
            ItemStack icon = job.getIcon(player).clone();
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();

            lore.add(" ");
            lore.add("§8§m------------------------");

            if (data.getJobId().equalsIgnoreCase(job.getId())) {
                lore.add("§a§l[ĐANG CHỌN]");
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
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
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // =========================================================
        // 1. XỬ LÝ GUI: "HỒ SƠ NGHỀ NGHIỆP" (Menu Quản Lý Cá Nhân)
        // =========================================================
        if (event.getView().getTitle().equals("§l§8HỒ SƠ NGHỀ NGHIỆP")) {
            event.setCancelled(true);

            if (slot == 10) {
                // [ĐÃ SỬA]: Mở Bảng xếp hạng thay vì báo đang bảo trì
                vn.minecraft.rPGCore.jobs.gui.JobLeaderboardGUI.openGUI(player);
            }
            else if (slot == 11) {
                vn.minecraft.rPGCore.jobs.gui.QuestBoardGUI.openGUI(player);
            }
            else if (slot == 15) {
                // Thay vì JobShopGUI.openGUI(player) -> Gọi lệnh của Job
                JobManager.getPlayerJob(player).openShop(player);
            }
            else if (slot == 16) {
                // Thay vì JobSkillTreeGUI.openGUI(player) -> Gọi lệnh của Job
                JobManager.getPlayerJob(player).openSkillTree(player);
            }

            return; // Rất quan trọng: Xử lý xong menu này thì DỪNG LẠI, không chạy xuống dưới
        }

        // =========================================================
        // 2. XỬ LÝ GUI: "TRUNG TÂM NGHỀ NGHIỆP" (Chọn / Đổi Nghề)
        // =========================================================
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        if (slot < 0 || slot >= 27) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Tìm xem người chơi click vào nghề nào dựa trên Tên hiển thị
        RPGJob targetJob = null;
        for (RPGJob job : JobManager.getAllJobs()) {
            if (clicked.getItemMeta().getDisplayName().equals(job.getIcon(player).getItemMeta().getDisplayName())) {
                targetJob = job;
                break;
            }
        }

        if (targetJob == null) return;

        PlayerJobData data = JobManager.getPlayerData(player);
        String oldJobId = data.getJobId();
        String newJobId = targetJob.getId();

        // [A]. Nếu click vào nghề đang làm
        if (oldJobId.equalsIgnoreCase(newJobId)) {
            player.sendMessage("§cBạn đang làm nghề này rồi!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // [B]. Nếu đã có nghề và muốn ĐỔI NGHỀ (Check Thẻ)
        if (!oldJobId.equals("NONE")) {
            boolean hasCard = removeJobChangeCard(player);

            if (hasCard) {
                player.sendMessage("§a✨ Đã sử dụng 1x Thẻ Đổi Nghề! Nghề §e" + JobManager.getPlayerJob(player).getName() + " §ađược giữ nguyên cấp độ.");
            } else {
                int oldLevel = data.getLevel(oldJobId);
                int penalizedLevel = Math.max(1, oldLevel - 5); // Phạt 5 cấp, tối thiểu là cấp 1
                data.setLevel(oldJobId, penalizedLevel);
                player.sendMessage("§cBạn đã đổi nghề mà không có Thẻ! Nghề cũ bị giảm xuống cấp §e" + penalizedLevel + "§c.");
            }
        } else {
            // [C]. Lần đầu chọn nghề
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

    public static void openStatusMenu(Player player) {
        PlayerJobData data = JobManager.getPlayerData(player);
        RPGJob currentJob = JobManager.getPlayerJob(player);
        if (currentJob == null) { openGUI(player); return; }

        Inventory inv = Bukkit.createInventory(null, 27, "§l§8HỒ SƠ NGHỀ NGHIỆP");
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // 1. Nút Bảng Xếp Hạng (Slot 10)
        inv.setItem(10, createMenuButton(Material.GOLD_BLOCK, "§e§lBẢNG XẾP HẠNG", "§7Xem những nghệ nhân xuất sắc nhất."));

        // 2. Nút Bảng Nhiệm Vụ (Slot 11)
        inv.setItem(11, createMenuButton(Material.WRITABLE_BOOK, "§a§lBẢNG NHIỆM VỤ", "§7Làm nhiệm vụ để nhận phần thưởng."));

        // 3. Icon Nghề (Slot 13 - Trung tâm)
        ItemStack icon = currentJob.getIcon(player).clone();
        ItemMeta meta = icon.getItemMeta();
        List<String> lore = meta.getLore();
        lore.add(" ");
        lore.add("§fTiến độ: §e" + (int)data.getExp() + "§8/§e" + (int)JobManager.getRequiredJobExp(data.getLevel()) + " EXP");
        lore.add(getProgressBar(data.getExp(), JobManager.getRequiredJobExp(data.getLevel()), 20));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        inv.setItem(13, icon);

        // 4. Nút Cửa Hàng (Slot 15)
        inv.setItem(15, createMenuButton(Material.EMERALD, "§6§lCỬA HÀNG NGHỀ", "§7Dùng Mạt Thợ Rèn để mua đồ quý."));

        // 5. Nút Kỹ Năng Tối Thượng (Slot 16)
        inv.setItem(16, createMenuButton(Material.NETHER_STAR, "§d§lKỸ NĂNG TỐI THƯỢNG", "§7Yêu cầu Cấp 50 để mở khóa."));

        player.openInventory(inv);
    }

    private static ItemStack createMenuButton(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(loreLine));
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