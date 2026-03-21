package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.ui.SkillUIManager;

public class BindsCommand implements CommandExecutor {
    private final SkillUIManager uiManager;
    public BindsCommand(SkillUIManager uiManager) { this.uiManager = uiManager; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) uiManager.openBindsGUI((Player) sender, null);
        return true;
    }
}
