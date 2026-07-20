package vn.minecraft.rPGCore.jobs.core;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.Bukkit;
import vn.minecraft.rPGCore.database.MongoManager;

import java.util.*;
import java.util.stream.Collectors;

public class JobLeaderboard {

    // Danh sách tĩnh (Cache) để hiển thị trong GUI
    private static final Map<String, List<LeaderboardEntry>> topPlayers = new HashMap<>();

    public static class LeaderboardEntry {
        public String name;
        public int level;
        public double exp;

        public LeaderboardEntry(String name, int level, double exp) {
            this.name = name;
            this.level = level;
            this.exp = exp;
        }
    }

    public static void updateLeaderboards() {
        Bukkit.getLogger().info("[RPGCore] Đang cập nhật Bảng Xếp Hạng Nghề Nghiệp từ MongoDB...");

        MongoCollection<Document> jobsCollection = MongoManager.getInstance().getDatabase().getCollection("player_jobs");

        // MỚI: Lấy tên người chơi từ Collection "player_accounts" (AuthManager) để tránh lag server do OfflinePlayer
        MongoCollection<Document> accountsCollection = MongoManager.getInstance().getDatabase().getCollection("player_accounts");

        Map<String, List<LeaderboardEntry>> tempMap = new HashMap<>();

        // Quét tất cả Document trên MongoDB
        for (Document doc : jobsCollection.find()) {
            String jobId = doc.getString("currentJob");
            if (jobId == null || jobId.equals("NONE")) continue;

            try {
                String uuidStr = doc.getString("_id");

                // Truy vấn tên từ AuthManager
                Document accountDoc = accountsCollection.find(new Document("_id", uuidStr)).first();
                String playerName = (accountDoc != null && accountDoc.getString("playerName") != null) ? accountDoc.getString("playerName") : "Người chơi ẩn";

                Document levels = (Document) doc.get("levels");
                Document exps = (Document) doc.get("exps");

                int level = (levels != null && levels.getInteger(jobId) != null) ? levels.getInteger(jobId) : 1;
                double exp = (exps != null && exps.get(jobId) != null) ? ((Number) exps.get(jobId)).doubleValue() : 0.0;

                tempMap.computeIfAbsent(jobId.toUpperCase(), k -> new ArrayList<>())
                        .add(new LeaderboardEntry(playerName, level, exp));
            } catch (Exception ignored) {}
        }

        // Sắp xếp: Level cao trước -> Exp cao trước, cắt lấy Top 10
        for (String jobId : tempMap.keySet()) {
            List<LeaderboardEntry> sorted = tempMap.get(jobId).stream()
                    .sorted((e1, e2) -> e2.level != e1.level ? Integer.compare(e2.level, e1.level) : Double.compare(e2.exp, e1.exp))
                    .limit(10)
                    .collect(Collectors.toList());
            topPlayers.put(jobId, sorted);
        }
        Bukkit.getLogger().info("[RPGCore] Đã cập nhật xong Bảng Xếp Hạng Nghề Nghiệp!");
    }

    public static List<LeaderboardEntry> getTop10(String jobId) {
        return topPlayers.getOrDefault(jobId.toUpperCase(), new ArrayList<>());
    }
}