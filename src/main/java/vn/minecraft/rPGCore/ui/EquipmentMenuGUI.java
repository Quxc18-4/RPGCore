package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class EquipmentMenuGUI implements Listener {

    public static final String GUI_NAME = "§8§lLò Rèn Tổng Hợp";

    // Bắt sự kiện Shift + Click Phải vào Đe (Anvil)
    @EventHandler
    public void onAnvilInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Material type = clickedBlock.getType();
        if (event.getPlayer().isSneaking() && (type == Material.ANVIL || type == Material.CHIPPED_ANVIL || type == Material.DAMAGED_ANVIL)) {
            event.setCancelled(true);
            this.openGUI(event.getPlayer());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);
        }
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        ItemStack blackGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Phủ nền đen
        for (int i = 0; i < 27; i++) inv.setItem(i, blackGlass);

        // Bo góc
        int[] graySlots = {0, 8, 9, 17, 18, 26};
        for (int s : graySlots) inv.setItem(s, grayGlass);

        // 1. Nút Đập Đồ (BlacksmithGUI)
        inv.setItem(11, createItem(Material.ANVIL, "§a§l[+] Cường Hóa Trang Bị",
                "§7Sử dụng Đá Nâng Cấp để",
                "§7đột phá sức mạnh của",
                "§7vũ khí và phòng cụ.",
                "",
                "§e▶ Click để mở Lò Cường Hóa!"));

        // 2. Nút Rèn Chế Tạo (Tính năng sắp ra mắt)
        inv.setItem(13, createItem(Material.IRON_INGOT, "§6§l[?] Chế Tạo Trang Bị",
                "§7Tự tay rèn ra những món",
                "§7trang bị độc nhất từ bản vẽ.",
                "",
                "§c▶ Tính năng đang được phát triển..."));

        // 3. Nút Dung Hợp (EquipmentStrongboxGUI)
        inv.setItem(15, createItem(Material.ENDER_CHEST, "§d§l[★] Dung Hợp Trang Bị",
                "§7Hiến tế 3 trang bị rác",
                "§7cùng phẩm chất để dung hợp",
                "§7thành một trang bị hoàn toàn mới.",
                "",
                "§e▶ Click để mở Dung Hợp!"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11) { // Mở Cường hóa
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new BlacksmithGUI().openGUI(player);
        } else if (slot == 13) { // Rèn đồ
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new EquipmentCategoryGUI().openGUI(player);
        } else if (slot == 15) { // Mở Dung Hợp
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new EquipmentStrongboxGUI().openGUI(player);
        }
    }
}