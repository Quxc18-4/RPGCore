package vn.minecraft.rPGCore.mounts.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class MountDataManager implements Listener {

    private final JavaPlugin plugin;
    private final MountManager mountManager;
    private final File dataFolder;
    private final Gson gson;

    public MountDataManager(JavaPlugin plugin, MountManager mountManager) {
        this.plugin = plugin;
        this.mountManager = mountManager;
        // Tạo folder riêng "plugins/RPGCore/mounts/"
        this.dataFolder = new File(plugin.getDataFolder(), "mounts");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        // Bật PrettyPrinting để file JSON có định dạng thụt lề dễ đọc
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // ===============================================
    // LƯU TOÀN BỘ SERVER (Dùng khi Server tắt / onDisable)
    // ===============================================
    public void saveAll() {
        for (UUID uuid : mountManager.getLoadedProfilesMap().keySet()) {
            saveProfile(uuid);
        }
        plugin.getLogger().info("Đã lưu dữ liệu Thú Cưỡi của toàn bộ người chơi!");
    }

    // ===============================================
    // ĐỌC / GHI CÁ NHÂN
    // ===============================================
    public void loadProfile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PlayerMountProfile profile = gson.fromJson(reader, PlayerMountProfile.class);
                mountManager.getLoadedProfilesMap().put(uuid, profile);
            } catch (IOException e) {
                plugin.getLogger().severe("Lỗi khi đọc file thú cưỡi JSON của: " + uuid);
                e.printStackTrace();
            }
        } else {
            // Lần đầu vào server, tạo mới hồ sơ 1 slot miễn phí
            mountManager.getLoadedProfilesMap().put(uuid, new PlayerMountProfile(uuid));
        }
    }

    public void saveProfile(UUID uuid) {
        PlayerMountProfile profile = mountManager.getLoadedProfilesMap().get(uuid);
        if (profile == null) return;

        File file = new File(dataFolder, uuid.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(profile, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Lỗi khi lưu file thú cưỡi JSON của: " + uuid);
            e.printStackTrace();
        }
    }

    // ===============================================
    // LẮNG NGHE SỰ KIỆN JOIN / QUIT ĐỂ AUTO SAVE
    // ===============================================
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Load data vào RAM
        loadProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 1. Lưu Data vào JSON
        saveProfile(player.getUniqueId());

        // 2. Xóa khỏi RAM để giải phóng bộ nhớ
        mountManager.getLoadedProfilesMap().remove(player.getUniqueId());
    }
}