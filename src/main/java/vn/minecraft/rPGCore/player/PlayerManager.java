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

        // Khởi tạo player với class mặc định (đã tự động gọi initDefaultAttributes trong RPGPlayer)
        RPGPlayer rpgPlayer = new RPGPlayer(player, defaultClass);

        // Nếu đã có dữ liệu cũ trong file thì nạp lên
        if (config.contains(uuid.toString())) {
            String classId = config.getString(uuid + ".class");
            double savedResource = config.getDouble(uuid + ".resource");
            double savedExp = config.getDouble(uuid + ".exp", 0.0); // Tải EXP lẻ

            RPGClass savedClass = classManager.getClassById(classId);
            if (savedClass != null) {
                rpgPlayer.setClass(savedClass);
                rpgPlayer.setCurrentResource(savedResource);
            }

            // Nạp lại EXP lẻ (Cần thêm hàm setCurrentExp trong RPGPlayer)
            rpgPlayer.setCurrentExp(savedExp);

            // Nạp dữ liệu Attribute (Chỉ số)
            if (config.contains(uuid + ".attr")) {
                rpgPlayer.setUnspentAttrPoints(config.getInt(uuid + ".attr.unspent", 0));
                rpgPlayer.setAttribute("STR", config.getInt(uuid + ".attr.STR", 0));
                rpgPlayer.setAttribute("INT", config.getInt(uuid + ".attr.INT", 0));
                rpgPlayer.setAttribute("VIT", config.getInt(uuid + ".attr.VIT", 0));
                rpgPlayer.setAttribute("AGI", config.getInt(uuid + ".attr.AGI", 0));
                rpgPlayer.setAttribute("LUK", config.getInt(uuid + ".attr.LUK", 0));
            }
        } else {
            // Người chơi mới tinh, setup class mặc định
            rpgPlayer.setClass(defaultClass);
        }

        // QUAN TRỌNG: Cập nhật lại Máu/Mana tối đa sau khi đã nạp xong đồ đạc và điểm VIT/INT
        rpgPlayer.updateMaxStats();
        rpgPlayer.updateExpBar(); // Cập nhật lại thanh xanh lá của Minecraft

        activePlayers.put(uuid, rpgPlayer);
        return rpgPlayer;
    }

    // --- CƠ CHẾ LƯU DỮ LIỆU KHI THOÁT GAME ---
    public void savePlayer(UUID uuid) {
        RPGPlayer rpgPlayer = activePlayers.get(uuid);
        if (rpgPlayer != null) {
            // Lưu Class và Năng lượng
            config.set(uuid + ".class", rpgPlayer.getCurrentClass().getId());
            config.set(uuid + ".resource", rpgPlayer.getCurrentResource());

            // Lưu tiến trình EXP lẻ
            config.set(uuid + ".exp", rpgPlayer.getCurrentExp());

            // Lưu dữ liệu Attribute
            config.set(uuid + ".attr.unspent", rpgPlayer.getUnspentAttrPoints());
            config.set(uuid + ".attr.STR", rpgPlayer.getAttribute("STR"));
            config.set(uuid + ".attr.INT", rpgPlayer.getAttribute("INT"));
            config.set(uuid + ".attr.VIT", rpgPlayer.getAttribute("VIT"));
            config.set(uuid + ".attr.AGI", rpgPlayer.getAttribute("AGI"));
            config.set(uuid + ".attr.LUK", rpgPlayer.getAttribute("LUK"));

            try { config.save(file); }
            catch (IOException e) { e.printStackTrace(); }
        }
        activePlayers.remove(uuid);
    }

    public RPGPlayer getRPGPlayer(UUID uuid) { return activePlayers.get(uuid); }
    public Set<UUID> getAllPlayerUUIDs() { return activePlayers.keySet(); }

    public void saveAllPlayers() {
        for (UUID uuid : activePlayers.keySet()) { savePlayer(uuid); }
    }
}