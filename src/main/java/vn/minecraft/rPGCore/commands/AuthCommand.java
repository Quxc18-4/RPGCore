package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.managers.AuthManager;

public class AuthCommand implements CommandExecutor {
    private final AuthManager authManager;

    public AuthCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("login")) {
            if (args.length < 1) {
                player.sendMessage("§cHDSD: /login <mật khẩu>");
                return true;
            }
            authManager.login(player, args[0]);
            return true;
        }

        if (command.getName().equalsIgnoreCase("register")) {
            if (args.length < 2) {
                player.sendMessage("§cHDSD: /register <mật khẩu> <email@haha.com>");
                return true;
            }
            authManager.register(player, args[0], args[1]);
            return true;
        }

        if (command.getName().equalsIgnoreCase("forgotpw")) {
            if (args.length < 1) {
                player.sendMessage("§cHDSD: /forgotpw <email đăng ký>");
                return true;
            }
            authManager.forgotPassword(player, args[0]);
            return true;
        }
        return false;
    }
}