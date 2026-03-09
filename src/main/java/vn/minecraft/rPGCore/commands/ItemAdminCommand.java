package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.IngredientManager;

import java.util.ArrayList;
import java.util.List;

public class ItemAdminCommand implements CommandExecutor, TabCompleter {

    private final IngredientManager ingredientManager;

    public ItemAdminCommand(IngredientManager ingredientManager) {
        this.ingredientManager = ingredientManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rpgcore.admin")) {
            sender.sendMessage("§cBạn không có quyền!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e/ig give <người_chơi> <id_vật_phẩm> [số_lượng]");
            sender.sendMessage("§e/ig drop <người_chơi> <id_vật_phẩm> <tỉ_lệ_%> <số_lượng> §7(Dành cho MythicMobs)");
            return true;
        }

        // ===========================================
        // LỆNH GIVE (Cho NPC hoặc Admin lấy trực tiếp)
        // ===========================================
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage("§cSử dụng: /ig give <player> <id> [số_lượng]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) return true;

            String id = args[2];
            int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;

            ItemStack item = ingredientManager.buildItem(id, amount);
            if (item != null) {
                target.getInventory().addItem(item);
                sender.sendMessage("§aĐã gửi " + amount + "x " + item.getItemMeta().getDisplayName() + " §acho " + target.getName());
            } else {
                sender.sendMessage("§cID vật phẩm '" + id + "' không tồn tại trong hệ thống!");
            }
            return true;
        }

        // ===========================================
        // LỆNH DROP (Tích hợp chuẩn với MythicMobs)
        // Cú pháp trong MM: Command{c="ig drop <target.name> food_beef 50.0 2"} ~onDeath
        // ===========================================
        if (args[0].equalsIgnoreCase("drop")) {
            if (args.length < 5) return true;

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) return true; // Bỏ qua nếu target không phải Player (ví dụ: bị giết bởi lửa)

            String id = args[2];
            double chance = Double.parseDouble(args[3]);
            int amount = Integer.parseInt(args[4]);

            // Quay xổ số tỉ lệ rớt
            if (Math.random() * 100.0 <= chance) {
                ItemStack item = ingredientManager.buildItem(id, amount);
                if (item != null) {
                    Location loc = target.getLocation();
                    // Vứt item ra ngay vị trí của người chơi
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
            return true;
        }

        return true;
    }

    // Auto-complete (Bấm Tab để tự hiện danh sách ID)
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) completions.addAll(List.of("give", "drop"));
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("drop"))) {
            completions.addAll(ingredientManager.getAllIds());
        }

        String currentWord = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String s : completions) {
            if (s.toLowerCase().startsWith(currentWord)) filtered.add(s);
        }
        return filtered;
    }
}