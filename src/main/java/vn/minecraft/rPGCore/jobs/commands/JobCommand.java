package vn.minecraft.rPGCore.jobs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.jobs.gui.JobSelectionGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JobCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            JobSelectionGUI.openGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("menu")) {
            JobSelectionGUI.openStatusMenu(player);
            return true;
        }

        // CỤM LỆNH ẨN DÀNH RIÊNG CHO ADMIN
        if (args[0].equalsIgnoreCase("admin")) {
            if (!player.isOp()) {
                player.sendMessage("§c§l(!) §cThao tác không hợp lệ. Dùng /job hoặc /job menu.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cCú pháp Admin: /job admin <roll/level/resetshop/baits> [số]");
                return true;
            }

            if (args[1].equalsIgnoreCase("roll")) {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.rollNewQuests(player, true, true);
                player.sendMessage("§a§l[!] §fĐã làm mới lại toàn bộ Nhiệm vụ Ngày & Tuần của bạn!");
                return true;
            }

            if (args[1].equalsIgnoreCase("resetshop")) {
                vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                data.getShopMonthlyPurchases().clear();
                player.sendMessage("§a§l[!] §fĐã reset toàn bộ giới hạn mua hàng trong Cửa hàng của bạn!");
                return true;
            }

            // [CẬP NHẬT 2]: LỆNH LẤY TOÀN BỘ MỒI CÂU - CÓ SẮP XẾP
            if (args[1].equalsIgnoreCase("baits")) {
                org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, "§8Kho Mồi Câu Admin");
                vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();

                List<String> baseBaits = new ArrayList<>();
                List<String> buffBaits = new ArrayList<>();
                List<String> otherBaits = new ArrayList<>();

                // Phân loại mồi
                for (String id : im.getAllIds()) {
                    if (id.startsWith("bait_base_")) baseBaits.add(id);
                    else if (id.startsWith("bait_buff_")) buffBaits.add(id);
                    else if (id.startsWith("bait_")) otherBaits.add(id);
                }

                // Sắp xếp Alphabet
                baseBaits.sort(String::compareTo);
                buffBaits.sort(String::compareTo);
                otherBaits.sort(String::compareTo);

                // Thêm vào Rương theo thứ tự: Nền -> Buff -> Khác
                for (String id : baseBaits) inv.addItem(im.buildItem(id, 64));
                for (String id : buffBaits) inv.addItem(im.buildItem(id, 64));
                for (String id : otherBaits) inv.addItem(im.buildItem(id, 64));

                player.openInventory(inv);
                return true;
            }

            if (args[1].equalsIgnoreCase("level")) {
                if (args.length < 3) {
                    player.sendMessage("§cCú pháp: /job admin level <số>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                    String jobId = data.getJobId();

                    if (jobId.equals("NONE")) {
                        player.sendMessage("§cNgười chơi chưa có nghề!");
                        return true;
                    }

                    vn.minecraft.rPGCore.jobs.core.RPGJob currentJob = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(player);
                    int currentLvl = data.getLevel(jobId);
                    int maxLvl = currentJob.getMaxLevel();

                    if (currentLvl >= maxLvl) {
                        data.addJobPoints(jobId, amount);
                        player.sendMessage("§aĐã đạt cấp tối đa! Tự động chuyển thành cộng §d" + amount + " JP§a.");
                    } else {
                        int newLvl = Math.min(maxLvl, currentLvl + amount);
                        data.setLevel(jobId, newLvl);
                        player.sendMessage("§aĐã tăng cấp nghề lên §bCấp " + newLvl + "§a.");
                    }
                } catch (Exception e) {
                    player.sendMessage("§cVui lòng nhập một số hợp lệ!");
                }
                return true;
            }
        }

        player.sendMessage("§c§l(!) §cThao tác không hợp lệ. Dùng /job hoặc /job menu.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("menu");
            if (sender.isOp()) subCommands.add("admin");

            String current = args[0].toLowerCase();
            return subCommands.stream().filter(s -> s.startsWith(current)).collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.isOp()) {
            List<String> adminCmds = new ArrayList<>();
            adminCmds.add("roll");
            adminCmds.add("level");
            adminCmds.add("resetshop");
            adminCmds.add("baits"); // Tab Complete cho baits
            String current = args[1].toLowerCase();
            return adminCmds.stream().filter(s -> s.startsWith(current)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}