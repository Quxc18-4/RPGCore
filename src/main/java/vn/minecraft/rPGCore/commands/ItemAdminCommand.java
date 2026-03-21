package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.List;

public class ItemAdminCommand implements CommandExecutor, TabCompleter {

    private final ItemManager ingredientManager;

    public ItemAdminCommand(ItemManager ingredientManager) {
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
        // LỆNH DROP (Tích hợp chuẩn với MythicMobs + Hỗ trợ Scavenger)
        // Cú pháp trong MM: Command{c="ig drop <target.name> food_beef 50.0 2"} ~onDeath
        // ===========================================
        if (args[0].equalsIgnoreCase("drop")) {
            // 1. Kiểm tra đủ số lượng tham số tối thiểu
            if (args.length < 5) {
                sender.sendMessage("§cSai cú pháp! Dùng: /ig drop <người_chơi> <id> <tỉ_lệ_%> <số_lượng> [x] [y] [z] [world]");
                return true;
            }

            // 2. Lấy người chơi (Bỏ qua êm ái nếu người chơi offline, tránh rác console)
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) return true;

            String id = args[2];
            double baseChance;
            int amount;

            // 3. Chống lỗi (Try-Catch) khi ép kiểu dữ liệu
            try {
                baseChance = Double.parseDouble(args[3]);
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cLỗi: Tỉ lệ hoặc số lượng không phải là số hợp lệ!");
                return true;
            }

            double finalChance = baseChance;

            // 4. Lấy dữ liệu RPG và tính toán Buff
            vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());

            if (rpgPlayer != null) {
                // - Tính Tỉ lệ rớt đồ tổng
                double dropBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tỉ lệ rớt đồ", 0.0);
                finalChance = baseChance * (1.0 + (dropBonus / 100.0));

                // - Kiểm tra Nội tại Set 4 SCAVENGER (Bọc try-catch để an toàn tuyệt đối)
                try {
                    // Chú ý: Đảm bảo rpgPlayer.getStats() và setCounts của bạn đã được khởi tạo
                    if (rpgPlayer.getStats() != null && rpgPlayer.getStats().setCounts != null) {
                        int scavengerCount = rpgPlayer.getStats().setCounts.getOrDefault("scavenger", 0);

                        if (scavengerCount >= 4) {
                            // Theo mô tả của bạn ở file Equipment, Scavenger 4 món có 10% cơ hội x2
                            if (Math.random() * 100.0 <= 10.0) {
                                amount *= 2;
                                target.sendMessage("§6§l[SCAVENGER] §eKích hoạt Nội tại: x2 Số lượng vật phẩm!");
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Nếu hệ thống Set chưa hoàn thiện thì sẽ âm thầm bỏ qua, không gây sập lệnh
                }
            }

            // 5. Quay xổ số quyết định rơi đồ
            // (Nếu finalChance vượt quá 100% thì chắc chắn rơi luôn không cần random)
            if (finalChance >= 100.0 || (Math.random() * 100.0) <= finalChance) {

                org.bukkit.inventory.ItemStack item = ingredientManager.buildItem(id, amount);

                if (item != null) {
                    org.bukkit.Location dropLoc = target.getLocation(); // Mặc định là chân người chơi

                    // 6. Xử lý tọa độ rớt đồ ưu tiên (Từ xác quái MythicMobs)
                    if (args.length >= 9) {
                        try {
                            double x = Double.parseDouble(args[5]);
                            double y = Double.parseDouble(args[6]);
                            double z = Double.parseDouble(args[7]);
                            org.bukkit.World world = org.bukkit.Bukkit.getWorld(args[8]);

                            if (world != null) {
                                dropLoc = new org.bukkit.Location(world, x, y, z);
                            }
                        } catch (Exception e) {
                            // Nếu truyền sai tọa độ, nó sẽ rớt tạm ở chân người chơi thay vì không rớt
                        }
                    }

                    // 7. Thực hiện rớt đồ ra đất
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item);
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