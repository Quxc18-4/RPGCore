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
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.List;

public class ClassGUI {

    public static final String GUI_TITLE = "§8§lCHỌN CHỨC NGHIỆP NHÂN VẬT";
    private static final int[] CLASS_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};

    public void openGUI(Player player, List<RPGClass> availableClasses) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        RPGCore core = RPGCore.getInstance();
        RPGPlayer rpgPlayer = (core != null && core.getPlayerManager() != null)
                ? core.getPlayerManager().getRPGPlayer(player.getUniqueId())
                : null;

        String currentClassId = (rpgPlayer != null && rpgPlayer.getCurrentClass() != null)
                ? rpgPlayer.getCurrentClass().getId()
                : "commoner";
        boolean isCommoner = currentClassId.equalsIgnoreCase("commoner");

        boolean hasScroll = false;
        if (!isCommoner && core != null && core.getItemManager() != null) {
            ItemStack scrollTemplate = core.getItemManager().buildItem("class_change_scroll", 1);
            hasScroll = scrollTemplate != null && core.getItemManager().countItem(player, scrollTemplate) > 0;
        }

        for (int i = 0; i < availableClasses.size() && i < CLASS_SLOTS.length; i++) {
            RPGClass rpgClass = availableClasses.get(i);
            ItemStack icon = new ItemStack(rpgClass.getIcon());
            ItemMeta meta = icon.getItemMeta();
            if (meta == null) continue;

            // Set tên và mô tả
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rpgClass.getDisplayName()));
            List<String> lore = new ArrayList<>();
            for (String line : rpgClass.getDescription()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add("");

            if (rpgClass.getId().equalsIgnoreCase(currentClassId)) {
                lore.add("§aChức Nghiệp hiện tại của nhân vật này");
            } else if (isCommoner) {
                lore.add("§e▶ Nhấp để gia nhập Chức Nghiệp này");
            } else if (hasScroll) {
                lore.add("§d▶ Nhấp để đổi Chức Nghiệp (Giữ nguyên Cấp độ & Chỉ số)");
                lore.add("§7Tiêu hao: §51x Cuộn Giấy Chuyển Chức Nghiệp");
            } else {
                lore.add("§cNhấp để đổi Chức Nghiệp (Reset về Cấp độ & Chỉ số)");
                lore.add("§7Mang §5[Cuộn Giấy Chuyển Chức Nghiệp] §7để giữ lại tiến độ");
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

            if (rpgClass.isEnchanted() || rpgClass.getId().equalsIgnoreCase(currentClassId)) {
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            icon.setItemMeta(meta);
            gui.setItem(CLASS_SLOTS[i], icon);
        }

        player.openInventory(gui);
    }
}