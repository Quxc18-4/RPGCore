package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;
import com.mongodb.client.model.WriteModel;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalEconomyManager {

    private final RPGCore plugin;
    private final MongoCollection<Document> collection;
    private final Map<String, EconomyData> marketData = new HashMap<>();

    public static class EconomyData {
        public long baseBuyPrice;
        public long baseSellPrice;

        public long totalBought; // Lịch sử tổng lượng Mua
        public long totalSold;   // Lịch sử tổng lượng Bán

        public long pendingBought; // Số lượng Mua trong ngày (Chưa chốt giá)
        public long pendingSold;   // Số lượng Bán trong ngày (Chưa chốt giá)

        public long currentBuyPrice;
        public long currentSellPrice;

        public EconomyData(long baseBuy, long baseSell, long totalBought, long totalSold, long pendingBought, long pendingSold) {
            this.baseBuyPrice = baseBuy;
            this.baseSellPrice = baseSell;
            this.totalBought = totalBought;
            this.totalSold = totalSold;
            this.pendingBought = pendingBought;
            this.pendingSold = pendingSold;
            this.currentBuyPrice = baseBuy;
            this.currentSellPrice = baseSell;
        }
    }

    public GlobalEconomyManager(RPGCore plugin) {
        this.plugin = plugin;
        this.collection = MongoManager.getInstance().getDatabase().getCollection("global_economy");
        syncEconomyData();
        startDailyTask();
    }

    // Thêm hàm này vào trong GlobalEconomyManager.java
    public boolean isItemInEconomy(String itemId) {
        return itemId != null && marketData.containsKey(itemId);
    }

    // ==========================================
    // 1. ĐỌC DỮ LIỆU TỪ YAML VÀ MONGODB
    // ==========================================
    public void syncEconomyData() {
        marketData.clear();
        File file = new File(plugin.getDataFolder(), "economy_base.yml");
        if (!file.exists()) {
            plugin.saveResource("economy_base.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("items")) return;

        // Tạo 1 danh sách chứa các thao tác ghi DB
        List<WriteModel<Document>> bulkOperations = new ArrayList<>();

        for (String itemId : config.getConfigurationSection("items").getKeys(false)) {
            long baseBuy = config.getLong("items." + itemId + ".buy_price", 0);
            long baseSell = config.getLong("items." + itemId + ".sell_price", 0);

            Document doc = collection.find(Filters.eq("_id", itemId)).first();
            long totalBought = 0, totalSold = 0, pendingBought = 0, pendingSold = 0;

            if (doc != null) {
                totalBought = doc.getLong("total_bought") != null ? doc.getLong("total_bought") : 0;
                totalSold = doc.getLong("total_sold") != null ? doc.getLong("total_sold") : 0;
                pendingBought = doc.getLong("pending_bought") != null ? doc.getLong("pending_bought") : 0;
                pendingSold = doc.getLong("pending_sold") != null ? doc.getLong("pending_sold") : 0;
            }

            EconomyData data = new EconomyData(baseBuy, baseSell, totalBought, totalSold, pendingBought, pendingSold);
            marketData.put(itemId, data);
            calculateCurrentPrice(itemId, data);

            // NẾU DATABASE TRỐNG -> Chuẩn bị gói hàng để gửi xuống MongoDB
            if (doc == null) {
                Document newDoc = new Document("_id", itemId)
                        .append("base_buy_price", data.baseBuyPrice)
                        .append("base_sell_price", data.baseSellPrice)
                        .append("total_bought", data.totalBought)
                        .append("total_sold", data.totalSold)
                        .append("pending_bought", data.pendingBought)
                        .append("pending_sold", data.pendingSold);

                bulkOperations.add(new ReplaceOneModel<>(Filters.eq("_id", itemId), newDoc, new ReplaceOptions().upsert(true)));
            }
        }

        // Đẩy 1 lần duy nhất toàn bộ vật phẩm xuống DB lúc khởi động
        if (!bulkOperations.isEmpty()) {
            collection.bulkWrite(bulkOperations);
            plugin.getLogger().info("Đã khởi tạo thành công " + bulkOperations.size() + " vật phẩm kinh tế vào MongoDB!");
        }

        plugin.getLogger().info("Đã đồng bộ dữ liệu Kinh Tế Toàn Cầu!");
    }

    // ==========================================
    // 2. THUẬT TOÁN ĐỘ SÂU THỊ TRƯỜNG (MARKET DEPTH)
    // ==========================================
    private void calculateCurrentPrice(String itemId, EconomyData data) {
        ItemManager.ItemData itemData = plugin.getItemManager().getItemData(itemId);
        if (itemData == null) return;

        // Xác định Threshold (Ngưỡng chênh lệch để đổi 1% giá) dựa trên Rarity
        int threshold = 1000;
        switch (itemData.rarity) {
            case UNCOMMON: threshold = 500; break;
            case RARE: threshold = 100; break;
            case EPIC: threshold = 20; break;
            case MYTHIC: threshold = 5; break;
            case LEGENDARY: threshold = 2; break;
        }

        // Tính toán cán cân Cung - Cầu
        long netFlow = data.totalBought - data.totalSold;
        double percentChange = (double) netFlow / threshold;

        // Giới hạn biên độ tối đa là +/- 50%
        if (percentChange > 50.0) percentChange = 50.0;
        if (percentChange < -50.0) percentChange = -50.0;

        double multiplier = 1.0 + (percentChange / 100.0);

        data.currentBuyPrice = Math.max(1, Math.round(data.baseBuyPrice * multiplier));
        data.currentSellPrice = Math.max(1, Math.round(data.baseSellPrice * multiplier));
    }

    // ==========================================
    // 3. XỬ LÝ GIAO DỊCH TRONG NGÀY
    // ==========================================
    public void recordTransaction(ItemStack item, int amount, boolean isPlayerBuying) {
        String id = plugin.getItemManager().getItemId(item);
        if (id == null || !marketData.containsKey(id)) return;

        EconomyData data = marketData.get(id);
        if (isPlayerBuying) {
            data.pendingBought += amount;
        } else {
            data.pendingSold += amount;
        }
        saveToDBAsync(id, data);
    }

    public long getBuyPrice(ItemStack item) {
        String id = plugin.getItemManager().getItemId(item);
        return (id != null && marketData.containsKey(id)) ? marketData.get(id).currentBuyPrice : 0;
    }

    public long getSellPrice(ItemStack item) {
        String id = plugin.getItemManager().getItemId(item);
        return (id != null && marketData.containsKey(id)) ? marketData.get(id).currentSellPrice : 0;
    }

    // ==========================================
    // 4. CHỐT SỔ LÚC 00:00 (TỰ ĐỘNG CHẠY)
    // ==========================================
    private void startDailyTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalTime now = LocalTime.now();
            // Kích hoạt chốt sổ khi đồng hồ hệ thống chỉ 00:00
            if (now.getHour() == 0 && now.getMinute() == 0 && now.getSecond() <= 10) {
                processDailyClosing();
            }
        }, 200L, 200L); // Quét mỗi 10 giây
    }

    public void processDailyClosing() {
        for (Map.Entry<String, EconomyData> entry : marketData.entrySet()) {
            String id = entry.getKey();
            EconomyData data = entry.getValue();

            // Cộng dồn giao dịch trong ngày vào tổng lịch sử
            data.totalBought += data.pendingBought;
            data.totalSold += data.pendingSold;

            // Reset dữ liệu ngày
            data.pendingBought = 0;
            data.pendingSold = 0;

            // Tính toán giá mới cho ngày hôm sau
            calculateCurrentPrice(id, data);
            saveToDBAsync(id, data);
        }
        Bukkit.broadcastMessage("§e§l[Kinh Tế] §fThị trường vừa đóng cửa và cập nhật giá mới cho ngày hôm nay!");
    }

    private void saveToDBAsync(String itemId, EconomyData data) {
        Document doc = new Document("_id", itemId)
                .append("base_buy_price", data.baseBuyPrice)
                .append("base_sell_price", data.baseSellPrice)
                .append("total_bought", data.totalBought)
                .append("total_sold", data.totalSold)
                .append("pending_bought", data.pendingBought)
                .append("pending_sold", data.pendingSold);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            collection.replaceOne(Filters.eq("_id", itemId), doc, new ReplaceOptions().upsert(true));
        });
    }
}