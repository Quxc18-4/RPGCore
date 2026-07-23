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

        // [NÂNG CẤP]: Liên kết sở hữu và lãnh thổ
        public UUID ownerId;
        public UUID waystoneId;

        public ShopNPC(String id, String name, EntityType type, Location location, String shopId, UUID ownerId, UUID waystoneId) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.location = location;
            this.shopId = shopId;
            this.ownerId = ownerId;
            this.waystoneId = waystoneId;
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
            if (world == null) continue;

            Location loc = new Location(
                    world, locDoc.getDouble("x"), locDoc.getDouble("y"), locDoc.getDouble("z"),
                    locDoc.getDouble("yaw").floatValue(), locDoc.getDouble("pitch").floatValue()
            );

            // [NÂNG CẤP]: Đọc dữ liệu Owner và Waystone
            String ownerStr = doc.getString("owner_id");
            UUID ownerId = (ownerStr != null && !ownerStr.isEmpty()) ? UUID.fromString(ownerStr) : null;

            String waystoneStr = doc.getString("waystone_id");
            UUID waystoneId = (waystoneStr != null && !waystoneStr.isEmpty()) ? UUID.fromString(waystoneStr) : null;

            ShopNPC npc = new ShopNPC(id, name, type, loc, shopId, ownerId, waystoneId);
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
                .append("current_entity_uuid", npc.currentEntityId != null ? npc.currentEntityId.toString() : "")
                // [NÂNG CẤP]: Lưu dữ liệu Owner và Waystone
                .append("owner_id", npc.ownerId != null ? npc.ownerId.toString() : "")
                .append("waystone_id", npc.waystoneId != null ? npc.waystoneId.toString() : "");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            collection.replaceOne(Filters.eq("_id", npc.id), doc, new ReplaceOptions().upsert(true));
        });
    }

    // [GIỮ NGUYÊN]: Dùng cho lệnh /spawnnpc của Admin (Chủ sở hữu là Server)
    public void createNPC(String id, String name, EntityType type, Location loc, String shopId) {
        ShopNPC npc = new ShopNPC(id, name, type, loc, shopId, null, null);
        npcs.put(id, npc);
        spawnNPC(npc);
    }

    // [MỚI]: Dùng cho hệ thống Waystone
    public void createPlayerNPC(String id, String name, EntityType type, Location loc, String shopId, UUID ownerId, UUID waystoneId) {
        ShopNPC npc = new ShopNPC(id, name, type, loc, shopId, ownerId, waystoneId);
        npcs.put(id, npc);
        spawnNPC(npc);
    }

    public void spawnNPC(ShopNPC npc) {
        if (npc.currentEntityId != null) {
            Entity oldEntity = Bukkit.getEntity(npc.currentEntityId);
            if (oldEntity != null) oldEntity.remove();
        }

        Entity entity = npc.location.getWorld().spawnEntity(npc.location, npc.type);
        entity.setCustomName(npc.name);
        entity.setCustomNameVisible(true);

        entity.getPersistentDataContainer().set(NPC_SHOP_KEY, PersistentDataType.STRING, npc.shopId);

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            living.setAI(false);
            living.setInvulnerable(true);
            living.setRemoveWhenFarAway(false);
            living.setCollidable(false);
        }

        npc.currentEntityId = entity.getUniqueId();
        saveNPC(npc);
    }

    // [MỚI]: API Xử lý tìm kiếm và Xóa NPC
    public ShopNPC getNPCByEntityUUID(UUID entityId) {
        for (ShopNPC npc : npcs.values()) {
            if (entityId.equals(npc.currentEntityId)) {
                return npc;
            }
        }
        return null;
    }

    public void removeNPC(String npcId) {
        ShopNPC npc = npcs.remove(npcId);
        if (npc != null) {
            if (npc.currentEntityId != null) {
                Entity entity = Bukkit.getEntity(npc.currentEntityId);
                if (entity != null) entity.remove();
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                collection.deleteOne(Filters.eq("_id", npcId));
            });
        }
    }

    private void startTrackerTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (ShopNPC npc : npcs.values()) {
                if (npc.currentEntityId == null) {
                    spawnNPC(npc);
                    continue;
                }
                Entity entity = Bukkit.getEntity(npc.currentEntityId);
                if (entity == null || entity.isDead()) {
                    spawnNPC(npc);
                } else {
                    if (entity.getLocation().distanceSquared(npc.location) > 4.0) {
                        entity.teleport(npc.location);
                    }
                }
            }
        }, 200L, 200L);
    }
    public ShopNPC getNPCById(String id) {
        return npcs.get(id);
    }
}