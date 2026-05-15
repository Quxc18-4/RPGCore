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
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;

import java.util.Arrays;

public class JobSkillTreeGUI implements Listener {

    public static final String GUI_NAME = "§l§dKỸ NĂNG TỐI THƯỢNG (LV.50)";

    public static void openGUI(Player player) {
        PlayerJobData data = JobManager.getPlayerData(player);

        if (data.getLevel() < 50) {
            player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Kỹ năng 1: Bàn tay của Chúa (Nội tại)
        ItemStack skill1 = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ItemMeta m1 = skill1.getItemMeta();
        m1.setDisplayName("§e§lBÀN TAY CỦA CHÚA");
        m1.setLore(Arrays.asList("§7Nội tại vĩnh viễn:", "§fTăng thêm §a+1% §ftỉ lệ thành công", "§fkhi rèn trang bị.", "", "§a§lĐÃ KÍCH HOẠT"));
        skill1.setItemMeta(m1);
        inv.setItem(11, skill1);

        // Kỹ năng 2: Bậc Thầy Tiết Kiệm
        ItemStack skill2 = new ItemStack(Material.GOLD_INGOT);
        ItemMeta m2 = skill2.getItemMeta();
        m2.setDisplayName("§6§lBẬC THẦY TIẾT KIỆM");
        m2.setLore(Arrays.asList("§7Nội tại vĩnh viễn:", "§fGiảm §e20% §fchi phí Mạt Thợ Rèn", "§fkhi sử dụng Rèn Bậc Thầy.", "", "§a§lĐÃ KÍCH HOẠT"));
        skill2.setItemMeta(m2);
        inv.setItem(13, skill2);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) event.setCancelled(true);
    }
}