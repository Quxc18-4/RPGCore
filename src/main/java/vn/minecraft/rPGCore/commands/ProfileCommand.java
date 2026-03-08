package vn.minecraft.rPGCore.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Map;

public class ProfileCommand implements CommandExecutor {

    private final PlayerManager playerManager;

    public ProfileCommand(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;

        // Xử lý nhánh: /profile stats (Xem tổng chỉ số)
        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            showStats(player);
            return true;
        }

        // Xử lý nhánh mặc định: /profile (Xem thông tin chung)
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return true;

        showProfile(player, rpgPlayer);
        return true;
    }

    // ===============================================
    // GIAO DIỆN /PROFILE CƠ BẢN (CÓ HOVER ITEM)
    // ===============================================
    private void showProfile(Player player, RPGPlayer rpgPlayer) {
        player.sendMessage("§8§m---------------------------------------");
        player.sendMessage("           §6§lHỒ SƠ NHÂN VẬT");
        player.sendMessage("");
        player.sendMessage(" §7▶ §fNgười chơi: §e" + player.getName());
        player.sendMessage(" §7▶ §fLớp nhân vật: " + ChatColor.translateAlternateColorCodes('&', rpgPlayer.getCurrentClass().getDisplayName()));
        player.sendMessage(" §7▶ §fCấp độ: §a" + player.getLevel() + " §8(§7" + (int)rpgPlayer.getCurrentExp() + "§8/§7" + (int)rpgPlayer.getRequiredExp() + " XP§8)");

        player.sendMessage("    " + getProgressBar(rpgPlayer.getCurrentExp(), rpgPlayer.getRequiredExp()));
        player.sendMessage("");
        player.sendMessage(" §6§lTRANG BỊ ĐANG MẶC: §7(Rê chuột để xem)");

        // Dùng Component thay vì String để tạo tính năng Hover
        player.sendMessage(getHoverableItemComponent(" §e⛨ §fMũ: ", player.getInventory().getHelmet()));
        player.sendMessage(getHoverableItemComponent(" §e⛨ §fGiáp: ", player.getInventory().getChestplate()));
        player.sendMessage(getHoverableItemComponent(" §e⛨ §fQuần: ", player.getInventory().getLeggings()));
        player.sendMessage(getHoverableItemComponent(" §e⛨ §fGiày: ", player.getInventory().getBoots()));
        player.sendMessage("");
        player.sendMessage(" §6§lVŨ KHÍ CHÍNH:");
        player.sendMessage(getHoverableItemComponent(" §c⚔ §fCầm tay: ", player.getInventory().getItemInMainHand()));
        player.sendMessage("");
        player.sendMessage(" §7§oGõ §f/profile stats §7§ođể xem tổng chỉ số chi tiết!");
        player.sendMessage("§8§m---------------------------------------");
    }

    // ===============================================
    // GIAO DIỆN /PROFILE STATS (TỔNG CHỈ SỐ MỚI)
    // ===============================================
    private void showStats(Player player) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // Ép hệ thống quét lại toàn bộ đồ trên người trước khi in ra
        rpgPlayer.updateEquipmentStats();
        RPGPlayer.PlayerStats stats = rpgPlayer.getStats();

        player.sendMessage("§8§m---------------------------------------");
        player.sendMessage("         §a§lBẢNG CHỈ SỐ CHI TIẾT");
        player.sendMessage("");

        Map<String, Double> map = new java.util.LinkedHashMap<>();

        if (stats.minPhysicalDamage > 0) {
            map.put("Sát thương vật lý (Nhỏ nhất)", stats.minPhysicalDamage);
            map.put("Sát thương vật lý (Lớn nhất)", stats.maxPhysicalDamage);
        }
        if (stats.minMagicalDamage > 0) {
            map.put("Sát thương phép thuật (Nhỏ nhất)", stats.minMagicalDamage);
            map.put("Sát thương phép thuật (Lớn nhất)", stats.maxMagicalDamage);
        }

        if (stats.maxHealth > 0) map.put("Máu tối đa", stats.maxHealth);
        if (stats.hpMultiplier > 1.0) map.put("Máu tối đa (%)", (stats.hpMultiplier - 1.0) * 100);

        if (stats.armor > 0) map.put("Giáp", stats.armor);
        if (stats.armorMultiplier > 1.0) map.put("Tổng Giáp (%)", (stats.armorMultiplier - 1.0) * 100);

        map.put("Tỉ lệ chí mạng", stats.critRate);
        map.put("Sát thương chí mạng", stats.critDamage);

        if (stats.armorPen > 0) map.put("Tỉ lệ xuyên giáp", stats.armorPen);
        if (stats.lifeSteal > 0) map.put("Tỉ lệ hút máu", stats.lifeSteal);
        if (stats.dodgeRate > 0) map.put("Tỉ lệ né tránh", stats.dodgeRate);
        if (stats.blockRate > 0) map.put("Tỉ lệ đỡ đòn", stats.blockRate);
        if (stats.regenRate > 0) map.put("Tỉ lệ hồi phục", stats.regenRate);
        if (stats.attackSpeed > 0) map.put("Tốc độ đánh", stats.attackSpeed);
        if (stats.cooldownReduction > 0) map.put("Giảm hồi chiêu", stats.cooldownReduction);
        if (stats.moveSpeed > 0) map.put("Tốc độ di chuyển", stats.moveSpeed);

        // Thêm các chỉ số của Dòng dõi
        if (stats.expBonus > 0) map.put("Nhận Kinh nghiệm", stats.expBonus);
        if (stats.manaRegen > 0) map.put("Hồi Năng lượng", stats.manaRegen);
        if (stats.dropRate > 0) map.put("Tỉ lệ rớt đồ", stats.dropRate);

        if (map.isEmpty()) {
            player.sendMessage(" §7- Chưa có chỉ số nào từ trang bị.");
        } else {
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                String key = entry.getKey();
                double value = entry.getValue();

                String suffix = (key.contains("Tỉ lệ") || key.contains("Tốc độ") || key.contains("Giảm") || key.contains("(%)") || key.equals("Nhận Kinh nghiệm") || key.equals("Hồi Năng lượng")) ? "%" : "";

                String color = "§e";
                if (key.equals("Giảm hồi chiêu") && value >= 40.0) color = "§c§l";
                if (key.equals("Tỉ lệ chí mạng") && value >= 100.0) color = "§c§l";
                if (key.equals("Tỉ lệ xuyên giáp") && value >= 80.0) color = "§c§l";
                if (key.equals("Tỉ lệ hút máu") && value >= 50.0) color = "§c§l";
                if (key.equals("Tốc độ đánh") && value >= 100.0) color = "§c§l";
                if (key.equals("Tỉ lệ đỡ đòn") && value >= 50.0) color = "§c§l";
                if (key.equals("Tỉ lệ né tránh") && value >= 50.0) color = "§c§l";

                String prefix = key.contains("Nhỏ nhất") || key.contains("Lớn nhất") ? "" : "+";

                player.sendMessage(" §7▶ " + key + ": " + color + prefix + String.format("%.1f", value) + suffix);
            }
        }
        player.sendMessage("");
        player.sendMessage("§8§m---------------------------------------");
    }

    // ===============================================
    // CÁC HÀM HỖ TRỢ (HELPERS)
    // ===============================================

    // Hàm tạo Component Text có tính năng Hover (Thay thế cho getItemName cũ)
    private Component getHoverableItemComponent(String prefix, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Component.text(prefix + "§8[Trống]");
        }

        String displayName;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // Giữ nguyên format ngoặc vuông màu xanh của bạn
            displayName = "§b[" + item.getItemMeta().getDisplayName() + "§b]";
        } else {
            displayName = "§f[" + item.getType().name().replace("_", " ").toLowerCase() + "]";
        }

        // Trả về chuỗi Prefix nối với Tên vật phẩm (có cài sự kiện HoverEvent)
        return Component.text(prefix)
                .append(Component.text(displayName).hoverEvent(item.asHoverEvent()));
    }

    // Hàm vẽ thanh tiến trình EXP (Giữ nguyên của bạn)
    private String getProgressBar(double current, double max) {
        int barSize = 20;
        int completed = (int) ((current / max) * barSize);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < barSize; i++) {
            if (i < completed) bar.append("§a■");
            else bar.append("§7■");
        }
        bar.append("§8]");
        return bar.toString();
    }
}