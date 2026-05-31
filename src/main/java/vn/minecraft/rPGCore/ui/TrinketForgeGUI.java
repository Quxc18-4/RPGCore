package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TrinketForgeGUI implements CommandExecutor, Listener {

    public static final String GUI_NAME = "§d§lGia Công Trang Sức";
    private final Random rand = new Random();

    private final double[] RATES = {100.0, 80.0, 60.0, 40.0, 20.0};
    private final int[] MASTER_COSTS = {3, 8, 15, 25, 40};

    // [ĐÃ SỬA]: Vẫn giữ nguyên cấu trúc hàm khởi tạo để file RPGCore.java của bạn không bị báo lỗi
    public TrinketForgeGUI(ItemManager ignoredManager) {
    }

    // Hàm lấy ItemManager trực tiếp, đảm bảo không bao giờ bị null
    private ItemManager getItemManager() {
        return RPGCore.getInstance().getItemManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) openGUI((Player) sender);
        return true;
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);
        ItemStack glass = createButton(Material.PINK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 27; i++) {
            if (i == 10 || (i >= 12 && i <= 14) || i == 16) continue;
            inv.setItem(i, glass);
        }

        inv.setItem(15, glass);
        updateButtons(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) meta.setLore(Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    private void updateButtons(Inventory inv) {
        ItemStack trinket = inv.getItem(10);
        int currentLevel = 0;
        boolean hasItem = false;

        if (trinket != null && trinket.hasItemMeta() && trinket.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "trinket_type"), PersistentDataType.STRING)) {
            currentLevel = getUpgradeLevel(trinket);
            hasItem = true;
        }

        List<String> lore1 = new ArrayList<>(Arrays.asList("§7Tiêu hao: 1 Tinh Thể Pha Lê", "§7(Không rớt cấp khi thất bại)"));
        List<String> lore10 = new ArrayList<>(Arrays.asList("§7Tiêu hao: Tối đa 10 Tinh Thể", "§7(Sẽ dừng lại ngay khi lên cấp)"));
        List<String> lore100 = new ArrayList<>(Arrays.asList("§7Tiêu hao: Tối đa 100 Tinh Thể", "§7(Sẽ dừng lại ngay khi lên cấp)"));

        if (hasItem && currentLevel < 5) {
            String rateStr = "§fTỉ lệ thành công: §a" + RATES[currentLevel] + "%";
            lore1.add(""); lore1.add(rateStr);
            lore10.add(""); lore10.add(rateStr);
            lore100.add(""); lore100.add(rateStr);

            inv.setItem(16, createButton(Material.AMETHYST_CLUSTER, "§d[★] Cường Hóa Bậc Thầy",
                    "§7Đột phá thẳng lên §d[+" + (currentLevel + 1) + "]",
                    "§7Tiêu hao: §e" + MASTER_COSTS[currentLevel] + " Tinh Thể Pha Lê",
                    "",
                    "§aTỉ lệ thành công: 100%"
            ));
        } else {
            lore1.add(""); lore1.add("§cChưa có hoặc đã max cấp");
            lore10.add(""); lore10.add("§cChưa có hoặc đã max cấp");
            lore100.add(""); lore100.add("§cChưa có hoặc đã max cấp");

            inv.setItem(16, createButton(Material.AMETHYST_CLUSTER, "§d[★] Cường Hóa Bậc Thầy", "§cChưa có trang sức hoặc đã max cấp."));
        }

        inv.setItem(12, createButton(Material.LIME_STAINED_GLASS_PANE, "§a[+] Đập 1 Lần", lore1.toArray(new String[0])));
        inv.setItem(13, createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9[+] Đập Tự Động (Max 10)", lore10.toArray(new String[0])));
        inv.setItem(14, createButton(Material.PURPLE_STAINED_GLASS_PANE, "§5[+] Đập Tự Động (Max 100)", lore100.toArray(new String[0])));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot == 10 || event.getClick().isShiftClick()) {
            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> updateButtons(player.getOpenInventory().getTopInventory()), 1L);
            return;
        }

        if (slot >= 0 && slot < 27 && (slot < 12 || slot > 14) && slot != 16) {
            event.setCancelled(true);
            return;
        }

        ItemStack trinket = event.getInventory().getItem(10);

        if (slot >= 12 && slot <= 14) {
            event.setCancelled(true);
            if (!isValidTrinket(trinket)) {
                player.sendMessage("§cBạn chưa đặt Trang sức vào ô!");
                return;
            }
            int attempts = (slot == 12) ? 1 : (slot == 13) ? 10 : 100;
            processLoop(player, trinket, attempts);
        } else if (slot == 16) {
            event.setCancelled(true);
            if (!isValidTrinket(trinket)) {
                player.sendMessage("§cBạn chưa đặt Trang sức vào ô!");
                return;
            }

            int currentLevel = getUpgradeLevel(trinket);
            if (currentLevel >= 5) {
                player.sendMessage("§cTrang sức đã đạt sức mạnh tối đa!");
                return;
            }

            int cost = MASTER_COSTS[currentLevel];
            ItemStack crystalTemplate = vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(6, 1);

            if (getItemManager().countItem(player, crystalTemplate) < cost) {
                player.sendMessage("§c§l[!] §cBạn cần " + cost + " Tinh Thể Pha Lê để Cường hóa Bậc thầy!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            getItemManager().takeItem(player, crystalTemplate, cost);
            applyTrinketStatsChange(trinket, currentLevel, currentLevel + 1);

            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            player.sendMessage("§d§l[Gia Công] §fĐột phá Bậc thầy! Trang sức tiến thẳng lên §d[+" + (currentLevel + 1) + "]");

            updateButtons(event.getInventory());
        }
    }

    private void processLoop(Player player, ItemStack trinket, int maxAttempts) {
        int currentLevel = getUpgradeLevel(trinket);
        if (currentLevel >= 5) {
            player.sendMessage("§cTrang sức đã đạt cấp độ tối đa!");
            return;
        }

        ItemStack crystalTemplate = vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(6, 1);
        int crystalsUsed = 0;
        boolean success = false;

        for (int i = 0; i < maxAttempts; i++) {
            if (currentLevel >= 5) break;
            if (getItemManager().countItem(player, crystalTemplate) <= 0) break;

            getItemManager().takeItem(player, crystalTemplate, 1);
            crystalsUsed++;

            if (rand.nextDouble() * 100.0 <= RATES[currentLevel]) {
                applyTrinketStatsChange(trinket, currentLevel, currentLevel + 1);
                success = true;
                currentLevel++;
                break; // Dừng ngay khi thành công
            }
        }

        if (crystalsUsed > 0) {
            if (success) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.sendMessage("§aNâng cấp thành công lên §d[+" + currentLevel + "] §8(Tiêu hao " + crystalsUsed + " Tinh thể)");
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                player.sendMessage("§cĐập " + crystalsUsed + " lần thất bại! Trang sức may mắn không bị rớt cấp.");
            }
            updateButtons(player.getOpenInventory().getTopInventory());
        } else {
            player.sendMessage("§cBạn không có đủ Tinh Thể Pha Lê trong túi!");
        }
    }

    private boolean isValidTrinket(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "trinket_type"), PersistentDataType.STRING);
    }

    private int getUpgradeLevel(ItemStack item) {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\+(\\d+)\\]$").matcher(name);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private double getMultiplier(int level) {
        return 1.0 + (level * 0.15);
    }

    private void applyTrinketStatsChange(ItemStack item, int oldLevel, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        double oldMulti = getMultiplier(oldLevel);
        double newMulti = getMultiplier(newLevel);

        boolean isMagicSection = false;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String clean = ChatColor.stripColor(line).trim();

            if (clean.startsWith("✦ Thuộc tính Ma thuật:")) { isMagicSection = true; continue; }
            if (clean.startsWith("[Set")) isMagicSection = false;

            if (isMagicSection && clean.startsWith("- ")) {
                int colonIdx = line.indexOf(":");
                if (colonIdx == -1) continue;

                String valuePart = line.substring(colonIdx + 1);
                // Bắt chính xác phần số thập phân (có hoặc không có dấu +)
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([+0-9]*\\.?[0-9]+)").matcher(ChatColor.stripColor(valuePart));

                if (m.find()) {
                    String numStr = m.group(1);
                    try {
                        double oldVal = Double.parseDouble(numStr.replace("+", ""));
                        double newVal = (oldVal / oldMulti) * newMulti;

                        // Tạo chuỗi số mới, giữ lại dấu + nếu số cũ có dấu +
                        String newNumStr = (numStr.startsWith("+") ? "+" : "") + String.format("%.1f", newVal);

                        // Thay thế chính xác cụm số cũ bằng cụm số mới
                        String newLine = line.replaceFirst(java.util.regex.Pattern.quote(numStr), newNumStr);
                        lore.set(i, newLine);
                    } catch (Exception ignored) {}
                }
            }
        }

        String name = meta.getDisplayName();
        name = name.replaceAll(" (?:§.)?\\[\\+\\d+\\]", "");
        if (newLevel > 0) name = name + " §d[+" + newLevel + "]";
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) {
            ItemStack item = event.getInventory().getItem(10);
            if (item != null && !item.getType().isAir()) event.getPlayer().getInventory().addItem(item);
        }
    }
}