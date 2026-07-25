package vn.minecraft.rPGCore.playercommunication.managers;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    // Lưu các lời mời chưa phản hồi: Key = Người nhận, Value = Người gửi
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    // Lưu các phiên đang diễn ra để cấm người chơi mở giao dịch khác
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

        // Tạo bàn giao dịch ảo
        TradeSession session = new TradeSession(sender, receiver);
        activeTrades.put(senderId, session);
        activeTrades.put(receiver.getUniqueId(), session);

        sender.sendMessage("§a" + receiver.getName() + " đã đồng ý giao dịch. Đang mở giao diện...");
        receiver.sendMessage("§aBắt đầu giao dịch với " + sender.getName() + "...");

        // Mở cái Inventory 54 ô cho CẢ HAI cùng nhìn
        sender.openInventory(session.getInventory());
        receiver.openInventory(session.getInventory());
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

    // Dọn dẹp trạng thái khi người chơi bấm nút Thoát (ESC)
    public void removeActiveTrade(UUID playerId) {
        TradeSession session = activeTrades.remove(playerId);
        if (session != null) {
            activeTrades.remove(session.getPlayer1Id());
            activeTrades.remove(session.getPlayer2Id());
        }
    }

    // ==========================================
    // 2. LƯU LỊCH SỬ MONGODB (Bất đồng bộ)
    // ==========================================
    public void saveTradeHistory(TradeSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TradeRecord record = new TradeRecord(session);
            tradeHistoryCol.insertOne(record.toDocument());
        });
    }

    public Map<UUID, TradeSession> getActiveTrades() { return activeTrades; }
}