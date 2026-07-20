package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.database.MongoManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RewardManager {
    private final RPGCore plugin;
    private final MongoCollection<Document> templatesCollection;
    private final MongoCollection<Document> playersCollection;

    // Class đại diện cho 1 gói Kit độc lập
    public static class CustomKit {
        public ItemStack[] items;
        public long cooldownSeconds;

        public CustomKit(ItemStack[] items, long cooldownSeconds) {
            this.items = items;
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    private final Map<String, CustomKit> customKits = new ConcurrentHashMap<>();
    private final Map<Integer, ItemStack[]> monthlyRewards = new ConcurrentHashMap<>();

    public RewardManager(RPGCore plugin) {
        this.plugin = plugin;
        this.templatesCollection = MongoManager.getInstance().getDatabase().getCollection("reward_templates");
        this.playersCollection = MongoManager.getInstance().getDatabase().getCollection("player_rewards");

        loadTemplatesFromDB();
    }

    private void loadTemplatesFromDB() {
        customKits.clear();
        monthlyRewards.clear();

        for (Document doc : templatesCollection.find()) {
            String id = doc.getString("_id");
            String base64 = doc.getString("items");
            ItemStack[] items = itemArrayFromBase64(base64);

            if (id.startsWith("kit_")) {
                String kitName = id.replace("kit_", "");
                // Mặc định là 6 tiếng (21600s) nếu DB cũ chưa có trường này
                long cooldown = doc.getLong("cooldown") != null ? doc.getLong("cooldown") : 21600L;
                customKits.put(kitName, new CustomKit(items, cooldown));
            } else if (id.startsWith("monthly_")) {
                int day = Integer.parseInt(id.replace("monthly_", ""));
                monthlyRewards.put(day, items);
            }
        }
        plugin.getLogger().info("Đã tải " + customKits.size() + " Kits và " + monthlyRewards.size() + " ngày Điểm Danh.");
    }

    public boolean createKit(String name, long cooldownSeconds) {
        String safeName = name.toLowerCase();
        if (customKits.containsKey(safeName)) return false;

        customKits.put(safeName, new CustomKit(new ItemStack[0], cooldownSeconds));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document doc = new Document("_id", "kit_" + safeName)
                    .append("items", "")
                    .append("cooldown", cooldownSeconds);
            templatesCollection.replaceOne(Filters.eq("_id", "kit_" + safeName), doc, new ReplaceOptions().upsert(true));
        });
        return true;
    }

    public boolean setKitCooldown(String name, long cooldownSeconds) {
        String safeName = name.toLowerCase();
        if (!customKits.containsKey(safeName)) return false;

        CustomKit kit = customKits.get(safeName);
        kit.cooldownSeconds = cooldownSeconds;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            templatesCollection.updateOne(Filters.eq("_id", "kit_" + safeName), new Document("$set", new Document("cooldown", cooldownSeconds)));
        });
        return true;
    }

    public void saveCustomKit(String name, Inventory inv) {
        String safeName = name.toLowerCase();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) items.add(item.clone());
        }

        ItemStack[] array = items.toArray(new ItemStack[0]);
        long currentCooldown = customKits.containsKey(safeName) ? customKits.get(safeName).cooldownSeconds : 21600L;

        customKits.put(safeName, new CustomKit(array, currentCooldown));
        String base64 = itemArrayToBase64(array);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document doc = new Document("_id", "kit_" + safeName)
                    .append("items", base64)
                    .append("cooldown", currentCooldown);
            templatesCollection.replaceOne(Filters.eq("_id", "kit_" + safeName), doc, new ReplaceOptions().upsert(true));
        });
    }

    public Set<String> getKitNames() { return customKits.keySet(); }
    public CustomKit getCustomKit(String name) { return customKits.get(name.toLowerCase()); }

    // [Các hàm Monthly giữ nguyên như cũ...]
    public void saveMonthlyReward(int day, Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            if (i == 9 || i == 17) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) items.add(item.clone());
        }
        ItemStack[] array = items.toArray(new ItemStack[0]);
        monthlyRewards.put(day, array);
        String base64 = itemArrayToBase64(array);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            templatesCollection.replaceOne(Filters.eq("_id", "monthly_" + day), new Document("_id", "monthly_" + day).append("items", base64), new ReplaceOptions().upsert(true));
        });
    }

    public Inventory getMonthlyAdminGUI(int day) {
        Inventory inv = Bukkit.createInventory(null, 18, "§0§lEdit Monthly: " + day);
        ItemStack[] items = monthlyRewards.get(day);
        if (items != null) {
            int slot = 0;
            for (ItemStack item : items) {
                if (slot == 9) slot++;
                if (slot < 18 && slot != 17) { inv.setItem(slot, item.clone()); slot++; }
            }
        }
        if (day > 1) {
            ItemStack prev = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = prev.getItemMeta(); meta.setDisplayName("§c⬅ Quay lại Ngày " + (day - 1)); prev.setItemMeta(meta); inv.setItem(9, prev);
        }
        if (day < 31) {
            ItemStack next = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta meta = next.getItemMeta(); meta.setDisplayName("§aTiến tới Ngày " + (day + 1) + " ➡"); next.setItemMeta(meta); inv.setItem(17, next);
        }
        return inv;
    }

    public void claimKit(Player player, String kitName) {
        String safeName = kitName.toLowerCase();
        if (!customKits.containsKey(safeName)) {
            player.sendMessage("§c§l[!] §cKit này không tồn tại!");
            return;
        }

        CustomKit kit = customKits.get(safeName);
        if (kit.items.length == 0) {
            player.sendMessage("§c§l[!] §cKit này chưa có phần thưởng!");
            return;
        }

        UUID uuid = player.getUniqueId();
        long cooldownTimeMillis = kit.cooldownSeconds * 1000L;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document doc = playersCollection.find(Filters.eq("_id", uuid.toString())).first();
            Document kitsDoc = (doc != null && doc.get("kits") != null) ? (Document) doc.get("kits") : new Document();

            long lastClaim = (kitsDoc.getLong(safeName) != null) ? kitsDoc.getLong(safeName) : 0L;
            long now = System.currentTimeMillis();

            if (now - lastClaim < cooldownTimeMillis) {
                long timeLeft = cooldownTimeMillis - (now - lastClaim);
                long days = timeLeft / 86400000L;
                long hours = (timeLeft % 86400000L) / 3600000L;
                long minutes = (timeLeft % 3600000L) / 60000L;
                long seconds = (timeLeft % 60000L) / 1000L;

                StringBuilder timeStr = new StringBuilder();
                if (days > 0) timeStr.append(days).append(" ngày ");
                if (hours > 0) timeStr.append(hours).append(" giờ ");
                if (minutes > 0) timeStr.append(minutes).append(" phút ");
                if (seconds > 0) timeStr.append(seconds).append(" giây");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c§l[!] §cKit §e" + safeName + "§c đang hồi chiêu! Quay lại sau: §e" + timeStr.toString().trim());
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                int emptySlots = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item == null || item.getType() == Material.AIR) emptySlots++;
                }

                if (emptySlots < kit.items.length) {
                    player.sendMessage("§c§l[!] §cTúi đồ đầy! Hãy chừa ít nhất §e" + kit.items.length + " §cô trống.");
                    return;
                }

                for (ItemStack item : kit.items) { player.getInventory().addItem(item.clone()); }
                player.sendMessage("§a§l[!] §aNhận thành công Kit §e" + safeName + "§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    kitsDoc.put(safeName, now);
                    Document updateDoc = (doc != null) ? doc : new Document("_id", uuid.toString());
                    updateDoc.put("kits", kitsDoc);
                    playersCollection.replaceOne(Filters.eq("_id", uuid.toString()), updateDoc, new ReplaceOptions().upsert(true));
                });
            });
        });
    }

    private String itemArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) { dataOutput.writeObject(item); }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) { return ""; }
    }

    private ItemStack[] itemArrayFromBase64(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) { items[i] = (ItemStack) dataInput.readObject(); }
            dataInput.close();
            return items;
        } catch (Exception e) { return new ItemStack[0]; }
    }
}