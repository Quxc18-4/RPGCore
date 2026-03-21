package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.ui.SkillUIManager;

public class SkillCommand implements CommandExecutor {
    private final SkillUIManager uiManager;

    public SkillCommand(SkillUIManager uiManager) {
        this.uiManager = uiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            // Đã thêm số 0 ở cuối để mặc định mở trang đầu tiên (Trang 0)
            uiManager.openSkillGUI((Player) sender, null, 0);
        }
        return true;
    }
}