package vn.minecraft.rPGCore.jobs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JobCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;

        // Nếu chỉ gõ /job -> Mở menu chọn nghề
        if (args.length == 0) {
            JobSelectionGUI.openGUI(player);
            return true;
        }

        // Xử lý các lệnh phụ: /job menu
        if (args[0].equalsIgnoreCase("menu")) {
            //JobSelectionGUI.openStatusMenu(player);
            return true;
        }

        player.sendMessage("§c§l(!) §cThao tác không hợp lệ. Dùng /job hoặc /job menu.");
        return true;
    }

    // Gợi ý khi bấm Tab (Remind)
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("menu");

            String current = args[0].toLowerCase();
            return subCommands.stream()
                    .filter(s -> s.startsWith(current))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}