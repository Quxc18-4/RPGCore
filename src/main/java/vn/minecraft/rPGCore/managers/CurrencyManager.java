package vn.minecraft.rPGCore.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CurrencyManager {

    private final JavaPlugin plugin;
    public final NamespacedKey CURRENCY_KEY;

    public CurrencyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.CURRENCY_KEY = new NamespacedKey(plugin, "rpg_currency_type");
    }

    // ==========================================
    // UI TIỀN TỆ: MÔ TẢ NẰM DƯỚI ĐƯỜNG KẺ
    // ==========================================
    public ItemStack getCurrencyItem(String type, int amount) {
        Material mat;
        String name;
        String desc;
        String rarityColor;

        if (type.equals("gold")) {
            mat = Material.GOLD_NUGGET;
            name = "Xu Vàng";
            desc = "Loại tiền có mệnh giá cao nhất.";
            rarityColor = "§b"; // RARE
        } else if (type.equals("iron")) {
            mat = Material.IRON_NUGGET;
            name = "Xu Bạc";
            desc = "Loại tiền tương đối quý giá.";
            rarityColor = "§a"; // UNCOMMON
        } else {
            mat = Material.COPPER_NUGGET;
            name = "Xu Đồng";
            desc = "Loại tiền cơ bản dùng để giao dịch.";
            rarityColor = "§7"; // COMMON
        }

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(rarityColor + "§l" + name);

        List<String> lore = new ArrayList<>();
        lore.add("§8[ Phân loại: Tiền tệ - Tiền kim loại ]");

        // Đường kẻ
        lore.add("§8§m                                ");

        // Mô tả nằm dưới cùng
        lore.add("§7" + desc);

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(CURRENCY_KEY, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    public long getCurrencyValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;
        String type = item.getItemMeta().getPersistentDataContainer().get(CURRENCY_KEY, PersistentDataType.STRING);
        if (type == null) return 0;

        long multiplier = type.equals("gold") ? 4096 : (type.equals("iron") ? 64 : 1);
        return item.getAmount() * multiplier;
    }

    public String formatCurrency(long amount) {
        if (amount <= 0) return "0 Đồng";

        long gold = amount / 4096;
        long iron = (amount % 4096) / 64;
        long copper = amount % 64;

        StringBuilder sb = new StringBuilder();
        if (gold > 0) sb.append("§e").append(gold).append(" Vàng ");
        if (iron > 0) sb.append("§f").append(iron).append(" Bạc ");
        if (copper > 0) sb.append("§6").append(copper).append(" Đồng");

        return sb.toString().trim();
    }

    public void compressCurrency(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = player.getInventory();
        long totalValue = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            long val = getCurrencyValue(item);
            if (val > 0) {
                totalValue += val;
                inv.setItem(i, null);
            }
        }

        if (totalValue == 0) return;

        long gold = totalValue / 4096;
        long iron = (totalValue % 4096) / 64;
        long copper = totalValue % 64;

        giveCurrencySafe(player, "gold", gold);
        giveCurrencySafe(player, "iron", iron);
        giveCurrencySafe(player, "copper", copper);
    }

    public void depositAll(org.bukkit.entity.Player player, vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer) {
        org.bukkit.inventory.Inventory inv = player.getInventory();
        long totalValue = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            long val = getCurrencyValue(item);
            if (val > 0) {
                totalValue += val;
                inv.setItem(i, null);
            }
        }

        if (totalValue > 0) {
            rpgPlayer.addBalance(totalValue);
            player.sendMessage("§a[Ngân Hàng] §fĐã nạp thành công " + formatCurrency(totalValue) + " vào tài khoản!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage("§cBạn không có đồng Xu nào trong túi đồ!");
        }
    }

    public void giveCurrencySafe(org.bukkit.entity.Player player, String type, long amount) {
        while (amount > 0) {
            int toGive = (int) Math.min(64, amount);
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(getCurrencyItem(type, toGive));
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            amount -= toGive;
        }
    }

    public ItemStack createCurrencyStackFromValue(long value) {
        if (value <= 0) return null;

        if (value % 4096 == 0 && (value / 4096) <= 64) {
            return getCurrencyItem("gold", (int) (value / 4096));
        }
        else if (value % 64 == 0 && (value / 64) <= 64) {
            return getCurrencyItem("iron", (int) (value / 64));
        }
        else if (value <= 64) {
            return getCurrencyItem("copper", (int) value);
        }
        return null;
    }

    public long getTotalPhysicalCurrency(org.bukkit.entity.Player player) {
        long total = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            total += getCurrencyValue(item);
        }
        return total;
    }

    public void givePhysicalCurrency(org.bukkit.entity.Player player, long amount) {
        if (amount <= 0) return;
        long gold = amount / 4096;
        long iron = (amount % 4096) / 64;
        long copper = amount % 64;

        if (gold > 0) giveCurrencySafe(player, "gold", gold);
        if (iron > 0) giveCurrencySafe(player, "iron", iron);
        if (copper > 0) giveCurrencySafe(player, "copper", copper);
    }

    public boolean takePhysicalCurrency(org.bukkit.entity.Player player, long amount) {
        long total = getTotalPhysicalCurrency(player);
        if (total < amount) return false;

        org.bukkit.inventory.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (getCurrencyValue(inv.getItem(i)) > 0) {
                inv.setItem(i, null);
            }
        }

        long remaining = total - amount;
        givePhysicalCurrency(player, remaining);

        return true;
    }

    // ==========================================
    // THANH TOÁN THÔNG MINH (TIỀN MẶT + NGÂN HÀNG)
    // ==========================================
    public boolean processSmartPayment(org.bukkit.entity.Player player, vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer, long amountRequired) {
        if (amountRequired <= 0) return true;

        // 1. Kiểm tra tiền mặt trong túi đồ
        long physicalCash = getTotalPhysicalCurrency(player);

        // Trường hợp 1: Tiền mặt đủ trả toàn bộ
        if (physicalCash >= amountRequired) {
            return takePhysicalCurrency(player, amountRequired);
        }

        // 2. Nếu tiền mặt không đủ, tính số tiền còn thiếu cần rút từ Ngân hàng
        long deficit = amountRequired - physicalCash;
        long bankBalance = rpgPlayer.getBalance();

        // Trường hợp 2: Tổng tài sản (Tiền mặt + Ngân hàng) không đủ
        if (bankBalance < deficit) {
            return false;
        }

        // Trường hợp 3: Tổng tài sản đủ -> Vét sạch tiền mặt và trừ nốt phần thiếu vào Ngân hàng
        if (physicalCash > 0) {
            takePhysicalCurrency(player, physicalCash);
        }

        // Sử dụng chuẩn xác hàm withdraw() từ mã nguồn RPGPlayer của bạn
        rpgPlayer.withdraw(deficit);

        player.sendMessage("§e[Giao Dịch] §fĐã trừ §a" + formatCurrency(deficit) + " §ftừ Ngân hàng do thiếu tiền mặt.");

        return true;
    }

    // ==========================================
    // KIỂM TRA ĐỦ TIỀN KHÔNG (DÙNG CHO GUI)
    // ==========================================
    public boolean canAffordSmart(org.bukkit.entity.Player player, vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer, long amountRequired) {
        if (amountRequired <= 0) return true;
        long physicalCash = getTotalPhysicalCurrency(player);
        if (physicalCash >= amountRequired) return true;

        long deficit = amountRequired - physicalCash;
        return rpgPlayer.getBalance() >= deficit;
    }

    // ==========================================
    // KIỂM TRA VÀ TRỪ TIỀN OFFLINE TỪ MONGODB
    // ==========================================
    public boolean processOfflinePayment(java.util.UUID uuid, long amountRequired) {
        if (amountRequired <= 0) return true;

        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(uuid);
            if (rpgPlayer != null && rpgPlayer.getBalance() >= amountRequired) {
                rpgPlayer.withdraw(amountRequired);
                return true;
            }
            return false;
        } else {
            com.mongodb.client.MongoCollection<org.bson.Document> collection = vn.minecraft.rPGCore.database.MongoManager.getInstance().getDatabase().getCollection("players");
            org.bson.Document doc = collection.find(com.mongodb.client.model.Filters.eq("_id", uuid.toString())).first();

            if (doc != null) {
                int activeSlot = (doc.get("active_slot") instanceof Number) ? ((Number) doc.get("active_slot")).intValue() : 1;
                org.bson.Document slotsDoc = doc.get("slots", org.bson.Document.class);
                org.bson.Document slotData = (slotsDoc != null) ? slotsDoc.get(String.valueOf(activeSlot), org.bson.Document.class) : null;

                long currentBalance = 0L;
                if (slotData != null && slotData.get("balance") instanceof Number) {
                    currentBalance = ((Number) slotData.get("balance")).longValue();
                } else if (doc.get("balance") instanceof Number) {
                    currentBalance = ((Number) doc.get("balance")).longValue();
                }

                if (currentBalance >= amountRequired) {
                    collection.updateOne(
                            com.mongodb.client.model.Filters.eq("_id", uuid.toString()),
                            com.mongodb.client.model.Updates.set("slots." + activeSlot + ".balance", currentBalance - amountRequired)
                    );
                    return true;
                }
            }
            return false;
        }
    }
    // ==========================================
    // CỘNG TIỀN THƯỞNG ONLINE / OFFLINE AN TOÀN
    // ==========================================

    public boolean processOfflineDeposit(java.util.UUID uuid, long amountToAdd, String notifyMessage) {
        if (amountToAdd <= 0) return true;

        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer =
                    vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(uuid);
            if (rpgPlayer != null) {
                rpgPlayer.addBalance(amountToAdd);
                if (notifyMessage != null && !notifyMessage.isEmpty()) {
                    player.sendMessage(notifyMessage);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                }
                return true;
            }
            return false;
        } else {
            com.mongodb.client.MongoCollection<org.bson.Document> collection =
                    vn.minecraft.rPGCore.database.MongoManager.getInstance().getDatabase().getCollection("players");
            org.bson.Document doc = collection.find(com.mongodb.client.model.Filters.eq("_id", uuid.toString())).first();
            if (doc == null) return false;

            int activeSlot = (doc.get("active_slot") instanceof Number) ? ((Number) doc.get("active_slot")).intValue() : 1;
            com.mongodb.client.result.UpdateResult result = collection.updateOne(
                    com.mongodb.client.model.Filters.eq("_id", uuid.toString()),
                    com.mongodb.client.model.Updates.inc("slots." + activeSlot + ".balance", amountToAdd)
            );
            return result.getMatchedCount() > 0;
        }
    }
}