package vn.minecraft.rPGCore.commands;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.database.MongoManager;
import vn.minecraft.rPGCore.managers.CurrencyManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        // 1. LỆNH: /bal top (ĐÃ SỬA: ĐỌC TỪ MONGODB)
        // ==========================================
        if (args[0].equalsIgnoreCase("top")) {
            sender.sendMessage("§6§lBẢNG XẾP HẠNG PHÚ HÀO SERVER");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                MongoCollection<Document> collection = MongoManager.getInstance().getDatabase().getCollection("players");

                // Truy vấn MongoDB: Sắp xếp theo "balance" giảm dần (-1) và giới hạn 10 người
                List<Document> topDocs = collection.find().sort(new Document("balance", -1)).limit(10).into(new ArrayList<>());

                int count = 1;
                for (Document doc : topDocs) {
                    long bal = doc.getLong("balance") != null ? doc.getLong("balance") : 0L;
                    if (bal <= 0) continue;

                    String uuidStr = doc.getString("_id");
                    try {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Người chơi ẩn";
                        sender.sendMessage("§e#" + count + " §f" + name + " §8- " + currencyManager.formatCurrency(bal));
                        count++;
                    } catch (Exception ignored) {}
                }
            });
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
                    long newBal = Math.max(0, currentBal + amount);
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