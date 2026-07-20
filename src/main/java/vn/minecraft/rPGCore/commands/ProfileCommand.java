package vn.minecraft.rPGCore.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

        // Nếu có tham số truyền vào
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("stats")) {
                // Lệnh: /profile stats <tên>
                if (args.length > 1) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage("§cNgười chơi này không online!");
                        return true;
                    }
                    RPGPlayer rpgTarget = playerManager.getRPGPlayer(target.getUniqueId());
                    if (rpgTarget != null) showStats(player, target, rpgTarget);
                }
                // Lệnh: /profile stats
                else {
                    RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
                    if (rpgPlayer != null) showStats(player, player, rpgPlayer);
                }
                return true;
            } else {
                // Lệnh: /profile <tên>
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage("§cNgười chơi này không online!");
                    return true;
                }
                RPGPlayer rpgTarget = playerManager.getRPGPlayer(target.getUniqueId());
                if (rpgTarget == null) {
                    player.sendMessage("§cDữ liệu của người chơi này chưa được tải!");
                    return true;
                }
                showProfile(player, target, rpgTarget);
                return true;
            }
        }

        // Lệnh gốc: /profile
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return true;

        showProfile(player, player, rpgPlayer);
        return true;
    }

    // [ĐÃ SỬA]: Thêm đối số viewer (người xem) và target (mục tiêu bị xem)
    private void showProfile(Player viewer, Player target, RPGPlayer rpgTarget) {
        viewer.sendMessage("§8§m---------------------------------------");
        viewer.sendMessage("           §6§lHỒ SƠ NHÂN VẬT");
        viewer.sendMessage("");
        viewer.sendMessage(" §7▶ §fNgười chơi: §e" + target.getName());
        viewer.sendMessage(" §7▶ §fLớp nhân vật: " + ChatColor.translateAlternateColorCodes('&', rpgTarget.getCurrentClass().getDisplayName()));

        vn.minecraft.rPGCore.jobs.core.RPGJob currentJob = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(target);
        if (currentJob != null) {
            int jobLevel = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJobLevel(target);
            viewer.sendMessage(" §7▶ §fNghề nghiệp: §d" + currentJob.getName() + " §8(Cấp " + jobLevel + ")");
        } else {
            viewer.sendMessage(" §7▶ §fNghề nghiệp: §7Chưa chọn");
        }
        viewer.sendMessage(" §7▶ §fCấp độ: §a" + target.getLevel() + " §8(§7" + (int)rpgTarget.getCurrentExp() + "§8/§7" + (int)rpgTarget.getRequiredExp() + " XP§8)");

        viewer.sendMessage("    " + getProgressBar(rpgTarget.getCurrentExp(), rpgTarget.getRequiredExp()));
        viewer.sendMessage("");
        viewer.sendMessage(" §6§lTRANG BỊ ĐANG MẶC: §7(Rê chuột để xem)");

        // Hiển thị kho đồ của Target, gởi cho Viewer xem
        viewer.sendMessage(getHoverableItemComponent(" §e⛨ §fMũ: ", target.getInventory().getHelmet()));
        viewer.sendMessage(getHoverableItemComponent(" §e⛨ §fGiáp: ", target.getInventory().getChestplate()));
        viewer.sendMessage(getHoverableItemComponent(" §e⛨ §fQuần: ", target.getInventory().getLeggings()));
        viewer.sendMessage(getHoverableItemComponent(" §e⛨ §fGiày: ", target.getInventory().getBoots()));
        viewer.sendMessage("");
        viewer.sendMessage(" §6§lVŨ KHÍ CHÍNH:");
        viewer.sendMessage(getHoverableItemComponent(" §c⚔ §fCầm tay: ", target.getInventory().getItemInMainHand()));
        viewer.sendMessage("");
        if (viewer.equals(target)) {
            viewer.sendMessage(" §7§oGõ §f/profile stats §7§ođể xem tổng chỉ số chi tiết!");
        } else {
            viewer.sendMessage(" §7§oGõ §f/profile stats " + target.getName() + " §7§ođể soi chỉ số!");
        }
        viewer.sendMessage("§8§m---------------------------------------");
    }

    private void showStats(Player viewer, Player target, RPGPlayer rpgTarget) {
        rpgTarget.updateEquipmentStats();

        viewer.sendMessage("§8§m---------------------------------------");
        viewer.sendMessage("         §a§lBẢNG CHỈ SỐ CỦA " + target.getName().toUpperCase());
        viewer.sendMessage("");

        Map<String, Double> map = rpgTarget.getTotalStatsMap();
        boolean hasDisplayed = false;

        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            if (value == 0 && !key.equals("Máu tối đa")) continue;
            if (key.equals("Sát thương vật lý (Nhỏ nhất)") && value == 5.0) continue;
            if (key.equals("Sát thương vật lý (Lớn nhất)") && value == 10.0) continue;

            hasDisplayed = true;

            boolean isPercent = key.contains("Tỉ lệ") || key.contains("Tốc độ") ||
                    key.contains("Giảm") || key.contains("(%)") ||
                    key.equals("Nhận Kinh nghiệm") || key.equals("Hồi Năng lượng") ||
                    key.equals("Sát thương chí mạng");

            String color = "§a";
            if (value < 0) color = "§c";
            else if (value > 200) color = "§5";
            else if (value > 100) color = "§e";

            String prefix = (value > 0 && !key.contains("Nhỏ nhất") && !key.contains("Lớn nhất") && !key.equals("Máu tối đa") && !key.equals("Giáp")) ? "+" : "";
            String suffix = isPercent ? "%" : "";

            viewer.sendMessage(" §7▶ " + key + ": " + color + prefix + String.format("%.1f", value) + suffix);
        }

        if (!hasDisplayed) {
            viewer.sendMessage(" §7- Chưa có chỉ số nào được gia tăng.");
        }

        viewer.sendMessage("");
        viewer.sendMessage("§8§m---------------------------------------");
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