package vn.minecraft.rPGCore.playercommunication.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.ui.CustomGUI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

import java.util.ArrayList;
import java.util.List;

public class InteractMenuListener implements Listener {

    private final RPGCore plugin;

    public InteractMenuListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            if (event.getRightClicked() instanceof Player) {
                Player target = (Player) event.getRightClicked();
                event.setCancelled(true);
                openInteractGUI(player, target);
            }
        }
    }

    private void openInteractGUI(Player player, Player target) {
        CustomGUI gui = new CustomGUI("INTERACT_MENU_" + target.getName(), 1);

        // Đã bỏ nút Đóng, gom lại còn 9 ô vừa khít
        Inventory inv = Bukkit.createInventory(gui, 9, "§0Tương tác: §8" + target.getName());

        // [ĐÃ SỬA]: Cấu trúc nút mới theo yêu cầu
        inv.setItem(0, createItem(Material.IRON_CHESTPLATE, "§a§lKiểm Tra Trang Bị", "§7Xem trang bị của " + target.getName()));
        inv.setItem(1, createItem(Material.BOOK, "§b§lKiểm Tra Chỉ Số", "§7Xem thông số sức mạnh."));
        inv.setItem(2, createItem(Material.PAPER, "§d§lNhắn Tin Mật", "§7Gửi tin nhắn riêng."));
        inv.setItem(3, createItem(Material.POPPY, "§e§lBạn Bè", "§7Quản lý quan hệ hảo hữu."));
        inv.setItem(4, createItem(Material.IRON_SWORD, "§6§lTổ Đội", "§7Mời vào Party của bạn."));
        inv.setItem(5, createItem(Material.SHIELD, "§9§lBang Hội", "§7Mời tham gia Bang hội."));
        inv.setItem(6, createItem(Material.GOLD_INGOT, "§e§lGiao Dịch", "§7Trao đổi vật phẩm & Tiền tệ an toàn."));
        inv.setItem(7, createItem(Material.LODESTONE, "§a§lKhách Mời Lãnh Thổ", "§7Cấp quyền thao tác trong nhà (Waystone)."));
        inv.setItem(8, createItem(Material.DIAMOND_SWORD, "§c§lThách Đấu (Duel)", "§7Chuyển tới Đấu Trường.", "§7(Sát thương giảm 80%, không rớt đồ)"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    @EventHandler
    public void onInteractGUIClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomGUI)) return;
        CustomGUI gui = (CustomGUI) event.getInventory().getHolder();

        if (gui.getGuiId().startsWith("INTERACT_MENU_")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            String targetName = gui.getGuiId().replace("INTERACT_MENU_", "");
            int slot = event.getRawSlot();

            player.closeInventory();

            switch (slot) {
                case 0:
                    player.performCommand("profile " + targetName);
                    break;
                case 1:
                    player.performCommand("stats " + targetName); // Bác thay bằng lệnh check chỉ số thực tế của bác
                    break;
                case 2:
                    // [ĐÃ SỬA]: Gửi TextComponent Clickable để bù đắp hạn chế của Minecraft
                    TextComponent msg = new TextComponent("§e§l[Hệ Thống] §fClick vào đây để nhắn tin cho §a" + targetName);
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + targetName + " "));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Bấm để mở thanh chat")));
                    player.spigot().sendMessage(msg);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    break;
                case 3:
                    player.performCommand("friend add " + targetName);
                    break;
                case 4:
                    player.performCommand("party invite " + targetName);
                    break;
                case 5:
                    player.performCommand("guild invite " + targetName);
                    break;
                case 6:
                    player.performCommand("trade " + targetName);
                    break;
                case 7:
                    player.performCommand("dens friend add " + targetName);
                    break;
                case 8:
                    // [ĐÃ SỬA LỖI OFFLINE]: Gửi thẳng tên, không có chữ invite
                    player.performCommand("duel " + targetName);
                    break;
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) { lore.add(line); }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}