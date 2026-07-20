package vn.minecraft.rPGCore.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;

import java.util.UUID;

public class SpawnNPCCommand implements CommandExecutor {

    private final RPGCore plugin;

    // Cập nhật Constructor để lấy plugin
    public SpawnNPCCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền dùng lệnh này!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cHDSD: /spawnnpc <weapon|food|general|fisher|alchemist|mount|trinket|bank>");
            return true;
        }

        String type = args[0].toLowerCase();
        String name;
        String shopPrefix;

        // Phân loại 7 Thương Nhân dựa trên ItemManager
        switch (type) {
            case "weapon":
                name = "§c§lThương Nhân Vũ Khí";
                shopPrefix = "shop_weapon";
                break;
            case "food":
                name = "§a§lThương Nhân Thực Phẩm";
                shopPrefix = "shop_food";
                break;
            case "fisher":
                name = "§b§lThương Nhân Ngư Cụ";
                shopPrefix = "shop_fisher";
                break;
            case "alchemist":
                name = "§d§lThương Nhân Dược Liệu";
                shopPrefix = "shop_alchemist";
                break;
            case "mount":
                name = "§6§lThương Nhân Thú Cưỡi";
                shopPrefix = "shop_mount";
                break;
            case "trinket":
                name = "§5§lThương Nhân Trang Sức";
                shopPrefix = "shop_trinket";
                break;
            case "bank":
                name = "§b§lNgân Hàng Trung Ương";
                shopPrefix = "bank";
                break;
            default:
                name = "§e§lThương Nhân Tạp Hóa";
                shopPrefix = "shop_general";
                break;
        }

        // Tạo ID duy nhất cho NPC và Shop để lưu vào MongoDB
        String shortUUID = UUID.randomUUID().toString().substring(0, 6);
        String npcId = "npc_" + shortUUID;
        String shopId = shopPrefix + "_" + shortUUID;
        Location loc = player.getLocation();

        // Gọi NPCManager để tạo thực thể và LƯU THẲNG XUỐNG MONGODB
        plugin.getNpcManager().createNPC(npcId, name, EntityType.VILLAGER, loc, shopId);

        player.sendMessage("§aĐã triệu hồi và lưu DB thành công: [" + name + "§a]");
        player.sendMessage("§7Mã Shop: §f" + shopId);
        player.sendMessage("§e► Hãy click chuột phải vào NPC để Setup mặt hàng!");
        return true;
    }
}