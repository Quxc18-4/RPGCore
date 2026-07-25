package vn.minecraft.rPGCore.playercommunication.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;

public class TradeCommand implements CommandExecutor {

    private final RPGCore plugin;

    public TradeCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới có thể giao dịch!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§c§l[!] §cSử dụng: /trade <tên_người_chơi> | accept | deny");
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("accept")) {
            plugin.getTradeManager().acceptTrade(player);
            return true;
        }

        if (action.equals("deny")) {
            plugin.getTradeManager().denyTrade(player);
            return true;
        }

        // Xử lý gửi lời mời
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c§l[!] §cKhông tìm thấy người chơi này!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§c§l[!] §cThôi nào, tự giao dịch với chính mình làm gì?");
            return true;
        }

        plugin.getTradeManager().sendTradeInvite(player, target);
        return true;
    }
}