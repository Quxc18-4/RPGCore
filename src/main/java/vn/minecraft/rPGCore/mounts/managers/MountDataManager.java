package vn.minecraft.rPGCore.mounts.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.database.MongoManager;
import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;
import vn.minecraft.rPGCore.mounts.type.MountArchetype;

import java.util.Map;
import java.util.UUID;

public class MountDataManager implements Listener {

    private final JavaPlugin plugin;
    private final MountManager mountManager;
    private final MongoCollection<Document> collection;

    public MountDataManager(JavaPlugin plugin, MountManager mountManager) {
        this.plugin = plugin;
        this.mountManager = mountManager;

        // Móc nối vào Collection 'player_mounts'
        this.collection = MongoManager.getInstance().getDatabase().getCollection("player_mounts");
    }

    // ===============================================
    // LƯU TOÀN BỘ SERVER (Dùng khi Tắt Server hoặc Auto-save)
    // ===============================================
    public void saveAll(boolean isAsync, boolean removeFromRAM) {
        // [FIX]: Bọc keySet() vào ArrayList để tạo bản sao, tránh ConcurrentModificationException
        for (UUID uuid : new java.util.ArrayList<>(mountManager.getLoadedProfilesMap().keySet())) {
            saveProfile(uuid, isAsync, removeFromRAM);
        }
        if (!isAsync) {
            plugin.getLogger().info("Đã lưu dữ liệu Thú Cưỡi của toàn bộ người chơi!");
        }
    }

    // ===============================================
    // ĐỌC DỮ LIỆU TỪ MONGODB (Chạy đồng bộ lúc Join)
    // ===============================================
    public void loadProfile(UUID uuid) {
        Document doc = collection.find(Filters.eq("_id", uuid.toString())).first();
        PlayerMountProfile profile = new PlayerMountProfile(uuid);

        if (doc != null) {
            profile.setUnlockedSlots(doc.getInteger("unlockedSlots", 1));

            Document mountsDoc = (Document) doc.get("mounts");
            if (mountsDoc != null) {
                for (String key : mountsDoc.keySet()) {
                    int slot = Integer.parseInt(key);
                    Document mDoc = (Document) mountsDoc.get(key);

                    MountArchetype type = MountArchetype.valueOf(mDoc.getString("type"));
                    MountData mData = new MountData(type);
                    mData.setCustomName(mDoc.getString("customName"));
                    mData.setTierSpeed(mDoc.getInteger("tierSpeed", 1));
                    mData.setTierJump(mDoc.getInteger("tierJump", 1));
                    mData.setTierDurability(mDoc.getInteger("tierDurability", 1));
                    mData.setTierCooldown(mDoc.getInteger("tierCooldown", 1));
                    mData.setTierStorage(mDoc.getInteger("tierStorage", 1));
                    mData.setInventoryBase64(mDoc.getString("inventoryBase64"));

                    // Nạp thời gian chết
                    Long deathTime = mDoc.getLong("deathTimestamp");
                    if (deathTime != null) mData.setDeathTimestamp(deathTime);

                    profile.setMountAt(slot, mData);
                }
            }
        }

        mountManager.getLoadedProfilesMap().put(uuid, profile);
    }

    // ===============================================
    // GHI DỮ LIỆU XUỐNG MONGODB (Save An Toàn)
    // ===============================================
    public void saveProfile(UUID uuid, boolean isAsync, boolean removeFromRAM) {
        PlayerMountProfile profile = mountManager.getLoadedProfilesMap().get(uuid);
        if (profile == null) return;

        // 1. Đóng gói danh sách Thú cưỡi
        Document mountsDoc = new Document();
        for (Map.Entry<Integer, MountData> entry : profile.getMounts().entrySet()) {
            MountData m = entry.getValue();
            Document mDoc = new Document("type", m.getType().name())
                    .append("customName", m.getCustomName())
                    .append("tierSpeed", m.getTierSpeed())
                    .append("tierJump", m.getTierJump())
                    .append("tierDurability", m.getTierDurability())
                    .append("tierCooldown", m.getTierCooldown())
                    .append("tierStorage", m.getTierStorage())
                    .append("inventoryBase64", m.getInventoryBase64())
                    .append("deathTimestamp", m.getDeathTimestamp());

            mountsDoc.append(String.valueOf(entry.getKey()), mDoc);
        }

        // 2. Gom vào Document chính
        Document mainDoc = new Document("_id", uuid.toString())
                .append("unlockedSlots", profile.getUnlockedSlots())
                .append("mounts", mountsDoc);

        // 3. Thực thi lưu trữ
        Runnable saveTask = () -> {
            collection.replaceOne(Filters.eq("_id", uuid.toString()), mainDoc, new ReplaceOptions().upsert(true));
        };

        if (isAsync) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run(); // Chạy đồng bộ nếu server đang tắt
        }

        // 4. CHỈ XÓA KHỎI RAM NẾU ĐƯỢC YÊU CẦU (Lúc Quit Game)
        if (removeFromRAM) {
            mountManager.getLoadedProfilesMap().remove(uuid);
        }
    }

    // ===============================================
    // LẮNG NGHE SỰ KIỆN JOIN / QUIT
    // ===============================================
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Quit game -> Lưu Async + Xóa khỏi RAM
        saveProfile(player.getUniqueId(), true, true);
    }
}