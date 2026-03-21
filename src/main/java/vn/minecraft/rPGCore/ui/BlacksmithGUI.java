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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.commands.RepairAdminCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BlacksmithGUI implements Listener {

    public static final String GUI_NAME = "§8Lò Rèn Trang Bị";
    // Tỉ lệ: 0->1(100%), 1->2(90%)... 14->15(0.1%)
    private final double[] RATES = {100, 90, 80, 65, 50, 35, 25, 15, 5, 3, 2, 1, 0.5, 0.3, 0.1};
    private final List<Integer> SAFE_LEVELS = Arrays.asList(0, 3, 6, 9, 12, 15);
    private final Random rand = new Random();

    @EventHandler
    public void onInteractAnvil(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.ANVIL || event.getClickedBlock().getType() == Material.CHIPPED_ANVIL || event.getClickedBlock().getType() == Material.DAMAGED_ANVIL) {
                event.setCancelled(true);
                openGUI(event.getPlayer());
            }
        }
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            if (i == 10) continue;
            if (i >= 12 && i <= 16) continue;
            inv.setItem(i, glass);
        }

        inv.setItem(12, createButton(Material.LIME_STAINED_GLASS_PANE, "§a[+] Đập 1 Lần", "§7Tiêu hao: 1 Đá Nâng Cấp"));
        inv.setItem(13, createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9[+] Đập Tự Động (Max 10 Lần)", "§7Tiêu hao: Tối đa 10 Đá Nâng Cấp", "§7(Sẽ dừng lại ngay khi lên cấp)"));
        inv.setItem(14, createButton(Material.PURPLE_STAINED_GLASS_PANE, "§5[+] Đập Tự Động (Max 100 Lần)", "§7Tiêu hao: Tối đa 100 Đá Nâng Cấp", "§7(Sẽ dừng lại ngay khi lên cấp)"));
        inv.setItem(15, glass);
        inv.setItem(16, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§6[★] Rèn Bậc Thầy (Thợ Rèn)", "§cĐang phát triển (TODO)", "§7Cần nghề phụ Thợ Rèn để sử dụng."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 27 && slot != 10) {
            event.setCancelled(true);
        } else { return; }

        Player player = (Player) event.getWhoClicked();
        ItemStack equipment = event.getInventory().getItem(10);

        if (slot >= 12 && slot <= 14) {
            if (equipment == null || equipment.getType().isAir() || !equipment.hasItemMeta() || !equipment.getItemMeta().hasLore()) {
                player.sendMessage("§cBạn chưa đặt trang bị RPG vào ô nâng cấp!");
                return;
            }

            int attempts = 1;
            if (slot == 13) attempts = 10;
            if (slot == 14) attempts = 100;

            processUpgradeLoop(player, equipment, attempts);
        }
    }

    // ===============================================
    // HỆ THỐNG XỬ LÝ VÒNG LẶP & DỪNG THÔNG MINH
    // ===============================================
    private void processUpgradeLoop(Player player, ItemStack item, int maxAttempts) {
        ItemStack stoneTemplate = RepairAdminCommand.getStoneItem(5, 1);
        int stonesUsed = 0;
        int currentLevel = getUpgradeLevel(item);
        int startLevel = currentLevel;

        // Mục tiêu của người chơi là nâng được 1 cấp so với ban đầu
        int targetLevel = startLevel + 1;

        if (currentLevel >= 15) {
            player.sendMessage("§cTrang bị này đã đạt cấp độ tối đa (+15)!");
            return;
        }

        for (int i = 0; i < maxAttempts; i++) {
            if (currentLevel >= 15) break;
            if (countCustomItem(player, stoneTemplate) <= 0) break; // Hết đá

            removeCustomItem(player, stoneTemplate, 1);
            stonesUsed++;

            double successRate = RATES[currentLevel];
            double roll = rand.nextDouble() * 100.0;

            if (roll <= successRate) {
                // Thành công Lên cấp
                currentLevel++;

                // Chỉ buff Ngôi Sao khi vừa đạp trúng mốc bảo hiểm BẰNG CÁCH NÂNG CẤP THÀNH CÔNG
                if (SAFE_LEVELS.contains(currentLevel)) {
                    applyMilestoneBonus(item);
                }

                // [QUAN TRỌNG] NẾU ĐÃ ĐẠT ĐƯỢC MỤC TIÊU LÊN 1 CẤP -> DỪNG ĐẬP NGAY LẬP TỨC
                if (currentLevel == targetLevel) {
                    break;
                }
            } else {
                // Thất bại Rớt cấp (Nếu không nằm ở mốc bảo hiểm)
                if (!SAFE_LEVELS.contains(currentLevel)) {
                    currentLevel--;
                }
            }
        }

        // Áp dụng thay đổi chỉ số chính ĐÚNG 1 LẦN sau khi chốt kết quả
        if (currentLevel != startLevel) {
            applyStatsChange(item, startLevel, currentLevel);
        }

        // Gửi thông báo
        if (stonesUsed > 0) {
            if (currentLevel > startLevel) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.sendMessage("§a§lNâng cấp thành công! §fTrang bị đạt mức " + getLevelColor(currentLevel) + "[+" + currentLevel + "] §8(Dừng sau " + stonesUsed + " lần đập)");
            } else if (currentLevel < startLevel) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1f, 0.5f);
                player.sendMessage("§c§lNâng cấp thất bại! §fTrang bị rớt xuống " + getLevelColor(currentLevel) + "[+" + currentLevel + "] §8(Tiêu hao " + stonesUsed + " đá)");
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                player.sendMessage("§eĐập " + stonesUsed + " lần thất bại! §fTrang bị giữ nguyên " + getLevelColor(currentLevel) + "[+" + currentLevel + "]");
            }
        } else {
            player.sendMessage("§cBạn không có Đá Nâng Cấp trong túi!");
        }
    }

    // Lấy cấp độ an toàn bằng Regex
    private int getUpgradeLevel(ItemStack item) {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\+(\\d+)\\]$").matcher(name);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    // Trả về Mã màu dựa theo Cấp độ
    private String getLevelColor(int level) {
        if (level >= 1 && level <= 3) return "§a";   // Xanh lá
        if (level >= 4 && level <= 6) return "§9";   // Xanh dương
        if (level >= 7 && level <= 9) return "§5";   // Tím
        if (level >= 10 && level <= 12) return "§c"; // Đỏ
        if (level >= 13 && level <= 15) return "§6"; // Vàng
        return "§e";
    }

    // Tính hệ số nhân dựa trên Cấp độ
    private double getMultiplier(int level) {
        if (level == 0) return 1.0;
        double multi = 1.0;
        multi += Math.min(level, 10) * 0.10; // +10% cho mỗi cấp từ 1-10
        if (level > 10) {
            multi += (level - 10) * 0.15; // +15% cho mỗi cấp từ 11-15
        }
        return multi;
    }

    // ===============================================
    // CẬP NHẬT CHỈ SỐ KHI LÊN CẤP (Hỗ trợ Kháng Phép)
    // ===============================================
    private void applyStatsChange(ItemStack item, int oldLevel, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        double oldMulti = getMultiplier(oldLevel);
        double newMulti = getMultiplier(newLevel);

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String clean = ChatColor.stripColor(line).trim();

            if (clean.startsWith("- Sát thương vật lý:") || clean.startsWith("- Sát thương phép thuật:")) {
                String[] parts = clean.split(":");
                String prefix = line.substring(0, line.indexOf(":") + 1);
                String[] range = parts[1].split("~");

                double oldMin = Double.parseDouble(ChatColor.stripColor(range[0]).replace("+", "").trim());
                double oldMax = Double.parseDouble(ChatColor.stripColor(range[1]).replace("+", "").trim());

                double newMin = (oldMin / oldMulti) * newMulti;
                double newMax = (oldMax / oldMulti) * newMulti;

                lore.set(i, prefix + " §f" + String.format("%.1f", newMin) + " §8~ §f" + String.format("%.1f", newMax));
            }
            // THÊM: Hỗ trợ cộng dồn Kháng Phép
            else if (clean.startsWith("- Máu tối đa:") || clean.startsWith("- Giáp:") || clean.startsWith("- Kháng phép:")) {
                String[] parts = clean.split(":");
                String prefix = line.substring(0, line.indexOf(":") + 1);
                double oldVal = Double.parseDouble(ChatColor.stripColor(parts[1]).replace("+", "").replace("%", "").trim());

                double newVal = (oldVal / oldMulti) * newMulti;
                String color = clean.startsWith("- Kháng phép:") ? "§b+" : (clean.startsWith("- Giáp:") ? "§f+" : "§a+");
                lore.set(i, prefix + " " + color + Math.round(newVal));
            }
        }

        String name = meta.getDisplayName();
        name = name.replaceAll(" (?:§.)?\\[\\+\\d+\\]", "");
        if (newLevel > 0) name = name + " " + getLevelColor(newLevel) + "[+" + newLevel + "]";
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // Buff ngẫu nhiên Dòng dõi (Tỉ lệ 50%) và 1 Dòng phụ
    private void applyMilestoneBonus(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        List<Integer> lineageLines = new ArrayList<>();
        List<Integer> subStatLines = new ArrayList<>();

        boolean isLineageSection = false;
        boolean isSubStatSection = false;

        for (int i = 0; i < lore.size(); i++) {
            String clean = ChatColor.stripColor(lore.get(i)).trim();

            if (clean.startsWith("✦ Thuộc tính Dòng dõi")) { isLineageSection = true; continue; }
            if (clean.startsWith("✦ Thuộc tính Phụ:")) { isLineageSection = false; isSubStatSection = true; continue; }
            if (clean.startsWith("✦") || clean.startsWith("[Set")) { isLineageSection = false; isSubStatSection = false; }

            if (isLineageSection && clean.startsWith("- ") && !clean.contains("Không có")) lineageLines.add(i);
            if (isSubStatSection && clean.startsWith("- ")) subStatLines.add(i);
        }

        // [SỬA ĐỔI]: Dòng dõi giờ chỉ có 50% cơ hội được nhận Ngôi sao nâng cấp
        if (!lineageLines.isEmpty()) {
            if (rand.nextBoolean()) { // Trả về true/false (50/50)
                int index = lineageLines.get(rand.nextInt(lineageLines.size()));
                boostStatLine(lore, index);
            }
        }

        // Dòng phụ luôn luôn được nhận 1 nâng cấp (Đảm bảo người chơi không bị lỗ hoàn toàn)
        if (!subStatLines.isEmpty()) {
            int index = subStatLines.get(rand.nextInt(subStatLines.size()));
            boostStatLine(lore, index);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ===============================================
    // HÀM ĐỘT PHÁ MỐC BẢO HIỂM (NERF CÒN 30%)
    // ===============================================
    private void boostStatLine(List<String> lore, int index) {
        String line = lore.get(index);
        int starCount = 0;
        for (char c : line.toCharArray()) if (c == '★') starCount++;

        String clean = ChatColor.stripColor(line).replace("★", "").trim();
        String[] parts = clean.split("\\+");
        if (parts.length < 2) return;

        String prefix = line.substring(0, line.indexOf("+") + 1);
        String valueStr = parts[1].replace("%", "").trim();

        try {
            double val = Double.parseDouble(valueStr);
            // YÊU CẦU: Giảm từ tăng 50% (1.5) xuống còn 30% (1.3)
            double newVal = val * 1.3;

            StringBuilder stars = new StringBuilder(" §e");
            for (int i = 0; i <= starCount; i++) stars.append("★");

            lore.set(index, prefix + String.format("%.1f", newVal) + "%" + stars.toString());
        } catch (Exception ignored) {}
    }

    private int countCustomItem(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) count += item.getAmount();
        }
        return count;
    }

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