package vn.minecraft.rPGCore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.ui.AdminArrowCraftingUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RPGClassAdminCommand implements CommandExecutor {

    private final AdminArrowCraftingUI adminArrowCraftingUI;

    // Các biến phụ trợ cho tính năng Upgrade
    private final List<Integer> SAFE_LEVELS = Arrays.asList(3, 6, 9, 12, 15);
    private final Random rand = new Random();

    public RPGClassAdminCommand(AdminArrowCraftingUI adminArrowCraftingUI) {
        this.adminArrowCraftingUI = adminArrowCraftingUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho Player!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eCác lệnh khả dụng:");
            player.sendMessage("§7- /rpgclassadmin edit archer crafting");
            player.sendMessage("§7- /rpgclassadmin upgrade <cấp_độ_mong_muốn>");
            return true;
        }

        // ==========================================
        // 1. LỆNH CHỈNH SỬA CHẾ TẠO MŨI TÊN
        // ==========================================
        if (args.length >= 3 && args[0].equalsIgnoreCase("edit")
                && args[1].equalsIgnoreCase("archer")
                && args[2].equalsIgnoreCase("crafting")) {

            adminArrowCraftingUI.openAdminGUI(player);
            player.sendMessage("§aĐã mở giao diện chỉnh sửa công thức Mũi Tên!");
            return true;
        }

        // ==========================================
        // 2. LỆNH ADMIN ÉP CẤP ĐỘ TRANG BỊ
        // ==========================================
        if (args[0].equalsIgnoreCase("upgrade")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().isAir() || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
                player.sendMessage("§cHãy cầm một trang bị RPG trên tay chính!");
                return true;
            }

            int currentLevel = getUpgradeLevel(item);
            int targetLevel = currentLevel + 1; // Mặc định là +1 nếu không nhập số

            if (args.length > 1) {
                try { targetLevel = Integer.parseInt(args[1]); }
                catch (Exception e) { player.sendMessage("§cCấp độ phải là một số!"); return true; }
            }

            if (targetLevel > 15) targetLevel = 15;
            if (targetLevel <= currentLevel) {
                player.sendMessage("§cTrang bị đã ở cấp độ " + currentLevel + " hoặc cao hơn mục tiêu.");
                return true;
            }

            // 1. Áp dụng Chỉ số chính
            applyStatsChange(item, currentLevel, targetLevel);

            // 2. Chạy giả lập qua các Mốc bảo hiểm để Buff Dòng phụ / Dòng dõi
            for (int i = currentLevel + 1; i <= targetLevel; i++) {
                if (SAFE_LEVELS.contains(i)) {
                    applyMilestoneBonus(item);
                }
            }

            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            player.sendMessage("§a§l[Admin] §fĐã ép xung trang bị lên " + getLevelColor(targetLevel) + "[+" + targetLevel + "]");
            return true;
        }

        player.sendMessage("§cSai cú pháp! Hãy gõ /rpgclassadmin để xem danh sách lệnh.");
        return true;
    }

    // =========================================================
    // CÁC HÀM XỬ LÝ TOÁN HỌC VÀ LORE (Tương tự như Lò Rèn)
    // =========================================================
    private int getUpgradeLevel(ItemStack item) {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\+(\\d+)\\]$").matcher(name);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private String getLevelColor(int level) {
        if (level >= 1 && level <= 3) return "§a";   // Xanh lá
        if (level >= 4 && level <= 6) return "§9";   // Xanh dương
        if (level >= 7 && level <= 9) return "§5";   // Tím
        if (level >= 10 && level <= 12) return "§c"; // Đỏ
        if (level >= 13 && level <= 15) return "§6"; // Vàng
        return "§e";
    }

    private double getMultiplier(int level) {
        if (level == 0) return 1.0;
        double multi = 1.0;
        multi += Math.min(level, 10) * 0.10; // +10% cho 10 cấp đầu
        if (level > 10) multi += (level - 10) * 0.15; // +15% cho cấp 11 đến 15
        return multi;
    }

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
            else if (clean.startsWith("- Máu tối đa:") || clean.startsWith("- Giáp:")) {
                String[] parts = clean.split(":");
                String prefix = line.substring(0, line.indexOf(":") + 1);
                double oldVal = Double.parseDouble(ChatColor.stripColor(parts[1]).replace("+", "").replace("%", "").trim());
                double newVal = (oldVal / oldMulti) * newMulti;
                lore.set(i, prefix + " §a+" + Math.round(newVal));
            }
        }

        // Xóa sạch các cấp độ cũ
        String name = meta.getDisplayName();
        name = name.replaceAll(" (?:§.)?\\[\\+\\d+\\]", "");

        // Thêm màu sắc và cấp độ mới
        if (newLevel > 0) name = name + " " + getLevelColor(newLevel) + "[+" + newLevel + "]";

        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void applyMilestoneBonus(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        List<Integer> lineageLines = new ArrayList<>();
        List<Integer> subStatLines = new ArrayList<>();
        boolean isLineageSection = false, isSubStatSection = false;

        for (int i = 0; i < lore.size(); i++) {
            String clean = ChatColor.stripColor(lore.get(i)).trim();
            if (clean.startsWith("✦ Thuộc tính Dòng dõi")) { isLineageSection = true; continue; }
            if (clean.startsWith("✦ Thuộc tính Phụ:")) { isLineageSection = false; isSubStatSection = true; continue; }
            if (clean.startsWith("✦") || clean.startsWith("[Set")) { isLineageSection = false; isSubStatSection = false; }

            if (isLineageSection && clean.startsWith("- ") && !clean.contains("Không có")) lineageLines.add(i);
            if (isSubStatSection && clean.startsWith("- ")) subStatLines.add(i);
        }

        // Dòng dõi giờ chỉ có 50% cơ hội nhận Ngôi Sao
        if (!lineageLines.isEmpty() && rand.nextBoolean()) {
            boostStatLine(lore, lineageLines.get(rand.nextInt(lineageLines.size())));
        }

        // Dòng phụ luôn nhận được buff (100%)
        if (!subStatLines.isEmpty()) {
            boostStatLine(lore, subStatLines.get(rand.nextInt(subStatLines.size())));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // Tích lũy Ngôi sao
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
            double newVal = val * 1.5;
            StringBuilder stars = new StringBuilder(" §e");
            for (int i = 0; i <= starCount; i++) stars.append("★");

            lore.set(index, prefix + String.format("%.1f", newVal) + "%" + stars.toString());
        } catch (Exception ignored) {}
    }
}