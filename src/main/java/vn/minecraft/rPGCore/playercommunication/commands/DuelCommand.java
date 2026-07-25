package vn.minecraft.rPGCore.playercommunication.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;

public class DuelCommand implements CommandExecutor {

    private final RPGCore plugin;

    public DuelCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§e§l[Đấu Trường] §fCách dùng: §a/duel <tên_người_chơi> §fhoặc §a/duel accept");
            return true;
        }

        String actionOrTarget = args[0];

        // Xử lý khi người chơi gõ /duel accept
        if (actionOrTarget.equalsIgnoreCase("accept")) {
            plugin.getDuelManager().acceptDuel(player);
            return true;
        }

        // Nếu không phải "accept", hệ thống coi đó là Tên người chơi để gửi lời mời
        Player target = Bukkit.getPlayer(actionOrTarget);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c§l[!] §cKhông tìm thấy người chơi này hoặc họ đang Offline!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§c§l[!] §cBạn không thể tự thách đấu với chính mình!");
            return true;
        }

        // Chuyển lệnh xuống Manager xử lý
        plugin.getDuelManager().sendDuelRequest(player, target);
        return true;
    }
}