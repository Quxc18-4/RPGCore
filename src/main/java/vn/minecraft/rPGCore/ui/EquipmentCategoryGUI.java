package vn.minecraft.rPGCore.ui;

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

import java.util.Arrays;

public class EquipmentCategoryGUI implements Listener {

    public static final String GUI_NAME = "§8§lBản Vẽ Trang Bị";

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, GUI_NAME);

        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) inv.setItem(i, bg);

        // --- NHÓM PHÒNG CỤ (Armor) ---
        inv.setItem(10, createItem(Material.IRON_HELMET, "§a§l[ Rèn Mũ Giáp ]", "§7Chế tạo các loại nón bảo hộ."));
        inv.setItem(11, createItem(Material.IRON_CHESTPLATE, "§a§l[ Rèn Áo Giáp ]", "§7Chế tạo các loại giáp thân."));
        inv.setItem(12, createItem(Material.IRON_LEGGINGS, "§a§l[ Rèn Quần Giáp ]", "§7Chế tạo quần bảo hộ."));
        inv.setItem(13, createItem(Material.IRON_BOOTS, "§a§l[ Rèn Giày Giáp ]", "§7Chế tạo các loại giày chiến đấu."));

        // --- NHÓM VŨ KHÍ (Weapons) ---
        inv.setItem(19, createItem(Material.IRON_SWORD, "§c§l[ Rèn Kiếm ]", "§7Vũ khí cận chiến của Warrior."));
        inv.setItem(20, createItem(Material.BOW, "§c§l[ Rèn Cung/Nỏ ]", "§7Vũ khí tầm xa của Archer."));
        inv.setItem(21, createItem(Material.STICK, "§c§l[ Rèn Gậy Phép ]", "§7Vũ khí ma thuật của Mage."));
        inv.setItem(22, createItem(Material.SHEARS, "§c§l[ Rèn Dao Găm ]", "§7Vũ khí ám sát của Assassin."));
        inv.setItem(23, createItem(Material.LEAD, "§c§l[ Rèn Roi Da ]", "§7Vũ khí điều khiển của Summoner."));

        // Khóa Khiên & Lưỡi Hái
        inv.setItem(24, createItem(Material.BARRIER, "§7§l[ Rèn Khiên ]", "§cTính năng Đỡ Đòn sắp ra mắt..."));
        inv.setItem(25, createItem(Material.BARRIER, "§7§l[ Rèn Lưỡi Hái ]", "§cTính năng Thần Chết sắp ra mắt..."));

        inv.setItem(31, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l[ Quay Về Lò Rèn Tổng ]", "§7Quay lại Menu trước đó."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
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

        String category = null;
        switch (slot) {
            case 10: category = "Helmet"; break;
            case 11: category = "Chestplate"; break;
            case 12: category = "Leggings"; break;
            case 13: category = "Boots"; break;
            case 19: category = "Sword"; break;
            case 20: category = "Bow"; break;
            case 21: category = "Staff"; break;
            case 22: category = "Dagger"; break;
            case 23: category = "Whip"; break;
            case 24:
            case 25:
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            case 31:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                new EquipmentMenuGUI().openGUI(player);
                return;
        }

        if (category != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 2f);
            new EquipmentRecipeCraftGUI().openGUI(player, category);
        }
    }
}