package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCManager {
    private final RPGCore plugin;
    private final MongoCollection<Document> collection;
    private final Map<String, ShopNPC> npcs = new HashMap<>();
    public final NamespacedKey NPC_SHOP_KEY;

    public static class ShopNPC {
        public String id;
        public String name;
        public EntityType type;
        public Location location;
        public String shopId;
        public UUID currentEntityId;

        public ShopNPC(String id, String name, EntityType type, Location location, String shopId) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.location = location;
            this.shopId = shopId;
        }
    }

    public NPCManager(RPGCore plugin) {
        this.plugin = plugin;
        this.collection = MongoManager.getInstance().getDatabase().getCollection("shop_npcs");
        this.NPC_SHOP_KEY = new NamespacedKey(plugin, "npc_shop_id");

        loadNPCs();
        startTrackerTask();
    }

    private void loadNPCs() {
        npcs.clear();
        for (Document doc : collection.find()) {
            String id = doc.getString("_id");
            String name = doc.getString("name");
            EntityType type = EntityType.valueOf(doc.getString("entity_type"));
            String shopId = doc.getString("shop_id");

            Document locDoc = (Document) doc.get("location");
            World world = Bukkit.getWorld(locDoc.getString("world"));
            if (world == null) continue; // Bỏ qua nếu World chưa được load

            Location loc = new Location(
                    world, locDoc.getDouble("x"), locDoc.getDouble("y"), locDoc.getDouble("z"),
                    locDoc.getDouble("yaw").floatValue(), locDoc.getDouble("pitch").floatValue()
            );

            ShopNPC npc = new ShopNPC(id, name, type, loc, shopId);
            String uuidStr = doc.getString("current_entity_uuid");
            if (uuidStr != null && !uuidStr.isEmpty()) {
                npc.currentEntityId = UUID.fromString(uuidStr);
            }
            npcs.put(id, npc);
        }
        plugin.getLogger().info("Đã tải " + npcs.size() + " NPC Shop từ MongoDB.");
    }

    public void saveNPC(ShopNPC npc) {
        Document locDoc = new Document("world", npc.location.getWorld().getName())
                .append("x", npc.location.getX())
                .append("y", npc.location.getY())
                .append("z", npc.location.getZ())
                .append("yaw", npc.location.getYaw())
                .append("pitch", npc.location.getPitch());

        Document doc = new Document("_id", npc.id)
                .append("name", npc.name)
                .append("entity_type", npc.type.name())
                .append("shop_id", npc.shopId)
                .append("location", locDoc)
                .append("current_entity_uuid", npc.currentEntityId != null ? npc.currentEntityId.toString() : "");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            collection.replaceOne(Filters.eq("_id", npc.id), doc, new ReplaceOptions().upsert(true));
        });
    }

    // Hàm dùng cho Lệnh /spawnnpc sau này
    public void createNPC(String id, String name, EntityType type, Location loc, String shopId) {
        ShopNPC npc = new ShopNPC(id, name, type, loc, shopId);
        npcs.put(id, npc);
        spawnNPC(npc);
    }

    public void spawnNPC(ShopNPC npc) {
        // Dọn dẹp xác cũ nếu còn kẹt lại
        if (npc.currentEntityId != null) {
            Entity oldEntity = Bukkit.getEntity(npc.currentEntityId);
            if (oldEntity != null) oldEntity.remove();
        }

        Entity entity = npc.location.getWorld().spawnEntity(npc.location, npc.type);
        entity.setCustomName(npc.name);
        entity.setCustomNameVisible(true);

        // Gắn thẻ ID Shop ẩn vào người NPC
        entity.getPersistentDataContainer().set(NPC_SHOP_KEY, PersistentDataType.STRING, npc.shopId);

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            living.setAI(false); // Đứng yên
            living.setInvulnerable(true); // Bất tử
            living.setRemoveWhenFarAway(false); // Không bị despawn khi người chơi đi xa
            living.setCollidable(false); // Không bị đẩy lùi
        }

        npc.currentEntityId = entity.getUniqueId();
        saveNPC(npc); // Lưu lại UUID mới xuống DB
    }

    private void startTrackerTask() {
        // Chạy kiểm tra định kỳ mỗi 10 giây (200 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (ShopNPC npc : npcs.values()) {
                if (npc.currentEntityId == null) {
                    spawnNPC(npc);
                    continue;
                }

                Entity entity = Bukkit.getEntity(npc.currentEntityId);

                if (entity == null || entity.isDead()) {
                    // Cảnh báo: NPC chết hoặc bị mất tích -> Hồi sinh!
                    spawnNPC(npc);
                } else {
                    // NPC còn sống, check xem có đi dạo lệch quá 2 block không
                    if (entity.getLocation().distanceSquared(npc.location) > 4.0) {
                        entity.teleport(npc.location);
                    }
                }
            }
        }, 200L, 200L);
    }
}