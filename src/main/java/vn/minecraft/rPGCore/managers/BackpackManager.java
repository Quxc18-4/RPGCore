package vn.minecraft.rPGCore.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BackpackManager {

    private final RPGCore plugin;
    private final File folder;

    public BackpackManager(RPGCore plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "backpacks");
        // Tự động tạo thư mục nếu chưa có
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    // LƯU TÚI ĐỒ VÀO FILE YML
    public void saveBackpack(String uuid, Inventory inv) {
        File file = new File(folder, uuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // YamlConfiguration hỗ trợ lưu trực tiếp mảng ItemStack rất chuẩn
        config.set("items", inv.getContents());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể lưu túi đồ " + uuid);
            e.printStackTrace();
        }
    }

    // ĐỌC TÚI ĐỒ TỪ FILE YML VÀO GUI
    @SuppressWarnings("unchecked")
    public void loadBackpack(String uuid, Inventory inv) {
        File file = new File(folder, uuid + ".yml");
        if (!file.exists()) return; // File chưa có thì túi rỗng (Mặc định)

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = (List<ItemStack>) config.getList("items");

        if (items != null) {
            inv.setContents(items.toArray(new ItemStack[0]));
        }
    }
}