package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import static vn.minecraft.rPGCore.managers.StatusEffectManager.applyAntiHeal;

public class RPGAdminCommand implements CommandExecutor {

    private final PlayerManager playerManager;

    public RPGAdminCommand(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Chỉ cho phép OP hoặc người có quyền quản trị sử dụng
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Không tìm thấy người chơi: " + args[1]);
            return true;
        }

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(target.getUniqueId());
        if (rpgPlayer == null) return true;

        switch (subCommand) {
            case "giveexp":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Sử dụng: /rpgadmin giveexp <tên> <lượng>");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    rpgPlayer.addExp(amount);
                    sender.sendMessage(ChatColor.GREEN + "Đã cộng " + amount + " EXP cho " + target.getName());
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Lượng EXP phải là một con số!");
                }
                break;

            case "levelup":
                // Logic: Cộng đúng bằng lượng getRequiredExp() của level hiện tại
                // Vì hàm addExp() đã có vòng lặp while và trừ dần dư nợ,
                // nên cộng đúng 1 cục 'Required' sẽ đẩy player lên lv sau và giữ nguyên Exp dư.
                double needed = rpgPlayer.getRequiredExp();
                rpgPlayer.addExp(needed);

                sender.sendMessage(ChatColor.AQUA + "Đã tăng 1 cấp cho " + target.getName() + " (Giữ nguyên tiến độ).");
                break;
            case "antiheal":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Sử dụng: /rpgadmin antiheal <player> <ticks>");
                    return true;
                }
                try {
                    int durationTicks = Integer.parseInt(args[2]);
                    // Kích hoạt hàm xử lý vết thương sâu trực tiếp lên mục tiêu
                    applyAntiHeal(target, durationTicks);
                    sender.sendMessage(ChatColor.GREEN + "Đã áp dụng Vết thương sâu lên " + target.getName() + " trong " + durationTicks + " ticks.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Thời gian ticks truyền vào phải là một số nguyên!");
                }
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== RPG Admin Commands ===");
        sender.sendMessage(ChatColor.WHITE + "/rpgadmin giveexp <player> <amount>");
        sender.sendMessage(ChatColor.WHITE + "/rpgadmin levelup <player>");
        sender.sendMessage(ChatColor.WHITE + "/rpgadmin antiheal <player> <ticks>");
    }
}