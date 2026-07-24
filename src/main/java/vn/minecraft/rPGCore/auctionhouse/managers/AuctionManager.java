package vn.minecraft.rPGCore.auctionhouse.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;
import vn.minecraft.rPGCore.auctionhouse.models.AuctionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

    private final RPGCore plugin;
    private final MongoCollection<Document> activeCollection;
    private final MongoCollection<Document> historyCollection;

    public AuctionManager(RPGCore plugin) {
        this.plugin = plugin;
        this.activeCollection = MongoManager.getInstance().getDatabase().getCollection("auction_active");
        this.historyCollection = MongoManager.getInstance().getDatabase().getCollection("auction_history");
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ CHỢ ĐANG HOẠT ĐỘNG
    // ==========================================

    // Đăng bán 1 món đồ
    public void listNewItem(AuctionItem item) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            activeCollection.replaceOne(
                    Filters.eq("_id", item.getId()),
                    item.toDocument(),
                    new ReplaceOptions().upsert(true)
            );
        });
    }

    // Cập nhật lại món đồ (Dùng khi tính thuế xong hoặc bị khóa)
    public void updateActiveItem(AuctionItem item) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            activeCollection.replaceOne(
                    Filters.eq("_id", item.getId()),
                    item.toDocument(),
                    new ReplaceOptions().upsert(true)
            );
        });
    }

    // Lấy TOÀN BỘ đồ đang bán trên chợ (Để hiển thị cho người mua)
    public List<AuctionItem> getAllActiveAuctions() {
        List<AuctionItem> items = new ArrayList<>();
        // Chỉ lấy những món có status là ACTIVE
        for (Document doc : activeCollection.find(Filters.eq("status", AuctionItem.Status.ACTIVE.name()))) {
            AuctionItem item = AuctionItem.fromDocument(doc);
            if (item != null) items.add(item);
        }
        // Có thể sort theo thời gian đăng (mới nhất lên đầu) ở đây nếu muốn
        return items;
    }

    // Lấy TOÀN BỘ đồ của MỘT người chơi (Đang bán + Bị khóa)
    public List<AuctionItem> getPlayerAuctions(UUID sellerId) {
        List<AuctionItem> items = new ArrayList<>();
        for (Document doc : activeCollection.find(Filters.eq("seller_id", sellerId.toString()))) {
            AuctionItem item = AuctionItem.fromDocument(doc);
            if (item != null) items.add(item);
        }
        return items;
    }

    // Lấy 1 món đồ cụ thể bằng ID (Dùng khi người chơi bấm Mua hàng)
    public AuctionItem getAuctionItemById(String auctionId) {
        Document doc = activeCollection.find(Filters.eq("_id", auctionId)).first();
        return AuctionItem.fromDocument(doc);
    }

    // Đếm số lượng món đồ đang treo của 1 người chơi (Giới hạn tối đa 5 món)
    public int getPlayerActiveListingCount(UUID sellerId) {
        long count = activeCollection.countDocuments(Filters.eq("seller_id", sellerId.toString()));
        return (int) count;
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ LỊCH SỬ / GỠ BỎ
    // ==========================================

    // Xóa vĩnh viễn (Dùng khi người chơi chọn nút Vứt bỏ đồ bị khóa)
    public void deleteAuctionItem(String auctionId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            activeCollection.deleteOne(Filters.eq("_id", auctionId));
        });
    }

    // Di chuyển món đồ từ Active sang History (Dùng khi Đã Bán)
    public void moveToHistory(AuctionItem item, UUID buyerId) {
        item.setStatus(AuctionItem.Status.SOLD);

        Document historyDoc = item.toDocument();
        historyDoc.append("buyer_id", buyerId != null ? buyerId.toString() : "UNKNOWN");
        historyDoc.append("sold_time", System.currentTimeMillis());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            historyCollection.insertOne(historyDoc);           // Đẩy vào History
            activeCollection.deleteOne(Filters.eq("_id", item.getId())); // Xóa khỏi Active
        });
    }
}