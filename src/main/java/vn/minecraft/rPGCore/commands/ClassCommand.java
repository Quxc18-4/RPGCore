package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.ui.ClassGUI;

import java.util.ArrayList;
import java.util.List;

public class ClassCommand implements CommandExecutor {

    private final ClassManager classManager;
    private final ClassGUI classGUI;

    // Truyền ClassManager và ClassGUI vào
    public ClassCommand(ClassManager classManager, ClassGUI classGUI) {
        this.classManager = classManager;
        this.classGUI = classGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        // Lấy danh sách tất cả các class đã đọc từ classes.yml
        List<RPGClass> availableClasses = new ArrayList<>();
        for (RPGClass rpgClass : classManager.getClasses().values()) {
            // Chỉ thêm vào list hiển thị nếu ID của class KHÔNG phải là "commoner"
            if (!rpgClass.getId().equalsIgnoreCase("commoner")) {
                availableClasses.add(rpgClass);
            }
        }

        // Mở cái rương 36 ô (4x9) đã được căn giữa các item
        classGUI.openGUI(player, availableClasses);

        return true;
    }
}