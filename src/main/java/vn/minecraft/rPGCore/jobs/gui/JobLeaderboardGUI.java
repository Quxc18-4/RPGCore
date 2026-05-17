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
import org.bukkit.inventory.meta.SkullMeta;
import vn.minecraft.rPGCore.jobs.core.JobLeaderboard;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.RPGJob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JobLeaderboardGUI implements Listener {

    public static final String GUI_NAME = "§l§eBẢNG XẾP HẠNG NGHỀ";

    public static void openGUI(Player player) {
        RPGJob currentJob = JobManager.getPlayerJob(player);
        if (currentJob == null) return;

        Inventory inv = Bukkit.createInventory(null, 45, GUI_NAME);

        ItemStack glass = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 45; i++) inv.setItem(i, glass);

        List<JobLeaderboard.LeaderboardEntry> top10 = JobLeaderboard.getTop10(currentJob.getId());

        // Vị trí xếp hạng đẹp (1 ở giữa trên cùng, 2-3 hai bên, v.v...)
        int[] slots = {13, 21, 23, 29, 30, 31, 32, 33, 39, 41};
        String[] rankColors = {"§e§lTOP 1", "§6§lTOP 2", "§c§lTOP 3", "§f§lTOP 4", "§f§lTOP 5", "§f§lTOP 6", "§f§lTOP 7", "§f§lTOP 8", "§f§lTOP 9", "§f§lTOP 10"};

        for (int i = 0; i < 10; i++) {
            if (i < top10.size()) {
                JobLeaderboard.LeaderboardEntry entry = top10.get(i);
                inv.setItem(slots[i], createPlayerHead(entry.name, rankColors[i], entry.level, entry.exp));
            } else {
                inv.setItem(slots[i], createEmptySlot(rankColors[i]));
            }
        }

        // Nút Quay Lại
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bMeta = back.getItemMeta(); bMeta.setDisplayName("§c§l[!] QUAY LẠI HỒ SƠ"); back.setItemMeta(bMeta);
        inv.setItem(44, back);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
    }

    private static ItemStack createPlayerHead(String playerName, String rankStr, int level, double exp) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(playerName); // Lấy skin người chơi
        meta.setDisplayName(rankStr + " §8- §a" + playerName);

        List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§fCấp độ Nghề: §b" + level);
        lore.add("§fTổng Kinh Nghiệm: §e" + String.format("%.1f", exp));
        lore.add("");
        lore.add("§a► Click để sao chép thông tin!");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack createEmptySlot(String rankStr) {
        ItemStack item = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(rankStr + " §8- §7Chưa có dữ liệu");
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 44) {
            JobSelectionGUI.openStatusMenu(player);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasLore()) {
                String name = meta.getDisplayName();
                player.sendMessage("");
                player.sendMessage("§e[Bảng Xếp Hạng] §fThông tin của người chơi:");
                player.sendMessage(name);
                for (String line : meta.getLore()) {
                    if (!line.contains("Click để sao chép")) player.sendMessage(line);
                }
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.closeInventory();
            }
        }
    }
}