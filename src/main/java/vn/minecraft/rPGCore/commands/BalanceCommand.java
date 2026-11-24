package vn.minecraft.rPGCore.commands;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
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

    private static class RichPlayerEntry {
        final UUID uuid;
        final String name;
        final long totalBalance;

        RichPlayerEntry(UUID uuid, String name, long totalBalance) {
            this.uuid = uuid;
            this.name = name;
            this.totalBalance = totalBalance;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Xem tiền của bản thân
        if (args.length == 0) {
            if (!(sender instanceof Player player)) return true;
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) {
                player.sendMessage("§e[Ngân Hàng] §7Số dư hiện tại của bạn: " + currencyManager.formatCurrency(rpgPlayer.getBalance()));
            }
            return true;
        }

        // ==========================================
        // 1. LỆNH: /bal top (ĐÃ FIX CHO CẤU TRÚC ĐA SLOT)
        // ==========================================
        if (args[0].equalsIgnoreCase("top")) {
            sender.sendMessage("§6§lBẢNG XẾP HẠNG PHÚ HÀO SERVER");

            // Chụp nhanh số dư trên RAM của người chơi đang online để đảm bảo tính chính xác tức thời
            Map<UUID, Integer> onlineActiveSlots = new HashMap<>();
            Map<UUID, Long> onlineBalances = new HashMap<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                RPGPlayer rp = playerManager.getRPGPlayer(p.getUniqueId());
                if (rp != null) {
                    onlineActiveSlots.put(p.getUniqueId(), rp.getActiveSlot());
                    onlineBalances.put(p.getUniqueId(), rp.getBalance());
                }
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                MongoCollection<Document> collection = MongoManager.getInstance().getDatabase().getCollection("players");
                List<RichPlayerEntry> entries = new ArrayList<>();

                // Chỉ lấy các trường cần thiết, loại bỏ Base64 túi đồ để tối ưu tốc độ đọc
                for (Document doc : collection.find().projection(
                        Projections.include("_id", "username", "balance", "slots")
                )) {
                    String uuidStr = doc.getString("_id");
                    if (uuidStr == null) continue;

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }

                    long totalBalance = 0L;
                    Document slotsDoc = doc.get("slots", Document.class);
                    Integer onlineSlot = onlineActiveSlots.get(uuid);
                    Long onlineBal = onlineBalances.get(uuid);

                    if (slotsDoc != null && !slotsDoc.isEmpty()) {
                        for (String slotKey : slotsDoc.keySet()) {
                            if (onlineSlot != null && onlineBal != null && String.valueOf(onlineSlot).equals(slotKey)) {
                                totalBalance += onlineBal;
                            } else {
                                Document sDoc = slotsDoc.get(slotKey, Document.class);
                                if (sDoc != null && sDoc.get("balance") instanceof Number num) {
                                    totalBalance += num.longValue();
                                }
                            }
                        }
                        // Trường hợp người chơi vừa tạo slot mới trên RAM nhưng chưa kịp Auto-Save
                        if (onlineSlot != null && onlineBal != null && !slotsDoc.containsKey(String.valueOf(onlineSlot))) {
                            totalBalance += onlineBal;
                        }
                    } else {
                        // Fallback cho dữ liệu cũ chưa chuyển sang cấu trúc slots
                        if (onlineBal != null) {
                            totalBalance = onlineBal;
                        } else if (doc.get("balance") instanceof Number num) {
                            totalBalance = num.longValue();
                        }
                    }

                    if (totalBalance <= 0) continue;

                    String name = doc.getString("username");
                    if (name == null || name.isEmpty()) {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                        name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Người chơi ẩn";
                    }

                    entries.add(new RichPlayerEntry(uuid, name, totalBalance));
                }

                entries.sort((a, b) -> Long.compare(b.totalBalance, a.totalBalance));
                List<RichPlayerEntry> top10 = entries.subList(0, Math.min(10, entries.size()));

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (top10.isEmpty()) {
                        sender.sendMessage("§7Chưa có dữ liệu xếp hạng.");
                        return;
                    }
                    int rank = 1;
                    for (RichPlayerEntry entry : top10) {
                        sender.sendMessage("§e#" + rank + " §f" + entry.name + " §8- " + currencyManager.formatCurrency(entry.totalBalance));
                        rank++;
                    }
                });
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
            if (sender instanceof Player player) {
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
                    playerManager.savePlayer(target.getUniqueId(), true, false);

                    sender.sendMessage("§aĐã thay đổi số dư của " + target.getName() + " thành " + currencyManager.formatCurrency(newBal));
                    target.sendMessage("§e[Ngân Hàng] §fBiến động số dư: " + (amount >= 0 ? "§a+" : "§c-") + currencyManager.formatCurrency(Math.abs(amount)));
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