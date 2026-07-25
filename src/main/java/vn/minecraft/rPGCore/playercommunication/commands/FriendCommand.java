package vn.minecraft.rPGCore.playercommunication.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;

public class FriendCommand implements CommandExecutor {

    private final RPGCore plugin;

    public FriendCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        // Xử lý lệnh: /friend list
        if (action.equals("list")) {
            plugin.getFriendManager().listFriends(player);
            return true;
        }

        if (args.length < 2) {
            sendHelp(player);
            return true;
        }

        String targetName = args[1];
        // Lấy OfflinePlayer để thao tác DB ngay cả khi người kia đang Offline
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[!] §cBạn không thể tự kết bạn với chính mình!");
            return true;
        }

        switch (action) {
            case "add":
            case "invite":
                plugin.getFriendManager().sendFriendRequest(player, target);
                break;
            case "accept":
                plugin.getFriendManager().acceptFriendRequest(player, target);
                break;
            case "remove":
            case "delete":
                plugin.getFriendManager().removeFriend(player, target);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e§lHỆ THỐNG BẠN BÈ:");
        player.sendMessage("§a/friend add <tên> §7- Gửi lời mời kết bạn");
        player.sendMessage("§a/friend accept <tên> §7- Đồng ý kết bạn");
        player.sendMessage("§a/friend remove <tên> §7- Hủy kết bạn");
        player.sendMessage("§a/friend list §7- Xem danh sách bạn bè");
    }
}