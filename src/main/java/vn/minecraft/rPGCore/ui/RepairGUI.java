package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.commands.RepairAdminCommand;

import java.util.List;

public class RepairGUI implements Listener {

    public static final String GUI_NAME = "§8Trạm Sửa Chữa Trang Bị";

    // Mở GUI khi click phải vào Heavy Core
    @EventHandler
    public void onInteractHeavyCore(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.HEAVY_CORE) {
                if (item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Trạm Sửa Chữa")) {
                    event.setCancelled(true);
                    openGUI(event.getPlayer(), item);
                }
            }
        }
    }

    public void openGUI(Player player, ItemStack kit) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        // Kính xám viền
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            if (i == 10) continue;
            if (i >= 12 && i <= 16) continue;
            inv.setItem(i, glass);
        }

        // Tạo 5 Nút bấm
        inv.setItem(12, createButton(Material.LIME_STAINED_GLASS_PANE, "§a[+] Dùng Đá Thường (500)", "§7Tiêu hao: 1 Đá Mài Thường"));
        inv.setItem(13, createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9[+] Dùng Đá Tốt (1200)", "§7Tiêu hao: 1 Đá Mài Tốt"));
        inv.setItem(14, createButton(Material.PURPLE_STAINED_GLASS_PANE, "§5[+] Dùng Đá Siêu Phẩm (3000)", "§7Tiêu hao: 1 Đá Mài Siêu Phẩm"));
        inv.setItem(15, createButton(Material.DIAMOND, "§b[✦] Sửa Tự Động (Tối Ưu)", "§7Tự động quét túi đồ", "§7và ghép đá mài phù hợp nhất."));
        inv.setItem(16, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§6[★] Sửa Cực Phẩm (100%)", "§7Tiêu hao: 1 Đá Mài Cực Phẩm", "§c§o(Yêu cầu xác nhận)"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 27 && slot != 10) {
            event.setCancelled(true);
        } else {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack equipment = event.getInventory().getItem(10);

        if (slot >= 12 && slot <= 16) {
            if (equipment == null || equipment.getType().isAir()) {
                player.sendMessage("§cBạn chưa đặt trang bị vào ô sửa chữa!");
                return;
            }

            switch (slot) {
                case 12: handleRepair(player, equipment, 1, 500); break;
                case 13: handleRepair(player, equipment, 2, 1200); break;
                case 14: handleRepair(player, equipment, 3, 3000); break;
                case 16: handleRepair(player, equipment, 4, 0); break;
                case 15: handleAutoRepair(player, equipment); break;
            }
        }
    }

    private void handleRepair(Player player, ItemStack item, int stoneType, int repairAmount) {
        ItemStack stoneTemp = RepairAdminCommand.getStoneItem(stoneType, 1);
        if (countCustomItem(player, stoneTemp) < 1) {
            player.sendMessage("§cBạn không có đủ " + stoneTemp.getItemMeta().getDisplayName());
            return;
        }

        boolean success = applyDurability(item, (stoneType == 4) ? 999999 : repairAmount);
        if (success) {
            removeCustomItem(player, stoneTemp, 1);
            player.sendMessage("§a§lSửa chữa thành công!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            consumeKitUsage(player); // KÍCH HOẠT TRỪ LƯỢT DÙNG CỦA KIT SỬA CHỮA
        } else {
            player.sendMessage("§eTrang bị này không cần sửa chữa thêm!");
        }
    }

    private void handleAutoRepair(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        int currentDura = -1, maxDura = -1;

        for (String line : lore) {
            String clean = ChatColor.stripColor(line);
            if (clean.contains("Độ bền:")) {
                String[] parts = clean.replace("- Độ bền:", "").trim().split("/");
                currentDura = Integer.parseInt(parts[0].trim());
                maxDura = Integer.parseInt(parts[1].trim());
                break;
            }
        }

        if (currentDura == -1) return;
        if (currentDura >= maxDura) {
            player.sendMessage("§eTrang bị này đã ở trạng thái hoàn hảo!");
            return;
        }

        int missing = maxDura - currentDura;

        ItemStack s3 = RepairAdminCommand.getStoneItem(3, 1);
        ItemStack s2 = RepairAdminCommand.getStoneItem(2, 1);
        ItemStack s1 = RepairAdminCommand.getStoneItem(1, 1);

        int has3 = countCustomItem(player, s3);
        int has2 = countCustomItem(player, s2);
        int has1 = countCustomItem(player, s1);

        int use3 = 0, use2 = 0, use1 = 0;

        while (missing >= 3000 && has3 > 0) { use3++; has3--; missing -= 3000; }
        while (missing >= 1200 && has2 > 0) { use2++; has2--; missing -= 1200; }
        while (missing > 0 && has1 > 0) { use1++; has1--; missing -= 500; }

        if (missing > 0) {
            if (has2 > 0) { use2++; has2--; missing -= 1200; }
            else if (has3 > 0) { use3++; has3--; missing -= 3000; }
        }

        if (use3 == 0 && use2 == 0 && use1 == 0) {
            player.sendMessage("§cBạn không có viên đá mài nào (Thường/Tốt/Siêu Phẩm) trong túi!");
            return;
        }

        removeCustomItem(player, s3, use3);
        removeCustomItem(player, s2, use2);
        removeCustomItem(player, s1, use1);

        applyDurability(item, (use3 * 3000) + (use2 * 1200) + (use1 * 500));

        player.sendMessage("§b§l[Tự Động Sửa] §fĐã dùng: " +
                (use3 > 0 ? "§5" + use3 + " Siêu Phẩm " : "") +
                (use2 > 0 ? "§9" + use2 + " Tốt " : "") +
                (use1 > 0 ? "§a" + use1 + " Thường" : ""));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);

        consumeKitUsage(player); // KÍCH HOẠT TRỪ LƯỢT DÙNG CỦA KIT SỬA CHỮA
    }

    private boolean applyDurability(ItemStack item, int addAmount) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        boolean changed = false;

        for (int i = 0; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i));
            if (line.contains("Độ bền:")) {
                String[] parts = line.replace("- Độ bền:", "").trim().split("/");
                int currentDura = Integer.parseInt(parts[0].trim());
                int maxDura = Integer.parseInt(parts[1].trim());

                if (currentDura >= maxDura) return false;

                int newDura = Math.min(maxDura, currentDura + addAmount);
                lore.set(i, "§7 - Độ bền: §a" + newDura + "§8/§a" + maxDura);
                changed = true;
                break;
            }
        }

        if (changed) {
            String displayName = meta.getDisplayName();
            if (displayName.startsWith("§8§m")) {
                meta.setDisplayName(displayName.replace("§8§m", ""));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return changed;
    }

    // ==========================================
    // THUẬT TOÁN TRỪ SỐ LẦN DÙNG CỦA KIT
    // ==========================================
    private void consumeKitUsage(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.HEAVY_CORE && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().contains("Trạm Sửa Chữa") && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    for (int j = 0; j < lore.size(); j++) {
                        String line = ChatColor.stripColor(lore.get(j));
                        if (line.startsWith("- Lượt sử dụng còn lại:")) {
                            String[] parts = line.replace("- Lượt sử dụng còn lại:", "").trim().split("/");
                            try {
                                int currentUses = Integer.parseInt(parts[0]);
                                int maxUses = Integer.parseInt(parts[1]);

                                if (currentUses > 1) {
                                    // Vẫn còn dùng được -> Trừ 1
                                    currentUses--;
                                    lore.set(j, "§7- Lượt sử dụng còn lại: §a" + currentUses + "§8/§a" + maxUses);
                                    meta.setLore(lore);
                                    item.setItemMeta(meta);
                                    return;
                                } else {
                                    // Hết lượt -> Bể nát và Tự Đóng GUI
                                    item.setAmount(0);
                                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                                    player.sendMessage("§c§l[!] Trạm Sửa Chữa đã hết lượt sử dụng và vỡ vụn!");
                                    player.closeInventory();
                                    return;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    // Helper: Tính số lượng đá mài
    private int countCustomItem(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    // Helper: Trừ đá mài
    private void removeCustomItem(Player player, ItemStack template, int amount) {
        if (amount <= 0) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) {
                int amt = item.getAmount();
                if (amt >= amount) {
                    item.setAmount(amt - amount);
                    return;
                } else {
                    item.setAmount(0);
                    amount -= amt;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) {
            ItemStack item = event.getInventory().getItem(10);
            if (item != null && !item.getType().isAir()) {
                event.getPlayer().getInventory().addItem(item);
            }
        }
    }
}