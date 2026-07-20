package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;

import java.util.ArrayList;
import java.util.List;

public class MonthlyRewardGUI {

    // 31 vị trí hoàn hảo để căn giữa trên GUI 54 ô
    private static final int[] SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,    // Dòng 2 (9 ô)
            18, 19, 20, 21, 22, 23, 24, 25, 26,   // Dòng 3 (9 ô)
            27, 28, 29, 30, 31, 32, 33, 34, 35,   // Dòng 4 (9 ô)
            38, 39, 40, 41                        // Dòng 5 (4 ô căn giữa)
    };

    public static void open(Player player, int currentProgress, boolean canClaimToday) {
        CustomGUI holder = new CustomGUI("MONTHLY_USER_GUI", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8§lĐIỂM DANH TÍCH LŨY");

        // 1. Phủ kính đen làm viền trang trí
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // 2. Load 31 ngày điểm danh
        for (int i = 0; i < 31; i++) {
            int day = i + 1;
            int slot = SLOTS[i];
            ItemStack icon;

            // Xử lý 3 Trạng thái: Đã Nhận (Tím) / Có thể nhận (Xanh) / Chưa tới (Đỏ)
            if (day <= currentProgress) {
                icon = buildIcon(Material.PURPLE_STAINED_GLASS_PANE, "§5Ngày " + day + " (Đã nhận)", false, day);
            }
            else if (day == currentProgress + 1 && canClaimToday) {
                icon = buildIcon(Material.LIME_STAINED_GLASS_PANE, "§a§lNgày " + day + " (Nhấn để nhận)", true, day);
            }
            else {
                icon = buildIcon(Material.RED_STAINED_GLASS_PANE, "§cNgày " + day + " (Chưa mở khóa)", false, day);
            }
            inv.setItem(slot, icon);
        }

        // 3. Thông tin tiến độ ở dòng cuối (Ô 49)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§lThông tin Điểm Danh");
        infoMeta.setLore(List.of(
                "§7Tiến độ của bạn: §a" + currentProgress + "§8/§a31",
                "§7Trạng thái hôm nay: " + (canClaimToday ? "§aCó thể nhận quà!" : "§cĐã nhận, hãy quay lại vào ngày mai.")
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        player.openInventory(inv);
    }

    private static ItemStack buildIcon(Material mat, String name, boolean glowing, int day) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        // [TÍNH NĂNG XỊN]: Tự động lấy danh sách phần thưởng từ DB để in ra màn hình
        List<String> lore = new ArrayList<>();
        ItemStack[] rewards = RPGCore.getInstance().getRewardManager().getMonthlyRewardsArray(day);

        if (rewards != null && rewards.length > 0) {
            lore.add(" ");
            lore.add("§f§nPhần thưởng gồm:");
            lore.add(" ");
            for (ItemStack rw : rewards) {
                String rwName = (rw.hasItemMeta() && rw.getItemMeta().hasDisplayName())
                        ? rw.getItemMeta().getDisplayName() : rw.getType().name();
                lore.add("§7- " + rw.getAmount() + "x " + rwName);
            }
        } else {
            lore.add("§7(Chưa có phần thưởng)");
        }

        if (glowing) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}