package vn.minecraft.rPGCore.classes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.impl.ArcherClass;
import vn.minecraft.rPGCore.classes.impl.AssassinClass;
import vn.minecraft.rPGCore.classes.impl.DefaultClass;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassManager {

    private final Map<String, RPGClass> classMap = new HashMap<>();
    private final JavaPlugin plugin;

    public ClassManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadClasses() {
        classMap.clear();
        File file = new File(plugin.getDataFolder(), "classes.yml");

        if (!file.exists()) {
            plugin.saveResource("classes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("classes");

        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display-name", key);
            Material icon = Material.valueOf(section.getString(key + ".icon", "STONE"));
            List<String> desc = section.getStringList(key + ".description");
            double maxHp = section.getDouble(key + ".max-health", 100.0);
            double maxRes = section.getDouble(key + ".max-resource", 100.0);
            ResourceType resType = ResourceType.valueOf(section.getString(key + ".resource-type", "STAMINA"));
            double regen = section.getDouble(key + ".regen-rate", 1.0);
            boolean enchanted = section.getBoolean(key + ".enchanted", false);

            RPGClass rpgClass;

            // ==========================================
            // FACTORY LOGIC: KHỞI TẠO ĐÚNG HỆ PHÁI
            // ==========================================
            switch (key.toLowerCase()) {
                case "archer":
                    rpgClass = new ArcherClass(key, displayName, icon, desc, maxHp, maxRes, resType, regen, enchanted);
                    break;
                case "assassin":
                    rpgClass = new AssassinClass(key, displayName, icon, desc, maxHp, maxRes, resType, regen, enchanted);
                    break;
                default:
                    // Các phái chưa code riêng sẽ xài khung mặc định
                    rpgClass = new DefaultClass(key, displayName, icon, desc, maxHp, maxRes, resType, regen, enchanted);
                    break;
            }

            classMap.put(key, rpgClass);
        }
        plugin.getLogger().info("Đã tải thành công " + classMap.size() + " class bằng Factory!");
    }

    public Map<String, RPGClass> getClasses() {
        return classMap;
    }

    public RPGClass getClassById(String id) {
        return classMap.get(id.toLowerCase());
    }
}