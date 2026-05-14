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
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            showStats(player);
            return true;
        }

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return true;

        showProfile(player, rpgPlayer);
        return true;
    }

    private void showProfile(Player player, RPGPlayer rpgPlayer) {
        player.sendMessage("§8§m---------------------------------------");
        player.sendMessage("           §6§lHỒ SƠ NHÂN VẬT");
        player.sendMessage("");
        player.sendMessage(" §7▶ §fNgười chơi: §e" + player.getName());
        player.sendMessage(" §7▶ §fLớp nhân vật: " + ChatColor.translateAlternateColorCodes('&', rpgPlayer.getCurrentClass().getDisplayName()));
        // [MỚI] Lấy và hiển thị thông tin Nghề Phụ (Cấp độ & Tên)
        vn.minecraft.rPGCore.jobs.core.RPGJob currentJob = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(player);
        if (currentJob != null) {
            int jobLevel = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJobLevel(player);
            player.sendMessage(" §7▶ §fNghề nghiệp: §d" + currentJob.getName() + " §8(Cấp " + jobLevel + ")");
        } else {
            player.sendMessage(" §7▶ §fNghề nghiệp: §7Chưa chọn (Dùng /job)");
        }
        player.sendMessage(" §7▶ §fCấp độ: §a" + player.getLevel() + " §8(§7" + (int)rpgPlayer.getCurrentExp() + "§8/§7" + (int)rpgPlayer.getRequiredExp() + " XP§8)");

        player.sendMessage("    " + getProgressBar(rpgPlayer.getCurrentExp(), rpgPlayer.getRequiredExp()));
        player.sendMessage("");
        player.sendMessage(" §6§lTRANG BỊ ĐANG MẶC: §7(Rê chuột để xem)");

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
    // GIAO DIỆN /PROFILE STATS (LỌC THÔNG MINH)
    // ===============================================
    private void showStats(Player player) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        rpgPlayer.updateEquipmentStats();

        player.sendMessage("§8§m---------------------------------------");
        player.sendMessage("         §a§lBẢNG CHỈ SỐ CHI TIẾT");
        player.sendMessage("");

        Map<String, Double> map = rpgPlayer.getTotalStatsMap();
        boolean hasDisplayed = false;

        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            // ==============================================
            // BỘ LỌC CHỈ SỐ THÔNG MINH (ẨN CÁC CHỈ SỐ CHƯA CỘNG)
            // ==============================================
            if (value == 0 && !key.equals("Máu tối đa")) continue; // Bỏ qua giá trị 0 (Luôn hiện Máu)
            if (key.equals("Sát thương vật lý (Nhỏ nhất)") && value == 5.0) continue; // Gốc tay không
            if (key.equals("Sát thương vật lý (Lớn nhất)") && value == 10.0) continue; // Gốc tay không

            // Đánh dấu là có chỉ số để hiển thị
            hasDisplayed = true;

            boolean isPercent = key.contains("Tỉ lệ") || key.contains("Tốc độ") ||
                    key.contains("Giảm") || key.contains("(%)") ||
                    key.equals("Nhận Kinh nghiệm") || key.equals("Hồi Năng lượng") ||
                    key.equals("Sát thương chí mạng");

            String color = "§a"; // Xanh lá nếu 0 đến 100
            if (value < 0) color = "§c"; // Đỏ nếu âm
            else if (value > 200) color = "§5"; // Tím nếu trên 200
            else if (value > 100) color = "§e"; // Vàng nếu 101-200

            String prefix = (value > 0 && !key.contains("Nhỏ nhất") && !key.contains("Lớn nhất") && !key.equals("Máu tối đa") && !key.equals("Giáp")) ? "+" : "";
            String suffix = isPercent ? "%" : "";

            player.sendMessage(" §7▶ " + key + ": " + color + prefix + String.format("%.1f", value) + suffix);
        }

        if (!hasDisplayed) {
            player.sendMessage(" §7- Chưa có chỉ số nào được gia tăng.");
        }

        player.sendMessage("");
        player.sendMessage("§8§m---------------------------------------");
    }

    private Component getHoverableItemComponent(String prefix, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Component.text(prefix + "§8[Trống]");
        }

        String displayName;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            displayName = "§b[" + item.getItemMeta().getDisplayName() + "§b]";
        } else {
            displayName = "§f[" + item.getType().name().replace("_", " ").toLowerCase() + "]";
        }

        return Component.text(prefix)
                .append(Component.text(displayName).hoverEvent(item.asHoverEvent()));
    }

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