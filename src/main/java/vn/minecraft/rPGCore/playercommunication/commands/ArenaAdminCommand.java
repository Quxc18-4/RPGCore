package vn.minecraft.rPGCore.playercommunication.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;

public class ArenaAdminCommand implements CommandExecutor {
    private final RPGCore plugin;

    public ArenaAdminCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin.arena")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§e§lQUẢN LÝ ĐẤU TRƯỜNG:");
            player.sendMessage("§a/arena create <tên> §7- Tạo map mới");
            player.sendMessage("§a/arena delete <tên> §7- Xóa map");
            player.sendMessage("§a/arena setspawn1 <tên> §7- Đặt vị trí P1 (Tại chỗ đang đứng)");
            player.sendMessage("§a/arena setspawn2 <tên> §7- Đặt vị trí P2 (Tại chỗ đang đứng)");
            return true;
        }

        String action = args[0].toLowerCase();
        String arenaName = args[1];
        // Lưu ý: Đảm bảo bác đã có plugin.getArenaManager() trong RPGCore
        vn.minecraft.rPGCore.playercommunication.managers.ArenaManager arenaManager = plugin.getArenaManager();

        switch (action) {
            case "create":
                if (arenaManager.createArena(arenaName)) player.sendMessage("§aĐã tạo map: §e" + arenaName);
                else player.sendMessage("§cĐấu trường đã tồn tại!");
                break;
            case "delete":
                if (arenaManager.deleteArena(arenaName)) player.sendMessage("§aĐã xóa map: §e" + arenaName);
                else player.sendMessage("§cKhông tìm thấy đấu trường này!");
                break;
            case "setspawn1":
                if (arenaManager.setArenaSpawn(arenaName, 1, player.getLocation()))
                    player.sendMessage("§aĐã lưu tọa độ Spawn 1 cho map §e" + arenaName);
                else player.sendMessage("§cKhông tìm thấy đấu trường này!");
                break;
            case "setspawn2":
                if (arenaManager.setArenaSpawn(arenaName, 2, player.getLocation()))
                    player.sendMessage("§aĐã lưu tọa độ Spawn 2 cho map §e" + arenaName);
                else player.sendMessage("§cKhông tìm thấy đấu trường này!");
                break;
            default:
                player.sendMessage("§cSai cú pháp!");
                break;
        }
        return true;
    }
}