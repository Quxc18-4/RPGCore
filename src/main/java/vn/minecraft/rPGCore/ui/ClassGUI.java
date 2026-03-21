package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.classes.RPGClass;

import java.util.ArrayList;
import java.util.List;

public class ClassGUI {

    public static final String GUI_TITLE = "§8§lCHỌN CLASS NHÂN VẬT";

    // Vị trí cố định đặt 7 class sao cho căn giữa và cách nhau 1 ô (GUI 36 ô)
    private static final int[] CLASS_SLOTS = {11, 13, 15, 28, 30, 32, 34};

    public void openGUI(Player player, List<RPGClass> availableClasses) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        // 1. Lấp đầy toàn bộ bằng Kính Đen không tên
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" "); // Để khoảng trắng để không hiện tooltip
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        // 2. Đặt các Class vào đúng vị trí
        for (int i = 0; i < availableClasses.size() && i < CLASS_SLOTS.length; i++) {
            RPGClass rpgClass = availableClasses.get(i);
            ItemStack icon = new ItemStack(rpgClass.getIcon());
            ItemMeta meta = icon.getItemMeta();

            // Set tên và mô tả
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rpgClass.getDisplayName()));
            List<String> lore = new ArrayList<>();
            for (String line : rpgClass.getDescription()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add("");
            lore.add("§eNhấp để chọn class này!");
            meta.setLore(lore);

            // ==========================================
            // THÊM DÒNG NÀY ĐỂ ẨN "When in Main Hand..."
            // ==========================================
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            // Thêm hiệu ứng phát sáng (Enchant) cho Mage/Reaper
            if (rpgClass.isEnchanted()) {
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Ẩn dòng chữ "Unbreaking I"
            }

            icon.setItemMeta(meta);
            gui.setItem(CLASS_SLOTS[i], icon);
        }

        player.openInventory(gui);
    }
}