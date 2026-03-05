package vn.minecraft.rPGCore.classes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Nếu file chưa tồn tại, copy từ trong thư mục resources ra ngoài
        if (!file.exists()) {
            plugin.saveResource("classes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("classes");

        if (section == null) return;

        // Quét tất cả các class trong file yaml
        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display-name", key);
            Material icon = Material.valueOf(section.getString(key + ".icon", "STONE"));
            List<String> desc = section.getStringList(key + ".description");
            double maxHp = section.getDouble(key + ".max-health", 100.0);
            double maxRes = section.getDouble(key + ".max-resource", 100.0);
            ResourceType resType = ResourceType.valueOf(section.getString(key + ".resource-type", "STAMINA"));
            double regen = section.getDouble(key + ".regen-rate", 1.0);
            boolean enchanted = section.getBoolean(key + ".enchanted", false);

            RPGClass rpgClass = new RPGClass(key, displayName, icon, desc, maxHp, maxRes, resType, regen, enchanted);
            classMap.put(key, rpgClass);
        }
        plugin.getLogger().info("Da tai thanh cong " + classMap.size() + " class!");
    }

    public Map<String, RPGClass> getClasses() {
        return classMap;
    }

    public RPGClass getClassById(String id) {
        return classMap.get(id.toLowerCase());
    }
}