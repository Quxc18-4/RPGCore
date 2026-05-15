package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.minecraft.rPGCore.RPGCore;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class JobLeaderboard {

    // Cache lưu trữ Top 10 của từng nghề
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
        File dataDir = new File(RPGCore.getInstance().getDataFolder(), "job_data");
        if (!dataDir.exists()) return;

        Map<String, List<LeaderboardEntry>> tempMap = new HashMap<>();

        for (File file : dataDir.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            String jobId = config.getString("currentJob", "NONE");
            if (jobId.equals("NONE")) continue;

            UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            int level = config.getInt("levels." + jobId, 1);
            double exp = config.getDouble("exps." + jobId, 0);

            tempMap.computeIfAbsent(jobId, k -> new ArrayList<>())
                    .add(new LeaderboardEntry(playerName, level, exp));
        }

        // Sắp xếp và lấy Top 10
        for (String jobId : tempMap.keySet()) {
            List<LeaderboardEntry> sorted = tempMap.get(jobId).stream()
                    .sorted((e1, e2) -> e2.level != e1.level ? Integer.compare(e2.level, e1.level) : Double.compare(e2.exp, e1.exp))
                    .limit(10)
                    .collect(Collectors.toList());
            topPlayers.put(jobId, sorted);
        }
    }

    public static List<LeaderboardEntry> getTop10(String jobId) {
        return topPlayers.getOrDefault(jobId.toUpperCase(), new ArrayList<>());
    }
}