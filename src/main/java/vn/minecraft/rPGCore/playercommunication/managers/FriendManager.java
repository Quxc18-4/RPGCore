package vn.minecraft.rPGCore.playercommunication.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendManager {

    private final RPGCore plugin;
    private final MongoCollection<Document> friendCol;

    public FriendManager(RPGCore plugin) {
        this.plugin = plugin;
        this.friendCol = MongoManager.getInstance().getDatabase().getCollection("friends");
    }

    // Tự động tạo Document cho người chơi mới nếu chưa có
    private void ensureDocumentExists(UUID uuid) {
        Document doc = friendCol.find(Filters.eq("_id", uuid.toString())).first();
        if (doc == null) {
            doc = new Document("_id", uuid.toString())
                    .append("friends", new ArrayList<String>())
                    .append("pending_requests", new ArrayList<String>());
            friendCol.insertOne(doc);
        }
    }

    // ==========================================
    // 1. GỬI LỜI MỜI KẾT BẠN
    // ==========================================
    public void sendFriendRequest(Player sender, OfflinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String senderId = sender.getUniqueId().toString();
            String targetId = target.getUniqueId().toString();

            ensureDocumentExists(target.getUniqueId());
            ensureDocumentExists(sender.getUniqueId());

            Document targetDoc = friendCol.find(Filters.eq("_id", targetId)).first();

            // Check nếu đã là bạn
            List<String> friends = targetDoc.getList("friends", String.class, new ArrayList<>());
            if (friends.contains(senderId)) {
                sender.sendMessage("§c§l[!] §cHai người đã là bạn bè rồi!");
                return;
            }

            // Check nếu đã gửi lời mời rồi
            List<String> requests = targetDoc.getList("pending_requests", String.class, new ArrayList<>());
            if (requests.contains(senderId)) {
                sender.sendMessage("§c§l[!] §cBạn đã gửi lời mời rồi, đang chờ đối phương phản hồi.");
                return;
            }

            // Đẩy UUID của sender vào mảng pending_requests của target
            friendCol.updateOne(Filters.eq("_id", targetId), Updates.addToSet("pending_requests", senderId));
            sender.sendMessage("§a§l[Bạn Bè] §fĐã gửi lời mời kết bạn đến §e" + target.getName());

            if (target.isOnline()) {
                Player onlineTarget = target.getPlayer();
                onlineTarget.sendMessage("§a§l[Bạn Bè] §b" + sender.getName() + " §fmuốn kết bạn với bạn!");
                onlineTarget.sendMessage("§7Gõ §a/friend accept " + sender.getName() + " §7để đồng ý.");
            }
        });
    }

    // ==========================================
    // 2. CHẤP NHẬN KẾT BẠN
    // ==========================================
    public void acceptFriendRequest(Player receiver, OfflinePlayer sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String receiverId = receiver.getUniqueId().toString();
            String senderId = sender.getUniqueId().toString();

            ensureDocumentExists(receiver.getUniqueId());
            Document receiverDoc = friendCol.find(Filters.eq("_id", receiverId)).first();
            List<String> requests = receiverDoc.getList("pending_requests", String.class, new ArrayList<>());

            if (!requests.contains(senderId)) {
                receiver.sendMessage("§c§l[!] §cNgười này chưa gửi lời mời kết bạn cho bạn.");
                return;
            }

            // 1. Xóa khỏi danh sách chờ của Receiver
            friendCol.updateOne(Filters.eq("_id", receiverId), Updates.pull("pending_requests", senderId));

            // 2. Thêm vào danh sách bạn bè của CẢ HAI
            friendCol.updateOne(Filters.eq("_id", receiverId), Updates.addToSet("friends", senderId));
            friendCol.updateOne(Filters.eq("_id", senderId), Updates.addToSet("friends", receiverId));

            receiver.sendMessage("§a§l[Bạn Bè] §fBạn và §e" + sender.getName() + " §fđã trở thành bạn bè!");

            if (sender.isOnline()) {
                sender.getPlayer().sendMessage("§a§l[Bạn Bè] §e" + receiver.getName() + " §fđã chấp nhận lời mời kết bạn!");
            }
        });
    }

    // ==========================================
    // 3. XÓA BẠN BÈ / HỦY KẾT BẠN
    // ==========================================
    public void removeFriend(Player player, OfflinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String playerId = player.getUniqueId().toString();
            String targetId = target.getUniqueId().toString();

            // Rút UUID ra khỏi danh sách của CẢ HAI
            friendCol.updateOne(Filters.eq("_id", playerId), Updates.pull("friends", targetId));
            friendCol.updateOne(Filters.eq("_id", targetId), Updates.pull("friends", playerId));

            player.sendMessage("§c§l[Bạn Bè] §fĐã hủy kết bạn với §e" + target.getName());
        });
    }

    // ==========================================
    // 4. XEM DANH SÁCH BẠN BÈ
    // ==========================================
    public void listFriends(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ensureDocumentExists(player.getUniqueId());
            Document doc = friendCol.find(Filters.eq("_id", player.getUniqueId().toString())).first();
            List<String> friends = doc.getList("friends", String.class, new ArrayList<>());

            player.sendMessage("§8§m                                ");
            player.sendMessage("§a§lDANH SÁCH BẠN BÈ §7(" + friends.size() + ")");

            if (friends.isEmpty()) {
                player.sendMessage("§7Bạn chưa có người bạn nào. Hãy dùng /friend add <tên> để kết bạn nhé!");
            } else {
                for (String friendId : friends) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(friendId));
                    String status = p.isOnline() ? "§a[Online]" : "§c[Offline]";
                    player.sendMessage("§8- " + status + " §f" + p.getName());
                }
            }
            player.sendMessage("§8§m                                ");
        });
    }
}