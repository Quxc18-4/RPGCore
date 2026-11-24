package vn.minecraft.rPGCore.player;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vn.minecraft.rPGCore.RPGCore;
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
    private final MongoCollection<Document> accountCollection;

    // Hằng số cấu hình ở đầu class PlayerManager
    public static final int MAX_ALLOWED_SLOTS = 10;
    private static final long SLOT_SWITCH_COOLDOWN_MS = 15L * 60L * 1000L; // 15 phút

    public PlayerManager(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        // Móc nối vào Collection 'players' và 'player_accounts' trên MongoDB
        this.collection = MongoManager.getInstance().getDatabase().getCollection("players");
        this.accountCollection = MongoManager.getInstance().getDatabase().getCollection("player_accounts");
    }

    public MongoCollection<Document> getPlayerCollection() {
        return collection;
    }

    public MongoCollection<Document> getAccountCollection() {
        return accountCollection;
    }

    /**
     * Lấy số slot tối đa của tài khoản từ collection player_accounts (mặc định = 2, tối đa = 10)
     */
    public int getMaxSlots(UUID uuid) {
        Document accDoc = accountCollection.find(
                Filters.or(Filters.eq("_id", uuid.toString()), Filters.eq("uuid", uuid.toString()))
        ).first();
        int maxSlots = (accDoc != null) ? getSafeInt(accDoc, "max_slots", 2) : 2;
        return Math.max(2, Math.min(MAX_ALLOWED_SLOTS, maxSlots));
    }

    /**
     * Giá mở khóa (VNĐ) đồng giá 100.000đ cho mỗi Slot từ 3 -> 10
     */
    public int getSlotUnlockCostVnd(int slotNum) {
        if (slotNum <= 2) return 0;
        return 50_000;
    }

    /**
     * Chuyển đổi sang Slot nhân vật mục tiêu (1..10)
     */
    public void switchSlot(Player player, int targetSlot) {
        UUID uuid = player.getUniqueId();
        RPGPlayer rpgPlayer = activePlayers.get(uuid);
        if (rpgPlayer == null) return;

        // 1. Kiểm tra điều kiện trên Main Thread
        if (rpgPlayer.isSwitchingSlot()) {
            player.sendMessage("§cHệ thống đang xử lý chuyển đổi nhân vật, vui lòng chờ giây lát...");
            return;
        }

        if (rpgPlayer.isInCombat()) {
            player.sendMessage("§cBạn không thể đổi nhân vật khi đang trong trạng thái giao tranh!");
            return;
        }

        if (targetSlot == rpgPlayer.getActiveSlot()) {
            player.sendMessage("§eBạn đang sử dụng nhân vật ở Slot " + targetSlot + " rồi.");
            return;
        }

        if (targetSlot < 1 || targetSlot > MAX_ALLOWED_SLOTS) {
            player.sendMessage("§cSlot nhân vật không hợp lệ (Chỉ hỗ trợ từ Slot 1 đến Slot " + MAX_ALLOWED_SLOTS + ").");
            return;
        }

        // Kiểm tra Cooldown 15 phút (Admin/OP được bỏ qua để test)
        if (!player.isOp() && !player.hasPermission("rpgcore.admin")) {
            long elapsed = System.currentTimeMillis() - rpgPlayer.getLastSlotSwitch();
            if (elapsed < SLOT_SWITCH_COOLDOWN_MS) {
                long remainingSec = (SLOT_SWITCH_COOLDOWN_MS - elapsed) / 1000L;
                long minutes = remainingSec / 60L;
                long seconds = remainingSec % 60L;
                player.sendMessage("§cBạn chỉ có thể đổi nhân vật mỗi 15 phút một lần! Vui lòng chờ thêm §e"
                        + minutes + " phút " + seconds + " giây§c.");
                return;
            }
        }

        rpgPlayer.setSwitchingSlot(true);

        // 2. Truy vấn max_slots và dữ liệu targetSlot bất đồng bộ trên MongoDB
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int maxSlots = getMaxSlots(uuid);
                if (targetSlot > maxSlots) {
                    int costVnd = getSlotUnlockCostVnd(targetSlot);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§cBạn chưa mở khóa Slot nhân vật thứ " + targetSlot
                                + " §7(Chi phí mở khóa trên Web: §e" + String.format("%,d", costVnd) + "đ§7)!");
                        rpgPlayer.setSwitchingSlot(false);
                    });
                    return;
                }

                Document playerDoc = collection.find(Filters.eq("_id", uuid.toString())).first();
                Document slotsDoc = (playerDoc != null) ? playerDoc.get("slots", Document.class) : null;
                Document targetSlotDoc = (slotsDoc != null) ? slotsDoc.get(String.valueOf(targetSlot), Document.class) : null;

                // 3. Quay lại Main Thread để lưu slot cũ và nạp slot mới an toàn
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (!player.isOnline()) return;

                        if (rpgPlayer.isInCombat()) {
                            player.sendMessage("§cBạn vừa vào trạng thái giao tranh! Hủy thao tác đổi Slot.");
                            return;
                        }

                        // Bước A: Lưu đồng bộ slot hiện tại vào MongoDB
                        savePlayer(uuid, false, false);

                        // Bước B: Dọn dẹp trạng thái slot cũ & nạp dữ liệu targetSlot
                        applySlotDocument(player, rpgPlayer, targetSlotDoc);

                        // Bước C: Cập nhật active_slot & mốc cooldown 15 phút
                        rpgPlayer.setActiveSlot(targetSlot);
                        rpgPlayer.setLastSlotSwitch(System.currentTimeMillis());
                        rpgPlayer.removeCombat();

                        // Bước D: Nạp lại BossBar hiển thị máu & tài nguyên
                        RPGCore core = RPGCore.getInstance();
                        if (core != null && core.getBossBarManager() != null) {
                            core.getBossBarManager().updateBars(player, rpgPlayer);
                        }

                        // Bước E: Lưu active_slot mới & last_slot_switch lên MongoDB
                        savePlayer(uuid, true, false);

                        if (targetSlotDoc == null || targetSlotDoc.isEmpty()) {
                            player.sendMessage("§a§l[Slot " + targetSlot + "] §fĐã khởi tạo nhân vật tân thủ mới (§eCommoner §7- §bLv.1§f)!");
                        } else {
                            String rawName = rpgPlayer.getCurrentClass() != null
                                    ? rpgPlayer.getCurrentClass().getDisplayName()
                                    : "Commoner";
                            String coloredName = org.bukkit.ChatColor.translateAlternateColorCodes('&', rawName);
                            player.sendMessage("§a§l[Slot " + targetSlot + "] §fĐã chuyển sang nhân vật: " + coloredName + " §7(§bLv." + rpgPlayer.getLevel() + "§7)");
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                    } finally {
                        rpgPlayer.setSwitchingSlot(false);
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().severe("[RPGCore] Lỗi khi chuyển đổi Slot nhân vật cho " + player.getName());
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cĐã xảy ra lỗi khi tải dữ liệu Slot nhân vật. Vui lòng thử lại!");
                    rpgPlayer.setSwitchingSlot(false);
                });
            }
        });
    }

    // --- CƠ CHẾ TẢI DỮ LIỆU KHI VÀO GAME (READ + AUTO MIGRATE) ---
    public RPGPlayer loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        RPGClass defaultClass = classManager.getClassById("commoner");
        RPGPlayer rpgPlayer = new RPGPlayer(player, defaultClass);

        // Truy vấn dữ liệu từ MongoDB (Đọc đồng bộ lúc Join game)
        Document doc = collection.find(Filters.eq("_id", uuid.toString())).first();

        int activeSlot = 1;
        Document slotDoc = null;

        if (doc != null) {
            // Kiểm tra cấu trúc đa Slot; nếu là document phẳng cũ -> tự động migrate vào slots.1
            if (!doc.containsKey("slots") || !(doc.get("slots") instanceof Document)) {
                doc = migrateLegacyPlayerDocument(uuid, player.getName(), doc);
            }

            Object rawActiveSlot = doc.get("active_slot");
            if (rawActiveSlot instanceof Number) {
                activeSlot = Math.max(1, ((Number) rawActiveSlot).intValue());
            }

            Document slotsDoc = doc.get("slots", Document.class);
            if (slotsDoc != null) {
                slotDoc = slotsDoc.get(String.valueOf(activeSlot), Document.class);
            }
        }

        rpgPlayer.setActiveSlot(activeSlot);

        if (doc != null) {
            rpgPlayer.setLastSlotSwitch(getSafeLong(doc, "last_slot_switch", 0L));
        }

        applySlotDocument(player, rpgPlayer, slotDoc);

        player.getWorld().setGameRule(org.bukkit.GameRules.KEEP_INVENTORY, true);

        activePlayers.put(uuid, rpgPlayer);
        return rpgPlayer;
    }

    /**
     * Chuyển đổi document cấu trúc cũ (phẳng) sang cấu trúc mới (active_slot + slots.1)
     */
    private Document migrateLegacyPlayerDocument(UUID uuid, String fallbackName, Document legacyDoc) {
        Document slot1 = new Document()
                .append("class", legacyDoc.getString("class") != null ? legacyDoc.getString("class") : "commoner")
                .append("level", getSafeInt(legacyDoc, "level", 1))
                .append("resource", getSafeDouble(legacyDoc, "resource", 100.0))
                .append("exp", getSafeDouble(legacyDoc, "exp", 0.0))
                .append("balance", getSafeLong(legacyDoc, "balance", 0L))
                .append("attributes", legacyDoc.get("attributes"))
                .append("elements", legacyDoc.get("elements"))
                .append("trinkets", legacyDoc.get("trinkets"))
                .append("skills_data", legacyDoc.get("skills_data"))
                .append("vanilla_inventory", legacyDoc.getString("vanilla_inventory") != null ? legacyDoc.getString("vanilla_inventory") : "")
                .append("ender_chest", legacyDoc.getString("ender_chest") != null ? legacyDoc.getString("ender_chest") : "");

        String username = legacyDoc.getString("username") != null ? legacyDoc.getString("username") : fallbackName;

        Document migratedDoc = new Document("_id", uuid.toString())
                .append("username", username)
                .append("active_slot", 1)
                .append("slots", new Document("1", slot1));

        collection.replaceOne(Filters.eq("_id", uuid.toString()), migratedDoc, new ReplaceOptions().upsert(true));
        plugin.getLogger().info("[RPGCore] Đã tự động migrate dữ liệu nhân vật sang cấu trúc đa Slot cho: " + username);
        return migratedDoc;
    }

    /**
     * Nạp dữ liệu của 1 Slot cụ thể vào đối tượng RPGPlayer trên RAM
     */
    public void applySlotDocument(Player player, RPGPlayer rpgPlayer, Document doc) {
        RPGClass defaultClass = classManager.getClassById("commoner");

        // Dọn dẹp trạng thái cũ trước khi nạp slot mới
        rpgPlayer.cleanUpClassData();
        rpgPlayer.getAttributeData().initDefaultAttributes();
        rpgPlayer.getAttributeData().setUnspentAttrPoints(0);
        for (String attr : new String[]{"STR", "INT", "VIT", "AGI", "LUK"}) {
            rpgPlayer.getAttributeData().setAttribute(attr, 0);
        }
        rpgPlayer.getSkillData().clearAllSkills();
        rpgPlayer.getSkillData().clearAllBinds();
        rpgPlayer.setUnlockedElements(new ArrayList<>());
        for (int i = 0; i < 4; i++) {
            rpgPlayer.setTrinket(i, null);
        }

        if (doc != null) {
            String classId = doc.getString("class");
            double savedResource = getSafeDouble(doc, "resource", defaultClass.getMaxResource());
            double savedExp = getSafeDouble(doc, "exp", 0.0);
            int savedLevel = getSafeInt(doc, "level", 1);
            long savedBalance = getSafeLong(doc, "balance", 0L);

            rpgPlayer.setBalance(savedBalance);
            rpgPlayer.setLevel(savedLevel);
            rpgPlayer.setCurrentExp(savedExp);

            RPGClass savedClass = classManager.getClassById(classId);
            if (savedClass != null) {
                rpgPlayer.setClass(savedClass);
                rpgPlayer.setCurrentResource(savedResource);
            } else {
                rpgPlayer.setClass(defaultClass);
            }

            // Nạp Attribute
            Document attrDoc = doc.get("attributes", Document.class);
            if (attrDoc != null) {
                rpgPlayer.getAttributeData().setUnspentAttrPoints(getSafeInt(attrDoc, "unspent", 0));
                rpgPlayer.getAttributeData().setAttribute("STR", getSafeInt(attrDoc, "STR", 0));
                rpgPlayer.getAttributeData().setAttribute("INT", getSafeInt(attrDoc, "INT", 0));
                rpgPlayer.getAttributeData().setAttribute("VIT", getSafeInt(attrDoc, "VIT", 0));
                rpgPlayer.getAttributeData().setAttribute("AGI", getSafeInt(attrDoc, "AGI", 0));
                rpgPlayer.getAttributeData().setAttribute("LUK", getSafeInt(attrDoc, "LUK", 0));
            }

            // Nạp Nguyên tố (Mage)
            List<String> elements = doc.getList("elements", String.class);
            rpgPlayer.setUnlockedElements(elements != null ? new ArrayList<>(elements) : new ArrayList<>());

            // Nạp Trang sức
            Document trinketsDoc = doc.get("trinkets", Document.class);
            if (trinketsDoc != null) {
                for (int i = 0; i < 4; i++) {
                    String base64Item = trinketsDoc.getString(String.valueOf(i));
                    if (base64Item != null && !base64Item.isEmpty()) {
                        rpgPlayer.setTrinket(i, itemFromBase64(base64Item));
                    }
                }
            }

            // Nạp Kỹ năng & Combo
            Document skillsDoc = doc.get("skills_data", Document.class);
            if (skillsDoc != null) {
                rpgPlayer.getSkillData().setSkillPoints(getSafeInt(skillsDoc, "sp", 0));

                Document levels = skillsDoc.get("levels", Document.class);
                if (levels != null) {
                    for (String skillId : levels.keySet()) {
                        rpgPlayer.getSkillData().setSkillLevel(skillId, getSafeInt(levels, skillId, 0));
                    }
                }

                Document binds = skillsDoc.get("binds", Document.class);
                if (binds != null) {
                    for (String combo : binds.keySet()) {
                        rpgPlayer.getSkillData().bindCombo(combo, binds.getString(combo));
                    }
                }
            }

            // Phục hồi Túi đồ
            String invData = doc.getString("vanilla_inventory");
            if (invData != null && !invData.isEmpty()) {
                ItemStack[] restoredInv = itemArrayFromBase64(invData);
                if (restoredInv.length > 0) {
                    player.getInventory().setContents(restoredInv);
                } else {
                    player.getInventory().clear();
                }
            } else {
                player.getInventory().clear();
            }

            // Phục hồi Ender Chest
            String ecData = doc.getString("ender_chest");
            if (ecData != null && !ecData.isEmpty()) {
                ItemStack[] restoredEc = itemArrayFromBase64(ecData);
                if (restoredEc.length > 0) {
                    player.getEnderChest().setContents(restoredEc);
                } else {
                    player.getEnderChest().clear();
                }
            } else {
                player.getEnderChest().clear();
            }
        } else {
            // Khởi tạo nhân vật mới hoàn toàn cho Slot trống
            player.getInventory().clear();
            player.getEnderChest().clear();
            rpgPlayer.setLevel(1);
            rpgPlayer.setCurrentExp(0.0);
            rpgPlayer.setBalance(0L);
            rpgPlayer.getSkillData().setSkillPoints(1);
            rpgPlayer.setClass(defaultClass);
        }

        rpgPlayer.updateMaxStats();
        rpgPlayer.updateExpBar();
        rpgPlayer.updateEquipmentStats();
    }

    // --- ĐÓNG GÓI DỮ LIỆU CỦA 1 SLOT THÀNH DOCUMENT ---
    public Document buildSlotDocument(RPGPlayer rpgPlayer) {
        Document attrDoc = new Document("unspent", rpgPlayer.getAttributeData().getUnspentAttrPoints())
                .append("STR", rpgPlayer.getAttributeData().getAttribute("STR"))
                .append("INT", rpgPlayer.getAttributeData().getAttribute("INT"))
                .append("VIT", rpgPlayer.getAttributeData().getAttribute("VIT"))
                .append("AGI", rpgPlayer.getAttributeData().getAttribute("AGI"))
                .append("LUK", rpgPlayer.getAttributeData().getAttribute("LUK"));

        Document trinketsDoc = new Document();
        for (int i = 0; i < 4; i++) {
            ItemStack trinket = rpgPlayer.getTrinket(i);
            if (trinket != null) {
                trinketsDoc.append(String.valueOf(i), itemToBase64(trinket));
            }
        }

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

        Player bukkitPlayer = rpgPlayer.getBukkitPlayer();
        String inventoryBase64 = "";
        String enderChestBase64 = "";

        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            inventoryBase64 = itemArrayToBase64(bukkitPlayer.getInventory().getContents());
            enderChestBase64 = itemArrayToBase64(bukkitPlayer.getEnderChest().getContents());
        }

        return new Document("class", rpgPlayer.getCurrentClass().getId())
                .append("level", rpgPlayer.getLevel())
                .append("resource", rpgPlayer.getCurrentResource())
                .append("exp", rpgPlayer.getCurrentExp())
                .append("balance", rpgPlayer.getBalance())
                .append("attributes", attrDoc)
                .append("elements", rpgPlayer.getUnlockedElements())
                .append("trinkets", trinketsDoc)
                .append("skills_data", skillsDoc)
                .append("vanilla_inventory", inventoryBase64)
                .append("ender_chest", enderChestBase64);
    }

    // --- CƠ CHẾ LƯU DỮ LIỆU THEO ACTIVE SLOT ---
    public void savePlayer(UUID uuid, boolean isAsync, boolean removeFromRAM) {
        RPGPlayer rpgPlayer = activePlayers.get(uuid);
        if (rpgPlayer != null) {
            int slot = rpgPlayer.getActiveSlot();
            Document slotDoc = buildSlotDocument(rpgPlayer);

            Player bukkitPlayer = rpgPlayer.getBukkitPlayer();
            String playerName = (bukkitPlayer != null && bukkitPlayer.isOnline()) ? bukkitPlayer.getName() : "Unknown";

            Bson updates = Updates.combine(
                    Updates.set("username", playerName),
                    Updates.set("active_slot", slot),
                    Updates.set("last_slot_switch", rpgPlayer.getLastSlotSwitch()),
                    Updates.set("slots." + slot, slotDoc)
            );
            UpdateOptions options = new UpdateOptions().upsert(true);

            if (isAsync) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    collection.updateOne(Filters.eq("_id", uuid.toString()), updates, options);
                });
            } else {
                collection.updateOne(Filters.eq("_id", uuid.toString()), updates, options);
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

    private int getSafeInt(Document doc, String key, int defaultValue) {
        Object val = doc.get(key);
        return (val instanceof Number) ? ((Number) val).intValue() : defaultValue;
    }

    private long getSafeLong(Document doc, String key, long defaultValue) {
        Object val = doc.get(key);
        return (val instanceof Number) ? ((Number) val).longValue() : defaultValue;
    }

    private double getSafeDouble(Document doc, String key, double defaultValue) {
        Object val = doc.get(key);
        return (val instanceof Number) ? ((Number) val).doubleValue() : defaultValue;
    }

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