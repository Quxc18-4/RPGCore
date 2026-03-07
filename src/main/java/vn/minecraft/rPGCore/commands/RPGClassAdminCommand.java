package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.ui.AdminArrowCraftingUI;

public class RPGClassAdminCommand implements CommandExecutor {

    private final AdminArrowCraftingUI adminArrowCraftingUI;

    public RPGClassAdminCommand(AdminArrowCraftingUI adminArrowCraftingUI) {
        this.adminArrowCraftingUI = adminArrowCraftingUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho Player!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        // Kiểm tra cú pháp: /rpgclassadmin edit archer crafting
        if (args.length >= 3 && args[0].equalsIgnoreCase("edit")
                && args[1].equalsIgnoreCase("archer")
                && args[2].equalsIgnoreCase("crafting")) {

            adminArrowCraftingUI.openAdminGUI(player);
            player.sendMessage("§aĐã mở giao diện chỉnh sửa công thức Mũi Tên!");
            return true;
        }

        player.sendMessage("§cSử dụng: /rpgclassadmin edit archer crafting");
        return true;
    }
}