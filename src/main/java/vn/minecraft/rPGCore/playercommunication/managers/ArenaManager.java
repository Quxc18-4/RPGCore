package vn.minecraft.rPGCore.playercommunication.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.playercommunication.models.Arena;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final RPGCore plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private File arenaFile;
    private FileConfiguration arenaConfig;

    public ArenaManager(RPGCore plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    // ==========================================
    // TẢI/LƯU FILE ARENAS.YML
    // ==========================================
    public void loadArenas() {
        arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenaFile.exists()) {
            try { arenaFile.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); }
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        arenas.clear();

        if (arenaConfig.contains("arenas")) {
            for (String key : arenaConfig.getConfigurationSection("arenas").getKeys(false)) {
                Arena arena = new Arena(key);

                if (arenaConfig.contains("arenas." + key + ".spawn1")) {
                    arena.setSpawn1((Location) arenaConfig.get("arenas." + key + ".spawn1"));
                }
                if (arenaConfig.contains("arenas." + key + ".spawn2")) {
                    arena.setSpawn2((Location) arenaConfig.get("arenas." + key + ".spawn2"));
                }
                arenas.put(key.toLowerCase(), arena);
            }
        }
        plugin.getLogger().info("Đã tải " + arenas.size() + " Đấu trường!");
    }

    public void saveArenas() {
        for (Arena arena : arenas.values()) {
            arenaConfig.set("arenas." + arena.getName() + ".spawn1", arena.getSpawn1());
            arenaConfig.set("arenas." + arena.getName() + ".spawn2", arena.getSpawn2());
        }
        try { arenaConfig.save(arenaFile); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ LỆNH TẠO MAP
    // ==========================================
    public boolean createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) return false;
        arenas.put(name.toLowerCase(), new Arena(name));
        saveArenas();
        return true;
    }

    public boolean deleteArena(String name) {
        if (!arenas.containsKey(name.toLowerCase())) return false;
        arenas.remove(name.toLowerCase());
        arenaConfig.set("arenas." + name, null); // Xóa khỏi file
        saveArenas();
        return true;
    }

    public boolean setArenaSpawn(String name, int pos, Location loc) {
        Arena arena = arenas.get(name.toLowerCase());
        if (arena == null) return false;

        if (pos == 1) arena.setSpawn1(loc);
        else if (pos == 2) arena.setSpawn2(loc);

        saveArenas();
        return true;
    }

    // ==========================================
    // TÌM MAP ĐANG RẢNH CHO 2 NGƯỜI VÀO
    // ==========================================
    public Arena getFreeArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isSetupComplete() && !arena.isOccupied()) {
                return arena;
            }
        }
        return null; // Trả về null nếu full map hoặc chưa set tọa độ
    }
}