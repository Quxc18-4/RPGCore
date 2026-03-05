package vn.minecraft.rPGCore.player;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerManager {
    private final Map<UUID, RPGPlayer> activePlayers = new HashMap<>();

    // Khai báo các biến quản lý file
    private final JavaPlugin plugin;
    private final ClassManager classManager;
    private File file;
    private FileConfiguration config;

    public PlayerManager(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        setupFile();
    }

    private void setupFile() {
        file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    // --- CƠ CHẾ TẢI DỮ LIỆU KHI VÀO GAME ---
    public RPGPlayer loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        RPGClass defaultClass = classManager.getClassById("commoner");
        RPGPlayer rpgPlayer = new RPGPlayer(player, defaultClass);

        // Nếu đã có dữ liệu cũ trong file thì nạp lên
        if (config.contains(uuid.toString())) {
            String classId = config.getString(uuid + ".class");
            double savedResource = config.getDouble(uuid + ".resource");

            RPGClass savedClass = classManager.getClassById(classId);
            if (savedClass != null) {
                rpgPlayer.setClass(savedClass);
                // Ghi đè lại mức năng lượng lúc vừa thoát game
                rpgPlayer.setCurrentResource(savedResource);
            }
        } else {
            // Người chơi mới tinh, setup máu mạc định
            rpgPlayer.setClass(defaultClass);
        }

        activePlayers.put(uuid, rpgPlayer);
        return rpgPlayer;
    }

    // --- CƠ CHẾ LƯU DỮ LIỆU KHI THOÁT GAME ---
    public void savePlayer(UUID uuid) {
        RPGPlayer rpgPlayer = activePlayers.get(uuid);
        if (rpgPlayer != null) {
            config.set(uuid + ".class", rpgPlayer.getCurrentClass().getId());
            config.set(uuid + ".resource", rpgPlayer.getCurrentResource());
            // (Máu sẽ được game tự lưu vào file world, không cần lưu tay)
            try { config.save(file); }
            catch (IOException e) { e.printStackTrace(); }
        }
        activePlayers.remove(uuid);
    }

    public RPGPlayer getRPGPlayer(UUID uuid) { return activePlayers.get(uuid); }
    public Set<UUID> getAllPlayerUUIDs() { return activePlayers.keySet(); }

    // Lưu toàn bộ khi tắt server
    public void saveAllPlayers() {
        for (UUID uuid : activePlayers.keySet()) { savePlayer(uuid); }
    }
}