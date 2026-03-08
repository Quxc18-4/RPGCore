package vn.minecraft.rPGCore.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RepairAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền dùng lệnh này.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/repairadmin kit <số_lần_dùng> §7- Lấy bộ sửa chữa.");
            player.sendMessage("§e/repairadmin stone <1|2|3|4> <số_lượng> §7- Lấy đá mài.");
            return true;
        }

        if (args[0].equalsIgnoreCase("kit")) {
            int uses = args.length > 1 ? Integer.parseInt(args[1]) : 10;
            ItemStack kit = new ItemStack(Material.HEAVY_CORE);
            ItemMeta meta = kit.getItemMeta();
            meta.setDisplayName("§6§lTrạm Sửa Chữa Lưu Động");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click phải để mở giao diện sửa chữa.");
            lore.add("§7- Lượt sử dụng còn lại: §a" + uses + "§8/§a" + uses);
            meta.setLore(lore);
            kit.setItemMeta(meta);
            player.getInventory().addItem(kit);
            player.sendMessage("§aĐã nhận Trạm Sửa Chữa (" + uses + " lượt).");
        }
        else if (args[0].equalsIgnoreCase("stone")) {
            int type = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            int amount = args.length > 2 ? Integer.parseInt(args[2]) : 64;
            ItemStack stone = getStoneItem(type, amount);
            player.getInventory().addItem(stone);
            player.sendMessage("§aĐã nhận Đá mài loại " + type + ".");
        }

        return true;
    }

    public static ItemStack getStoneItem(int type, int amount) {
        ItemStack stone;
        ItemMeta meta;
        switch (type) {
            case 1:
                stone = new ItemStack(Material.LIME_DYE, amount);
                meta = stone.getItemMeta();
                meta.setDisplayName("§aĐá Mài Thường");
                meta.setLore(List.of("§7Dùng để phục hồi §a500 §7độ bền."));
                break;
            case 2:
                stone = new ItemStack(Material.LIGHT_BLUE_DYE, amount);
                meta = stone.getItemMeta();
                meta.setDisplayName("§9Đá Mài Tốt");
                meta.setLore(List.of("§7Dùng để phục hồi §91200 §7độ bền."));
                break;
            case 3:
                stone = new ItemStack(Material.PURPLE_DYE, amount);
                meta = stone.getItemMeta();
                meta.setDisplayName("§5Đá Mài Siêu Phẩm");
                meta.setLore(List.of("§7Dùng để phục hồi §53000 §7độ bền."));
                break;
            case 4:
                stone = new ItemStack(Material.YELLOW_DYE, amount);
                meta = stone.getItemMeta();
                meta.setDisplayName("§6Đá Mài Cực Phẩm");
                meta.setLore(List.of("§7Dùng để phục hồi §6100% §7độ bền."));
                break;
            case 5:
                stone = new ItemStack(Material.NETHER_STAR, amount);
                meta = stone.getItemMeta();
                meta.setDisplayName("§dĐá Nâng Cấp");
                meta.setLore(List.of("§7Nguyên liệu dùng tại Lò Rèn", "§7để nâng cấp chỉ số trang bị."));
                break;
            default:
                stone = new ItemStack(Material.YELLOW_DYE, amount);
                meta = stone.getItemMeta();
                meta.setDisplayName("§6Đá Mài Cực Phẩm");
                meta.setLore(List.of("§7Dùng để phục hồi §6100% §7độ bền."));
                break;
        }
        stone.setItemMeta(meta);
        return stone;
    }
}