package vn.minecraft.rPGCore.player;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.database.MongoManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    // Chuyển sang ConcurrentHashMap để an toàn tuyệt đối khi Multi-threading
    private final Map<UUID, RPGPlayer> activePlayers = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final ClassManager classManager;
    private final MongoCollection<Document> collection;

    public PlayerManager(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        // Móc nối vào Collection 'players' trên MongoDB
        this.collection = MongoManager.getInstance().getDatabase().getCollection("players");
    }

    // --- CƠ CHẾ TẢI DỮ LIỆU KHI VÀO GAME (READ) ---
    public RPGPlayer loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        RPGClass defaultClass = classManager.getClassById("commoner");
        RPGPlayer rpgPlayer = new RPGPlayer(player, defaultClass);

        // Truy vấn dữ liệu từ MongoDB (Đọc đồng bộ lúc Join game)
        Document doc = collection.find(Filters.eq("_id", uuid.toString())).first();

        if (doc != null) {
            String classId = doc.getString("class");
            double savedResource = doc.getDouble("resource");
            double savedExp = doc.getDouble("exp") != null ? doc.getDouble("exp") : 0.0;

            // [ĐỒNG BỘ LEVEL]: Lấy dữ liệu Level từ DB
            int savedLevel = doc.getInteger("level") != null ? doc.getInteger("level") : 1;

            long savedBalance = doc.getLong("balance") != null ? doc.getLong("balance") : 0L;

            rpgPlayer.setBalance(savedBalance);

            RPGClass savedClass = classManager.getClassById(classId);
            if (savedClass != null) {
                rpgPlayer.setClass(savedClass);
                rpgPlayer.setCurrentResource(savedResource);
            }

            rpgPlayer.setCurrentExp(savedExp);
            // [ĐỒNG BỘ LEVEL]: Bơm Level vào cho người chơi
            rpgPlayer.setLevel(savedLevel);

            // Nạp dữ liệu Attribute
            Document attrDoc = (Document) doc.get("attributes");
            if (attrDoc != null) {
                rpgPlayer.getAttributeData().setUnspentAttrPoints(attrDoc.getInteger("unspent", 0));
                rpgPlayer.getAttributeData().setAttribute("STR", attrDoc.getInteger("STR", 0));
                rpgPlayer.getAttributeData().setAttribute("INT", attrDoc.getInteger("INT", 0));
                rpgPlayer.getAttributeData().setAttribute("VIT", attrDoc.getInteger("VIT", 0));
                rpgPlayer.getAttributeData().setAttribute("AGI", attrDoc.getInteger("AGI", 0));
                rpgPlayer.getAttributeData().setAttribute("LUK", attrDoc.getInteger("LUK", 0));
            }

            // Nạp dữ liệu Nguyên tố (Dành cho Mage)
            List<String> elements = doc.getList("elements", String.class);
            if (elements != null) {
                rpgPlayer.setUnlockedElements(elements);
            }

            // Nạp dữ liệu Trang sức (Giải mã từ Base64)
            Document trinketsDoc = (Document) doc.get("trinkets");
            if (trinketsDoc != null) {
                for (int i = 0; i < 4; i++) {
                    String base64Item = trinketsDoc.getString(String.valueOf(i));
                    if (base64Item != null && !base64Item.isEmpty()) {
                        rpgPlayer.setTrinket(i, itemFromBase64(base64Item));
                    }
                }
            }

            // Nạp dữ liệu Kỹ năng & Combo
            Document skillsDoc = (Document) doc.get("skills_data");
            if (skillsDoc != null) {
                rpgPlayer.getSkillData().setSkillPoints(skillsDoc.getInteger("sp", 0));

                Document levels = (Document) skillsDoc.get("levels");
                if (levels != null) {
                    for (String skillId : levels.keySet()) {
                        rpgPlayer.getSkillData().setSkillLevel(skillId, levels.getInteger(skillId));
                    }
                }

                Document binds = (Document) skillsDoc.get("binds");
                if (binds != null) {
                    for (String combo : binds.keySet()) {
                        rpgPlayer.getSkillData().bindCombo(combo, binds.getString(combo));
                    }
                }
            }
            // [MỚI CHÈN]: Phục hồi Túi đồ (Inventory + Armor + Offhand)
            String invData = doc.getString("vanilla_inventory");
            if (invData != null && !invData.isEmpty()) {
                ItemStack[] restoredInv = itemArrayFromBase64(invData);
                if (restoredInv.length > 0) {
                    player.getInventory().setContents(restoredInv);
                }
            } else {
                // Đảm bảo túi đồ trống nếu DB không có (Phòng trường hợp file World còn giữ đồ rác)
                player.getInventory().clear();
            }

            // [MỚI CHÈN]: Phục hồi Ender Chest
            String ecData = doc.getString("ender_chest");
            if (ecData != null && !ecData.isEmpty()) {
                ItemStack[] restoredEc = itemArrayFromBase64(ecData);
                if (restoredEc.length > 0) {
                    player.getEnderChest().setContents(restoredEc);
                }
            } else {
                player.getEnderChest().clear();
            }
        } else {
            // Nếu là Tân thủ (Chưa có trên DB), xóa sạch túi đồ để tránh đồ rác từ World cũ
            player.getInventory().clear();
            player.getEnderChest().clear();
            rpgPlayer.setClass(defaultClass);
        }

        rpgPlayer.updateMaxStats();
        rpgPlayer.updateExpBar();
        rpgPlayer.updateEquipmentStats();

        // ==========================================
        // [AUTO KEEP INVENTORY]: Ép luật bảo vệ đồ cho World hiện tại
        // ==========================================
        player.getWorld().setGameRule(org.bukkit.GameRules.KEEP_INVENTORY, true);

        activePlayers.put(uuid, rpgPlayer);
        return rpgPlayer;
    }

    // --- CƠ CHẾ LƯU DỮ LIỆU KHI THOÁT GAME (CREATE/UPDATE) ---
    public void savePlayer(UUID uuid, boolean isAsync, boolean removeFromRAM) {
        RPGPlayer rpgPlayer = activePlayers.get(uuid);
        if (rpgPlayer != null) {

            // 1. Đóng gói Attribute
            Document attrDoc = new Document("unspent", rpgPlayer.getAttributeData().getUnspentAttrPoints())
                    .append("STR", rpgPlayer.getAttributeData().getAttribute("STR"))
                    .append("INT", rpgPlayer.getAttributeData().getAttribute("INT"))
                    .append("VIT", rpgPlayer.getAttributeData().getAttribute("VIT"))
                    .append("AGI", rpgPlayer.getAttributeData().getAttribute("AGI"))
                    .append("LUK", rpgPlayer.getAttributeData().getAttribute("LUK"));

            // 2. Đóng gói Trang sức (Mã hóa ra Base64)
            Document trinketsDoc = new Document();
            for (int i = 0; i < 4; i++) {
                ItemStack trinket = rpgPlayer.getTrinket(i);
                if (trinket != null) {
                    trinketsDoc.append(String.valueOf(i), itemToBase64(trinket));
                }
            }

            // 3. Đóng gói Kỹ năng và Combo
            Document levelsDoc = new Document();
            for (Map.Entry<String, Integer> entry : rpgPlayer.getSkillData().getSkills().entrySet()) {
                levelsDoc.append(entry.getKey(), entry.getValue());
            }

            Document bindsDoc = new Document();
            for (Map.Entry<String, String> entry : rpgPlayer.getSkillData().getBinds().entrySet()) {
                bindsDoc.append(entry.getKey(), entry.getValue());
            }

            Document skillsDoc = new Document("sp", rpgPlayer.getSkillData().getSkillPoints())
                    .append("levels", levelsDoc)
                    .append("binds", bindsDoc);

            // 4. Lấy Túi Đồ và Ender Chest
            Player bukkitPlayer = rpgPlayer.getBukkitPlayer();
            String inventoryBase64 = "";
            String enderChestBase64 = "";

            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                inventoryBase64 = itemArrayToBase64(bukkitPlayer.getInventory().getContents());
                enderChestBase64 = itemArrayToBase64(bukkitPlayer.getEnderChest().getContents());
            }

            String playerName = (bukkitPlayer != null && bukkitPlayer.isOnline()) ? bukkitPlayer.getName() : "Unknown";
            // 5. Gộp tất cả vào Document Chính
            Document mainDoc = new Document("_id", uuid.toString())
                    .append("username", playerName)
                    .append("class", rpgPlayer.getCurrentClass().getId())
                    .append("level", rpgPlayer.getLevel()) // [ĐỒNG BỘ LEVEL]: Lưu xuống DB
                    .append("resource", rpgPlayer.getCurrentResource())
                    .append("exp", rpgPlayer.getCurrentExp())
                    .append("balance", rpgPlayer.getBalance())
                    .append("attributes", attrDoc)
                    .append("elements", rpgPlayer.getUnlockedElements())
                    .append("trinkets", trinketsDoc)
                    .append("skills_data", skillsDoc)
                    .append("vanilla_inventory", inventoryBase64)
                    .append("ender_chest", enderChestBase64);

            // 6. XỬ LÝ LƯU (ĐỒNG BỘ HOẶC BẤT ĐỒNG BỘ)
            if (isAsync) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    collection.replaceOne(Filters.eq("_id", uuid.toString()), mainDoc, new ReplaceOptions().upsert(true));
                });
            } else {
                // Chạy đồng bộ: Bắt buộc server chờ lưu xong mới được tắt
                collection.replaceOne(Filters.eq("_id", uuid.toString()), mainDoc, new ReplaceOptions().upsert(true));
            }
        }

        // [QUAN TRỌNG]: Tách lệnh xóa RAM ra khỏi quá trình lưu
        if (removeFromRAM) {
            activePlayers.remove(uuid);
        }
    }

    public void saveAllPlayers(boolean isAsync) {
        for (UUID uuid : new ArrayList<>(activePlayers.keySet())) {
            // Khi server Auto-save (5 phút/lần), ép tham số thứ 3 là FALSE để không bị xóa nhân vật
            savePlayer(uuid, isAsync, false);
        }
    }

    public RPGPlayer getRPGPlayer(UUID uuid) { return activePlayers.get(uuid); }
    public Set<UUID> getAllPlayerUUIDs() { return activePlayers.keySet(); }

    // ============================================================
    // CÔNG CỤ CHUYỂN ĐỔI ITEMSTACK <-> CHUỖI BASE64 CHO MONGODB
    // ============================================================
    private String itemToBase64(ItemStack item) {
        if (item == null) return "";
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi mã hóa Item Trinket sang Base64!");
            return "";
        }
    }

    private ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi giải mã Item Trinket từ Base64!");
            return null;
        }
    }

    // ============================================================
    // CÔNG CỤ CHUYỂN ĐỔI TOÀN BỘ TÚI ĐỒ (ARRAY) <-> CHUỖI BASE64
    // ============================================================
    private String itemArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi mã hóa Túi đồ sang Base64!");
            return "";
        }
    }

    private ItemStack[] itemArrayFromBase64(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi giải mã Túi đồ từ Base64!");
            return new ItemStack[0];
        }
    }
}