package vn.minecraft.rPGCore.jobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.managers.ItemManager;

public class JobShopGUI implements Listener {

    public static final String GUI_NAME = "§l§8CỬA HÀNG NGHỀ NGHIỆP";

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);
        ItemManager im = RPGCore.getInstance().getItemManager();

        // Nền
        ItemStack glass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Mặt hàng 1: Thẻ đổi nghề (Giá: 1000 Mạt)
        inv.setItem(11, im.buildItem("misc_job_change_card", 1));
        // Mặt hàng 2: Đá nâng cấp (Giá: 50 Mạt)
        inv.setItem(13, vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 1));
        // Mặt hàng 3: Búa Thợ Rèn Cổ Đại (Ví dụ item hiếm)
        inv.setItem(15, new ItemStack(Material.GOLDEN_PICKAXE)); // Giả định

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        PlayerJobData data = JobManager.getPlayerData(player);
        int slot = event.getRawSlot();

        // Ví dụ logic mua Thẻ Đổi Nghề
        if (slot == 11) {
            if (data.getBlacksmithScraps() >= 1000) {
                data.addBlacksmithScraps(-1000);
                player.getInventory().addItem(RPGCore.getInstance().getItemManager().buildItem("misc_job_change_card", 1));
                player.sendMessage("§aBạn đã mua 1x Thẻ Đổi Nghề!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } else {
                player.sendMessage("§cBạn không đủ Mạt Thợ Rèn!");
            }
        }
    }
}