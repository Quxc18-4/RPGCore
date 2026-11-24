package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.ui.SlotGUI;

public class SlotCommand implements CommandExecutor {
    private final RPGCore plugin;

    public SlotCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi trong game.");
            return true;
        }

        SlotGUI.open(player, plugin);
        return true;
    }
}