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

        if (config.contains(uuid.toString())) {
            String classId = config.getString(uuid + ".class");
            double savedResource = config.getDouble(uuid + ".resource");
            double savedExp = config.getDouble(uuid + ".exp", 0.0);

            // Nạp dữ liệu tiền tệ
            long savedBalance = config.getLong(uuid + ".balance", 0L);
            rpgPlayer.setBalance(savedBalance);

            RPGClass savedClass = classManager.getClassById(classId);
            if (savedClass != null) {
                rpgPlayer.setClass(savedClass);
                rpgPlayer.setCurrentResource(savedResource);
            }

            rpgPlayer.setCurrentExp(savedExp);

            // Nạp dữ liệu Attribute (Gọi qua getAttributeData)
            rpgPlayer.getAttributeData().setUnspentAttrPoints(config.getInt(uuid + ".attr.unspent", 0));
            rpgPlayer.getAttributeData().setAttribute("STR", config.getInt(uuid + ".attr.STR", 0));
            rpgPlayer.getAttributeData().setAttribute("INT", config.getInt(uuid + ".attr.INT", 0));
            rpgPlayer.getAttributeData().setAttribute("VIT", config.getInt(uuid + ".attr.VIT", 0));
            rpgPlayer.getAttributeData().setAttribute("AGI", config.getInt(uuid + ".attr.AGI", 0));
            rpgPlayer.getAttributeData().setAttribute("LUK", config.getInt(uuid + ".attr.LUK", 0));
        } else {
            rpgPlayer.setClass(defaultClass);
        }

        // NẠP HỆ THỐNG KỸ NĂNG VÀ COMBO (Gọi qua getSkillData)
        rpgPlayer.getSkillData().setSkillPoints(config.getInt(uuid + ".sp", 0));

        if (config.contains(uuid + ".skills")) {
            for (String skillId : config.getConfigurationSection(uuid + ".skills").getKeys(false)) {
                int level = config.getInt(uuid + ".skills." + skillId);
                rpgPlayer.getSkillData().setSkillLevel(skillId, level);
            }
        }

        if (config.contains(uuid + ".binds")) {
            for (String combo : config.getConfigurationSection(uuid + ".binds").getKeys(false)) {
                String skillId = config.getString(uuid + ".binds." + combo);
                rpgPlayer.getSkillData().bindCombo(combo, skillId);
            }
        }

        rpgPlayer.updateMaxStats();
        rpgPlayer.updateExpBar();
        rpgPlayer.updateEquipmentStats(); // <-- THÊM DÒNG NÀY ĐỂ QUÉT CHỈ SỐ LÚC VỪA VÀO GAME

        activePlayers.put(uuid, rpgPlayer);
        return rpgPlayer;
    }

    // --- CƠ CHẾ LƯU DỮ LIỆU KHI THOÁT GAME ---
    public void savePlayer(UUID uuid) {
        RPGPlayer rpgPlayer = activePlayers.get(uuid);
        if (rpgPlayer != null) {
            config.set(uuid + ".class", rpgPlayer.getCurrentClass().getId());
            config.set(uuid + ".resource", rpgPlayer.getCurrentResource());
            config.set(uuid + ".exp", rpgPlayer.getCurrentExp());

            // Lưu dữ liệu tiền tệ
            config.set(uuid + ".balance", rpgPlayer.getBalance());

            // Lưu dữ liệu Attribute
            config.set(uuid + ".attr.unspent", rpgPlayer.getAttributeData().getUnspentAttrPoints());
            config.set(uuid + ".attr.STR", rpgPlayer.getAttributeData().getAttribute("STR"));
            config.set(uuid + ".attr.INT", rpgPlayer.getAttributeData().getAttribute("INT"));
            config.set(uuid + ".attr.VIT", rpgPlayer.getAttributeData().getAttribute("VIT"));
            config.set(uuid + ".attr.AGI", rpgPlayer.getAttributeData().getAttribute("AGI"));
            config.set(uuid + ".attr.LUK", rpgPlayer.getAttributeData().getAttribute("LUK"));

            // LƯU HỆ THỐNG KỸ NĂNG VÀ COMBO
            config.set(uuid + ".sp", rpgPlayer.getSkillData().getSkillPoints());

            config.set(uuid + ".skills", null);
            config.set(uuid + ".binds", null);

            for (Map.Entry<String, Integer> entry : rpgPlayer.getSkillData().getSkills().entrySet()) {
                config.set(uuid + ".skills." + entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, String> entry : rpgPlayer.getSkillData().getBinds().entrySet()) {
                config.set(uuid + ".binds." + entry.getKey(), entry.getValue());
            }

            try { config.save(file); }
            catch (IOException e) { e.printStackTrace(); }
        }
        activePlayers.remove(uuid);
    }

    public RPGPlayer getRPGPlayer(UUID uuid) { return activePlayers.get(uuid); }
    public Set<UUID> getAllPlayerUUIDs() { return activePlayers.keySet(); }

    public void saveAllPlayers() {
        // Tạo một ArrayList mới để copy toàn bộ UUID ra, tránh lỗi khi vừa duyệt vừa xóa
        for (UUID uuid : new java.util.ArrayList<>(activePlayers.keySet())) {
            savePlayer(uuid);
        }
    }
}