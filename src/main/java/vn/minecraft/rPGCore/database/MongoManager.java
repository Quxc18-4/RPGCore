package vn.minecraft.rPGCore.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class MongoManager {

    // Áp dụng Singleton Pattern để đảm bảo chỉ có 1 kết nối duy nhất toàn Server
    private static MongoManager instance;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private final JavaPlugin plugin;

    private MongoManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new MongoManager(plugin);
            instance.connect();
        }
    }

    public static MongoManager getInstance() {
        return instance;
    }

    private void connect() {
        try {
            // 1. Chuỗi kết nối tới MongoDB Compass bạn vừa cài
            ConnectionString connString = new ConnectionString("mongodb://localhost:27017");

            // 2. Cấu hình Connection Pool (Tối ưu cho Server Minecraft)
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .applyToConnectionPoolSettings(builder -> builder
                            .maxSize(50) // Tối đa 50 luồng kết nối cùng lúc
                            .maxWaitTime(5, TimeUnit.SECONDS))
                    .build();

            // 3. Khởi tạo Client và chọn Database
            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase("rpg_server_db");

            // Test Ping thử xem Database có phản hồi không
            database.runCommand(new org.bson.Document("ping", 1));

            plugin.getLogger().info("=========================================");
            plugin.getLogger().info("✅ [RPGCore] KẾT NỐI MONGODB THÀNH CÔNG!");
            plugin.getLogger().info("=========================================");

        } catch (Exception e) {
            plugin.getLogger().severe("=========================================");
            plugin.getLogger().severe("❌ [RPGCore] KHÔNG THỂ KẾT NỐI MONGODB!");
            plugin.getLogger().severe("Vui lòng kiểm tra lại MongoDB Compass.");
            plugin.getLogger().severe("=========================================");
            e.printStackTrace();
        }
    }

    // Hàm public để các class/plugin khác (như RPGGuild, RPGDens) lấy database ra dùng
    public MongoDatabase getDatabase() {
        return database;
    }

    // Hàm gọi khi Server tắt (Stop / Reload)
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("[RPGCore] Đã đóng kết nối MongoDB an toàn.");
        }
    }
}