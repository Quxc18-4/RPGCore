package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BackpackManager {

    private final RPGCore plugin;
    private final MongoCollection<Document> collection;

    // Bộ đệm RAM (Cache) siêu tốc
    private final Map<String, ItemStack[]> cache = new HashMap<>();

    public BackpackManager(RPGCore plugin) {
        this.plugin = plugin;
        this.collection = MongoManager.getInstance().getDatabase().getCollection("backpacks");
    }

    // ==========================================
    // LƯU TÚI ĐỒ (Ghi đè RAM + Đẩy ngầm xuống DB)
    // ==========================================
    public void saveBackpack(String uuid, Inventory inv) {
        ItemStack[] contents = inv.getContents();

        // 1. [ĐÃ FIX]: CLONE TỪNG VẬT PHẨM ĐỂ TRÁNH LỖI THAM CHIẾU CỦA BUKKIT
        ItemStack[] clonedForCache = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clonedForCache[i] = (contents[i] != null) ? contents[i].clone() : null;
        }
        cache.put(uuid, clonedForCache);

        // 2. Chuyển đổi và đẩy xuống MongoDB ở luồng chạy nền (Async)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String base64 = inventoryToBase64(contents);
            Document doc = new Document("_id", uuid).append("items_base64", base64);
            collection.replaceOne(Filters.eq("_id", uuid), doc, new ReplaceOptions().upsert(true));
        });
    }

    // ==========================================
    // ĐỌC TÚI ĐỒ (Ưu tiên đọc từ RAM trước)
    // ==========================================
    public void loadBackpack(String uuid, Inventory inv) {
        // 1. Kiểm tra RAM trước.
        if (cache.containsKey(uuid)) {
            // [ĐÃ FIX]: Phải Clone mảng từ Cache ra để dùng
            ItemStack[] cachedItems = cache.get(uuid);
            ItemStack[] clonedForInv = new ItemStack[cachedItems.length];
            for (int i = 0; i < cachedItems.length; i++) {
                clonedForInv[i] = (cachedItems[i] != null) ? cachedItems[i].clone() : null;
            }
            inv.setContents(clonedForInv);
            return;
        }

        // 2. Nếu RAM chưa có, gọi MongoDB
        Document doc = collection.find(Filters.eq("_id", uuid)).first();
        if (doc != null) {
            String base64 = doc.getString("items_base64");
            ItemStack[] items = inventoryFromBase64(base64);

            if (items != null && items.length > 0) {
                cache.put(uuid, items);

                // Clone đưa vào Inv
                ItemStack[] clonedForInv = new ItemStack[items.length];
                for (int i = 0; i < items.length; i++) {
                    clonedForInv[i] = (items[i] != null) ? items[i].clone() : null;
                }
                inv.setContents(clonedForInv);
            }
        } else {
            cache.put(uuid, new ItemStack[inv.getSize()]);
        }
    }

    // [MỚI]: Dọn RAM khi người chơi thoát game (Tránh tràn bộ nhớ)
    public void removeCache(String uuid) {
        cache.remove(uuid);
    }

    // ==========================================
    // CÔNG CỤ CHUYỂN ĐỔI (ĐỒNG BỘ CẤU TRÚC VỚI PLAYER MANAGER)
    // ==========================================
    private String inventoryToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // [ĐÃ FIX]: Lưu từng item một thay vì lưu nguyên mảng
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Lỗi khi mã hóa túi đồ: " + e.getMessage());
            return "";
        }
    }

    private ItemStack[] inventoryFromBase64(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            // [ĐÃ FIX]: Giải mã từng item một
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().severe("Lỗi khi giải mã túi đồ: " + e.getMessage());
            return new ItemStack[0];
        }
    }
}