package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.ui.AttrGUI;

public class AttrCommand implements CommandExecutor {

    private final AttrGUI attrGUI;

    public AttrCommand(AttrGUI attrGUI) {
        this.attrGUI = attrGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            attrGUI.openGUI((Player) sender);
        }
        return true;
    }
}