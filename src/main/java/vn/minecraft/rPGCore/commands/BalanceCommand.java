package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.managers.CurrencyManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.io.File;
import java.util.*;

public class BalanceCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;
    private final CurrencyManager currencyManager;

    public BalanceCommand(JavaPlugin plugin, PlayerManager playerManager, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.currencyManager = currencyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nếu không có arg nào -> Xem tiền của bản thân
        if (args.length == 0) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) {
                player.sendMessage("§e[Ngân Hàng] §7Số dư hiện tại của bạn: " + currencyManager.formatCurrency(rpgPlayer.getBalance()));
            }
            return true;
        }

        // ==========================================
        // 1. LỆNH: /bal top (Xem top 10 đại gia)
        // ==========================================
        if (args[0].equalsIgnoreCase("top")) {
            sender.sendMessage("§6§lBẢNG XẾP HẠNG PHÚ HÀO SERVER");

            // Đọc thẳng từ file players.yml để lấy cả người offline
            File file = new File(plugin.getDataFolder(), "players.yml");
            if (!file.exists()) return true;
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            Map<String, Long> topBalances = new HashMap<>();
            for (String uuidStr : config.getKeys(false)) {
                long bal = config.getLong(uuidStr + ".balance", 0L);
                if (bal > 0) {
                    try {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Người chơi ẩn";
                        topBalances.put(name, bal);
                    } catch (Exception ignored) {}
                }
            }

            // Sắp xếp Map giảm dần
            List<Map.Entry<String, Long>> list = new ArrayList<>(topBalances.entrySet());
            list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            int count = 1;
            for (Map.Entry<String, Long> entry : list) {
                sender.sendMessage("§e#" + count + " §f" + entry.getKey() + " §8- " + currencyManager.formatCurrency(entry.getValue()));
                if (count++ >= 10) break;
            }
            return true;
        }

        // ==========================================
        // 2. LỆNH ADMIN: /bal coin (Lấy tiền vật lý)
        // ==========================================
        if (args[0].equalsIgnoreCase("coin")) {
            if (!sender.hasPermission("rpgcore.admin")) {
                sender.sendMessage("§cBạn không có quyền dùng lệnh này!");
                return true;
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.getInventory().addItem(currencyManager.getCurrencyItem("copper", 63));
                player.getInventory().addItem(currencyManager.getCurrencyItem("iron", 63));
                player.getInventory().addItem(currencyManager.getCurrencyItem("gold", 63));
                player.sendMessage("§aĐã nhận 3 loại tiền tệ để setup Cửa Hàng!");
            }
            return true;
        }

        // ==========================================
        // 3. LỆNH ADMIN: /bal add <số tiền> [player]
        // ==========================================
        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("rpgcore.admin")) {
                sender.sendMessage("§cBạn không có quyền dùng lệnh này!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§cHDSD: /bal add <số tiền> [tên_người_chơi]");
                return true;
            }

            try {
                long amount = Long.parseLong(args[1]);
                Player target = (args.length >= 3) ? Bukkit.getPlayer(args[2]) : (sender instanceof Player ? (Player) sender : null);

                if (target == null) {
                    sender.sendMessage("§cKhông tìm thấy người chơi!");
                    return true;
                }

                RPGPlayer rpgTarget = playerManager.getRPGPlayer(target.getUniqueId());
                if (rpgTarget != null) {
                    long currentBal = rpgTarget.getBalance();
                    long newBal = Math.max(0, currentBal + amount); // Không cho phép âm tiền
                    rpgTarget.setBalance(newBal);

                    sender.sendMessage("§aĐã thay đổi số dư của " + target.getName() + " thành " + currencyManager.formatCurrency(newBal));
                    target.sendMessage("§e[Ngân Hàng] §fBiến động số dư: " + " " + (amount > 0 ? "§a+" : "§c") + currencyManager.formatCurrency(Math.abs(amount)));
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cSố tiền không hợp lệ!");
            }
            return true;
        }

        // ==========================================
        // 4. LỆNH: /bal <player> (Xem tiền người khác)
        // ==========================================
        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            RPGPlayer rpgTarget = playerManager.getRPGPlayer(target.getUniqueId());
            if (rpgTarget != null) {
                sender.sendMessage("§e[Ngân Hàng] §7Số dư của " + target.getName() + ": " + currencyManager.formatCurrency(rpgTarget.getBalance()));
            }
        } else {
            sender.sendMessage("§cNgười chơi này không online!");
        }

        return true;
    }
}