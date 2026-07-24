package vn.minecraft.rPGCore.auctionhouse.models;

import org.bson.Document;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

public class AuctionItem {
    private String id;
    private UUID sellerId;
    private String sellerName;
    private ItemStack item;
    private long price;
    private long listTime;
    private long nextTaxTime;
    private Status status;

    public enum Status {
        ACTIVE,     // Đang bán trên chợ
        LOCKED,     // Bị khóa do nợ thuế (Chờ chuộc hoặc xóa)
        EXPIRED,    // Hết hạn bán (Nếu bạn muốn sau này có vụ 30 ngày ko ai mua thì trả về)
        SOLD        // Đã bán (Lưu lại lịch sử)
    }

    // Tạo mới khi người chơi đăng bán
    public AuctionItem(UUID sellerId, String sellerName, ItemStack item, long price) {
        this.id = UUID.randomUUID().toString();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.listTime = System.currentTimeMillis();
        // Miễn phí 7 ngày đầu tiên (7 * 24 * 60 * 60 * 1000)
        this.nextTaxTime = this.listTime + 604800000L;
        this.status = Status.ACTIVE;
    }

    // Dùng để Load từ Database lên
    private AuctionItem() {}

    public Document toDocument() {
        return new Document("_id", id)
                .append("seller_id", sellerId.toString())
                .append("seller_name", sellerName)
                .append("item_base64", itemToBase64(item))
                .append("price", price)
                .append("list_time", listTime)
                .append("next_tax_time", nextTaxTime)
                .append("status", status.name());
    }

    public static AuctionItem fromDocument(Document doc) {
        if (doc == null) return null;
        AuctionItem ai = new AuctionItem();
        ai.id = doc.getString("_id");
        ai.sellerId = UUID.fromString(doc.getString("seller_id"));
        ai.sellerName = doc.getString("seller_name");
        ai.item = itemFromBase64(doc.getString("item_base64"));
        ai.price = doc.getLong("price");
        ai.listTime = doc.getLong("list_time");
        ai.nextTaxTime = doc.getLong("next_tax_time");
        ai.status = Status.valueOf(doc.getString("status"));
        return ai;
    }

    // --- BASE 64 UTILS ---
    private static String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private static ItemStack itemFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    // --- GETTERS & SETTERS ---
    public String getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public long getPrice() { return price; }
    public long getListTime() { return listTime; }
    public long getNextTaxTime() { return nextTaxTime; }
    public void setNextTaxTime(long nextTaxTime) { this.nextTaxTime = nextTaxTime; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}