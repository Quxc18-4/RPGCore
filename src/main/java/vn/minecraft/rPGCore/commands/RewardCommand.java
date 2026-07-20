package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.RewardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RewardCommand implements CommandExecutor, TabCompleter {
    private final RPGCore plugin;

    public RewardCommand(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        RewardManager rm = plugin.getRewardManager();

        // ==========================================
        // LỆNH ADMIN (/adminkit)
        // ==========================================
        if (command.getName().equalsIgnoreCase("adminkit")) {
            if (!player.hasPermission("rpgcore.admin")) {
                player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage("§e=== ADMIN KIT ===");
                player.sendMessage("§e/adminkit create <tên> <cooldown_giây> §7- Tạo kit mới");
                player.sendMessage("§e/adminkit time <tên> <giây> §7- Chỉnh thời gian chờ");
                player.sendMessage("§e/adminkit edit <tên> §7- Chỉnh đồ cho Kit");
                player.sendMessage("§e/adminkit edit <1-31> §7- Chỉnh quà Điểm danh");
                player.sendMessage("§e/adminkit list §7- Xem danh sách");
                return true;
            }

            String sub = args[0].toLowerCase();

            if (sub.equals("create") && args.length >= 2) {
                long cooldown = args.length == 3 ? Long.parseLong(args[2]) : 86400L; // Mặc định 24h
                if (rm.createKit(args[1], cooldown)) {
                    player.sendMessage("§aTạo thành công Kit: §e" + args[1] + " §a(Hồi chiêu: " + cooldown + "s)");
                } else {
                    player.sendMessage("§cKit này đã tồn tại!");
                }
                return true;
            }

            if (sub.equals("time") && args.length == 3) {
                long cooldown = Long.parseLong(args[2]);
                if (rm.setKitCooldown(args[1], cooldown)) {
                    player.sendMessage("§aĐã đổi thời gian Kit §e" + args[1] + " §athành " + cooldown + " giây.");
                } else {
                    player.sendMessage("§cKhông tìm thấy Kit này!");
                }
                return true;
            }

            if (sub.equals("list")) {
                Set<String> kits = rm.getKitNames();
                player.sendMessage("§aDanh sách Kits hiện có: §f" + (kits.isEmpty() ? "Trống" : String.join(", ", kits)));
                return true;
            }

            if (sub.equals("edit") && args.length == 2) {
                String target = args[1];
                if (target.matches("\\d+")) {
                    int day = Integer.parseInt(target);
                    if (day >= 1 && day <= 31) player.openInventory(rm.getMonthlyAdminGUI(day));
                    else player.sendMessage("§cSố ngày phải từ 1 đến 31!");
                    return true;
                } else {
                    if (!rm.getKitNames().contains(target.toLowerCase())) {
                        player.sendMessage("§cKit này chưa được tạo!");
                        return true;
                    }

                    Inventory inv = Bukkit.createInventory(null, 27, "§0§lEdit Kit: " + target);
                    RewardManager.CustomKit kit = rm.getCustomKit(target);
                    if (kit != null && kit.items != null) {
                        for (int i = 0; i < kit.items.length && i < 27; i++) {
                            inv.setItem(i, kit.items[i].clone());
                        }
                    }
                    player.openInventory(inv);
                    return true;
                }
            }
            return true;
        }

        // ==========================================
        // LỆNH NGƯỜI CHƠI (/kit)
        // ==========================================
        if (command.getName().equalsIgnoreCase("kit")) {
            if (args.length == 0) {
                Set<String> kits = rm.getKitNames();
                player.sendMessage("§e§lCÁC GÓI QUÀ HIỆN CÓ:");
                if (kits.isEmpty()) player.sendMessage("§7- (Chưa có gói nào)");
                else for (String k : kits) player.sendMessage("§7- §a" + k + " §f(/kit " + k + ")");
                return true;
            }

            rm.claimKit(player, args[0]);
            return true;
        }

        // ==========================================
        // LỆNH ĐIỂM DANH (/diemdanh)
        // ==========================================
        if (command.getName().equalsIgnoreCase("diemdanh")) {
            rm.openMonthlyGUI(player);
            return true;
        }

        return true;
    }

    // ==========================================
    // HỆ THỐNG GỢI Ý LỆNH (TAB COMPLETER)
    // ==========================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("kit")) {
            if (args.length == 1) {
                for (String k : plugin.getRewardManager().getKitNames()) {
                    if (k.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(k);
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("adminkit")) {
            // Giấu sạch lệnh nếu không phải Admin
            if (!sender.hasPermission("rpgcore.admin")) return completions;

            if (args.length == 1) {
                String[] subs = {"create", "edit", "list", "time"};
                for (String s : subs) {
                    if (s.startsWith(args[0].toLowerCase())) completions.add(s);
                }
            } else if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("edit") || sub.equals("time")) {
                    // Gợi ý tên Kit
                    for (String k : plugin.getRewardManager().getKitNames()) {
                        if (k.toLowerCase().startsWith(args[1].toLowerCase())) completions.add(k);
                    }
                    // Gợi ý thêm số ngày nếu đang dùng edit
                    if (sub.equals("edit")) {
                        for (int i = 1; i <= 31; i++) {
                            if (String.valueOf(i).startsWith(args[1])) completions.add(String.valueOf(i));
                        }
                    }
                }
            }
        }
        return completions;
    }
}