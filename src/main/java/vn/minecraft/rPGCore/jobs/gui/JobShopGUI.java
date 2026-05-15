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
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.Arrays;

public class JobShopGUI implements Listener {

    public static final String GUI_NAME = "§l§8CỬA HÀNG NGHỀ NGHIỆP";

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);
        ItemManager im = RPGCore.getInstance().getItemManager();

        ItemStack glass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Slot 11: Thẻ Đổi Nghề (1000 Mạt)
        ItemStack card = im.buildItem("misc_job_change_card", 1);
        ItemMeta cMeta = card.getItemMeta();
        cMeta.setLore(Arrays.asList("§7Vật phẩm đổi nghề không mất cấp.", "", "§fGiá: §e1000 Mạt Thợ Rèn"));
        card.setItemMeta(cMeta);
        inv.setItem(11, card);

        // Slot 13: Đá Nâng Cấp x1 (50 Mạt)
        ItemStack stone = vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 1);
        ItemMeta sMeta = stone.getItemMeta();
        sMeta.setLore(Arrays.asList("§7Đá nâng cấp trang bị cấp 5.", "", "§fGiá: §e50 Mạt Thợ Rèn"));
        stone.setItemMeta(sMeta);
        inv.setItem(13, stone);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        PlayerJobData data = JobManager.getPlayerData(player);
        int slot = event.getRawSlot();

        if (slot == 11) { // Mua thẻ đổi nghề
            if (data.getBlacksmithScraps() >= 1000) {
                data.addBlacksmithScraps(-1000);
                player.getInventory().addItem(RPGCore.getInstance().getItemManager().buildItem("misc_job_change_card", 1));
                player.sendMessage("§a[Shop] §fMua thành công §eThẻ Đổi Nghề§f!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } else player.sendMessage("§cBạn không đủ Mạt Thợ Rèn!");
        } else if (slot == 13) { // Mua đá nâng cấp
            if (data.getBlacksmithScraps() >= 50) {
                data.addBlacksmithScraps(-50);
                player.getInventory().addItem(vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(5, 1));
                player.sendMessage("§a[Shop] §fMua thành công §bĐá Nâng Cấp§f!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } else player.sendMessage("§cBạn không đủ Mạt Thợ Rèn!");
        }
    }
}