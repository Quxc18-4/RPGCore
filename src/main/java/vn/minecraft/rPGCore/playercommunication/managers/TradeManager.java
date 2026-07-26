package vn.minecraft.rPGCore.playercommunication.managers;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;
import vn.minecraft.rPGCore.playercommunication.models.TradeRecord;
import vn.minecraft.rPGCore.playercommunication.models.TradeSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private final RPGCore plugin;
    private final MongoCollection<Document> tradeHistoryCol;

    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    public TradeManager(RPGCore plugin) {
        this.plugin = plugin;
        this.tradeHistoryCol = MongoManager.getInstance().getDatabase().getCollection("trade_history");
    }

    // ==========================================
    // 1. QUẢN LÝ LỜI MỜI
    // ==========================================
    public void sendTradeInvite(Player sender, Player receiver) {
        if (activeTrades.containsKey(sender.getUniqueId()) || activeTrades.containsKey(receiver.getUniqueId())) {
            sender.sendMessage("§c§l[!] §cMột trong hai người đang trong một giao dịch khác!");
            return;
        }

        // [FIX UX]: Chặn ngay lúc gửi lời mời nếu đứng quá xa
        if (sender.getWorld() != receiver.getWorld() || sender.getLocation().distance(receiver.getLocation()) > 10) {
            sender.sendMessage("§c§l[!] §cNgười chơi này ở quá xa (hoặc khác thế giới). Cần tiến lại gần để giao dịch!");
            return;
        }

        pendingInvites.put(receiver.getUniqueId(), sender.getUniqueId());
        sender.sendMessage("§a§l[Giao Dịch] §fĐã gửi yêu cầu giao dịch đến §e" + receiver.getName());

        receiver.sendMessage("§e§l[Giao Dịch] §b" + sender.getName() + " §fmuốn trao đổi vật phẩm với bạn!");
        receiver.sendMessage("§7Gõ §a/trade accept §7để đồng ý hoặc §c/trade deny §7để từ chối.");

        // Tự động hủy lời mời sau 30 giây
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.remove(receiver.getUniqueId(), sender.getUniqueId())) {
                sender.sendMessage("§c§l[!] §cLời mời giao dịch tới " + receiver.getName() + " đã hết hạn.");
                receiver.sendMessage("§c§l[!] §cLời mời giao dịch từ " + sender.getName() + " đã hết hạn.");
            }
        }, 20L * 30);
    }

    public void acceptTrade(Player receiver) {
        UUID senderId = pendingInvites.remove(receiver.getUniqueId());
        if (senderId == null) {
            receiver.sendMessage("§c§l[!] §cBạn không có lời mời giao dịch nào đang chờ!");
            return;
        }

        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            receiver.sendMessage("§c§l[!] §cNgười gửi lời mời đã thoát khỏi máy chủ.");
            return;
        }

        // [FIX UX]: Chặn lúc đồng ý (Phòng trường hợp gửi lời mời xong 1 người dùng lệnh /spawn bay đi chỗ khác)
        if (sender.getWorld() != receiver.getWorld() || sender.getLocation().distance(receiver.getLocation()) > 10) {
            receiver.sendMessage("§c§l[!] §cĐối tác giao dịch hiện đang ở quá xa!");
            sender.sendMessage("§c§l[!] §c" + receiver.getName() + " muốn đồng ý nhưng hai bạn đang ở quá xa nhau!");
            return;
        }

        // Tạo bàn giao dịch ảo
        TradeSession session = new TradeSession(sender, receiver);
        activeTrades.put(senderId, session);
        activeTrades.put(receiver.getUniqueId(), session);

        sender.sendMessage("§a" + receiver.getName() + " đã đồng ý giao dịch. Đang mở giao diện...");
        receiver.sendMessage("§aBắt đầu giao dịch với " + sender.getName() + "...");

        sender.openInventory(session.getInventory());
        receiver.openInventory(session.getInventory());

        // ==========================================
        // VÒNG LẶP KIỂM TRA LÚC ĐANG TRADE
        // (Chống trường hợp đang mở GUI mà lén lùi lại bỏ đi)
        // ==========================================
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeTrades.containsKey(senderId)) {
                    this.cancel();
                    return;
                }

                if (sender.isOnline() && receiver.isOnline()) {
                    boolean isDifferentWorld = sender.getWorld() != receiver.getWorld();
                    boolean isTooFar = !isDifferentWorld && sender.getLocation().distance(receiver.getLocation()) > 10;

                    if (isDifferentWorld || isTooFar) {
                        sender.sendMessage("§c§l[!] §cĐối phương đã rời đi, giao dịch tự động hủy!");
                        receiver.sendMessage("§c§l[!] §cKhoảng cách quá xa, giao dịch tự động hủy!");
                        sender.closeInventory();
                        receiver.closeInventory();
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void denyTrade(Player receiver) {
        UUID senderId = pendingInvites.remove(receiver.getUniqueId());
        if (senderId != null) {
            Player sender = Bukkit.getPlayer(senderId);
            if (sender != null && sender.isOnline()) {
                sender.sendMessage("§c§l[!] " + receiver.getName() + " đã từ chối giao dịch.");
            }
            receiver.sendMessage("§cĐã từ chối lời mời giao dịch.");
        } else {
            receiver.sendMessage("§c§l[!] §cBạn không có lời mời giao dịch nào!");
        }
    }

    public void removeActiveTrade(UUID playerId) {
        TradeSession session = activeTrades.remove(playerId);
        if (session != null) {
            activeTrades.remove(session.getPlayer1Id());
            activeTrades.remove(session.getPlayer2Id());
        }
    }

    public void saveTradeHistory(TradeSession session) {
        TradeRecord record = new TradeRecord(session);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            tradeHistoryCol.insertOne(record.toDocument());
        });
    }

    public Map<UUID, TradeSession> getActiveTrades() { return activeTrades; }
}