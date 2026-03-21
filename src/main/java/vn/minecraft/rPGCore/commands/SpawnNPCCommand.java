package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class SpawnNPCCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền dùng lệnh này!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cHDSD: /spawnnpc <bank|weapon|food|general>");
            return true;
        }

        // 1. Triệu hồi Dân làng tại vị trí của Admin
        Villager npc = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);

        // 2. Ép các thuộc tính "Trấn Phái" của NPC

        // npc.setAI(false); // XÓA HOẶC COMMENT DÒNG NÀY ĐI ĐỂ BẬT LẠI NÃO CHO NPC

        // THÊM 2 DÒNG NÀY VÀO ĐỂ ÉP TỐC ĐỘ VỀ 0 VÀ CHỐNG ĐẨY
        npc.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.0);
        npc.setCollidable(false);

        npc.setInvulnerable(true);
        npc.setCustomNameVisible(true);
        npc.setSilent(true);

        // 3. Xóa toàn bộ công thức Trade Vanilla để tránh bị kẹt UI
        java.util.List<org.bukkit.inventory.MerchantRecipe> emptyRecipes = new java.util.ArrayList<>();
        npc.setRecipes(emptyRecipes);

        // 4. Phân loại NPC theo tham số lệnh
        String type = args[0].toLowerCase();
        switch (type) {
            case "bank":
                npc.setCustomName("§b§lNgân Hàng");
                npc.setProfession(Villager.Profession.LIBRARIAN); // Mặc đồ Thủ thư (đeo kính)
                break;
            case "weapon":
                npc.setCustomName("§c§lThương Nhân Vũ Khí");
                npc.setProfession(Villager.Profession.WEAPONSMITH); // Mặc đồ Thợ rèn (đeo tạp dề đen)
                break;
            case "food":
                npc.setCustomName("§a§lThương Nhân Thực Phẩm");
                npc.setProfession(Villager.Profession.BUTCHER); // Mặc đồ Đồ tể (đeo băng đô)
                break;
            default:
                npc.setCustomName("§e§lThương Nhân Tạp Hóa");
                npc.setProfession(Villager.Profession.FARMER); // Mặc đồ Nông dân (đội nón rơm)
                break;
        }

        player.sendMessage("§aĐã triệu hồi NPC [" + npc.getCustomName() + "§a] thành công!");
        return true;
    }
}