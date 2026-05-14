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
}