package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vn.minecraft.rPGCore.database.MongoManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class ShopManager {

    private final JavaPlugin plugin;
    private final CurrencyManager currencyManager;
    private final MongoCollection<Document> collection;

    // Cấu trúc: ShopID -> Page -> Slot -> ShopItem
    private final Map<String, Map<Integer, Map<Integer, ShopItem>>> shops = new HashMap<>();

    public static class ShopItem {
        public ItemStack item;
        public long sellPrice;
        public long buyPrice;

        public ShopItem(ItemStack item, long sellPrice, long buyPrice) {
            this.item = item;
            this.sellPrice = sellPrice;
            this.buyPrice = buyPrice;
        }
    }

    public ShopManager(JavaPlugin plugin, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        // Kết nối tới Collection shops
        this.collection = MongoManager.getInstance().getDatabase().getCollection("shops");
        loadShopsFromDB();
    }

    // ==========================================
    // ĐỌC DỮ LIỆU TỪ MONGODB (Thay cho YAML cũ)
    // ==========================================
    private void loadShopsFromDB() {
        shops.clear();
        for (Document doc : collection.find()) {
            String shopId = doc.getString("_id");
            shops.put(shopId, new HashMap<>());

            Document pagesDoc = (Document) doc.get("data");
            if (pagesDoc == null) continue;

            for (String pageStr : pagesDoc.keySet()) {
                int page = Integer.parseInt(pageStr);
                shops.get(shopId).put(page, new HashMap<>());

                Document slotsDoc = (Document) pagesDoc.get(pageStr);
                for (String slotStr : slotsDoc.keySet()) {
                    int slot = Integer.parseInt(slotStr);
                    Document itemDoc = (Document) slotsDoc.get(slotStr);

                    ItemStack item = itemFromBase64(itemDoc.getString("item"));
                    long sell = itemDoc.getLong("sellPrice");
                    long buy = itemDoc.getLong("buyPrice");

                    if (item != null) {
                        shops.get(shopId).get(page).put(slot, new ShopItem(item, sell, buy));
                    }
                }
            }
        }
        plugin.getLogger().info("Đã tải dữ liệu Cửa hàng từ MongoDB.");
    }

    // ==========================================
    // LƯU DỮ LIỆU XUỐNG MONGODB (Hàm đang bị thiếu)
    // ==========================================
    public void saveShopToDB(String shopId) {
        if (!shops.containsKey(shopId)) return;

        Document dataDoc = new Document();
        for (Map.Entry<Integer, Map<Integer, ShopItem>> pageEntry : shops.get(shopId).entrySet()) {
            Document slotsDoc = new Document();
            for (Map.Entry<Integer, ShopItem> slotEntry : pageEntry.getValue().entrySet()) {
                ShopItem sItem = slotEntry.getValue();
                slotsDoc.append(String.valueOf(slotEntry.getKey()), new Document()
                        .append("item", itemToBase64(sItem.item))
                        .append("sellPrice", sItem.sellPrice)
                        .append("buyPrice", sItem.buyPrice));
            }
            dataDoc.append(String.valueOf(pageEntry.getKey()), slotsDoc);
        }

        Document shopDoc = new Document("_id", shopId).append("data", dataDoc);

        // Chạy bất đồng bộ để không kẹt Server khi Admin save shop
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            collection.replaceOne(Filters.eq("_id", shopId), shopDoc, new ReplaceOptions().upsert(true));
        });
    }

    public void saveShopItem(String shopId, int page, int slot, ItemStack item, long sellPrice, long buyPrice) {
        shops.putIfAbsent(shopId, new HashMap<>());
        shops.get(shopId).putIfAbsent(page, new HashMap<>());

        if (item == null || item.getType() == Material.AIR) {
            shops.get(shopId).get(page).remove(slot);
        } else {
            shops.get(shopId).get(page).put(slot, new ShopItem(item.clone(), sellPrice, buyPrice));
        }
    }

    public ShopItem getShopItem(String shopId, int page, int slot) {
        if (!shops.containsKey(shopId) || !shops.get(shopId).containsKey(page)) return null;
        return shops.get(shopId).get(page).get(slot);
    }

    public boolean hasPage(String shopId, int page) {
        return shops.containsKey(shopId) && shops.get(shopId).containsKey(page) && !shops.get(shopId).get(page).isEmpty();
    }

    // ==========================================
    // CÁC HÀM UI (Giữ nguyên y hệt code của bạn)
    // ==========================================
    // ==========================================
    // TẠO UI CHO ADMIN (Rút gọn còn 1 hàng - 9 ô)
    // ==========================================
    public Inventory openAdminGUI(String shopId, String shopName, int page) {
        String safeName = shopName.length() > 20 ? shopName.substring(0, 20) : shopName;
        // Rút gọn kích thước Inventory xuống còn 9
        Inventory inv = Bukkit.createInventory(null, 9, "§0§l" + safeName + " | Tr " + page);

        // Ô số 8 (cuối cùng) là nút Chuyển Trang
        inv.setItem(8, getPaginationButton(shopId, page, true));

        // Load vật phẩm đã lưu (Tối đa 8 item từ ô 0 -> 7)
        if (shops.containsKey(shopId) && shops.get(shopId).containsKey(page)) {
            for (int i = 0; i < 8; i++) {
                ShopItem sItem = shops.get(shopId).get(page).get(i);
                if (sItem != null) {
                    inv.setItem(i, sItem.item); // Chỉ lưu và hiển thị Item, không cần hiển thị giá
                }
            }
        }
        return inv;
    }

    // ==========================================
    // TẠO UI CHO NGƯỜI CHƠI (Tự động lấy giá từ GlobalEconomy)
    // ==========================================
    public Inventory openPlayerGUI(org.bukkit.entity.Player player, String shopId, String shopName, int page) {
        String safeName = shopName.length() > 20 ? shopName.substring(0, 20) : shopName;
        Inventory inv = Bukkit.createInventory(null, 27, "§0§l" + safeName + " | Tr " + page);

        ItemStack blackGlass = createGlass(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 9; i++) inv.setItem(i, blackGlass);
        for (int i = 18; i < 27; i++) inv.setItem(i, blackGlass);
        inv.setItem(17, getPaginationButton(shopId, page, false));

        if (shops.containsKey(shopId) && shops.get(shopId).containsKey(page)) {
            for (int i = 0; i < 8; i++) {
                ShopItem sItem = shops.get(shopId).get(page).get(i);
                if (sItem != null) {
                    // MÓC NỐI VỚI GLOBAL ECONOMY ĐỂ LẤY GIÁ
                    long currentSellPrice = vn.minecraft.rPGCore.RPGCore.getInstance().getGlobalEconomyManager().getBuyPrice(sItem.item);  // Giá Shop Bán -> Người mua
                    long currentBuyPrice = vn.minecraft.rPGCore.RPGCore.getInstance().getGlobalEconomyManager().getSellPrice(sItem.item);  // Giá Shop Mua -> Người bán

                    if (currentSellPrice <= 0 && currentBuyPrice <= 0) continue;

                    ItemStack displayItem = sItem.item.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    double discount = vn.minecraft.rPGCore.jobs.types.MerchantJob.getBargainDiscount(player);

                    lore.add("");

                    if (currentSellPrice > 0) {
                        long finalSellPrice = currentSellPrice - (long)(currentSellPrice * discount);
                        if (discount > 0) {
                            lore.add("§8▶ Giá thị trường: §m" + currencyManager.formatCurrency(currentSellPrice));
                            lore.add("§e▶ Bạn mua: " + currencyManager.formatCurrency(finalSellPrice) + " §a(-" + (int)(discount*100) + "%) §7(Click Phải)");
                        } else {
                            lore.add("§e▶ Mua: " + currencyManager.formatCurrency(currentSellPrice) + " §7(Click Phải)");
                        }
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_final_sell"), org.bukkit.persistence.PersistentDataType.LONG, finalSellPrice);
                    }

                    if (currentBuyPrice > 0) {
                        long finalBuyPrice = currentBuyPrice + (long)(currentBuyPrice * discount);
                        if (discount > 0) {
                            lore.add("§8▶ Shop thu gốc: §m" + currencyManager.formatCurrency(currentBuyPrice));
                            lore.add("§a▶ Bạn bán được: " + currencyManager.formatCurrency(finalBuyPrice) + " §a(+" + (int)(discount*100) + "%) §7(Click Trái)");
                        } else {
                            lore.add("§a▶ Bán: " + currencyManager.formatCurrency(currentBuyPrice) + " §7(Click Trái)");
                        }
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_final_buy"), org.bukkit.persistence.PersistentDataType.LONG, finalBuyPrice);
                    }

                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                    inv.setItem(i + 9, displayItem);
                }
            }
        }
        return inv;
    }

    private ItemStack getPaginationButton(String shopId, int currentPage, boolean isAdmin) {
        boolean hasNext = isAdmin || (shops.containsKey(shopId) && shops.get(shopId).containsKey(currentPage + 1));
        Material glass = hasNext ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack btn = createGlass(glass, "§f§lChuyển Trang");
        ItemMeta meta = btn.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add("§7Trang hiện tại: " + currentPage);
        lore.add("§eClick Trái: §fTrang tiếp theo");
        lore.add("§eClick Phải: §fTrang trước");
        meta.setLore(lore);
        btn.setItemMeta(meta);
        return btn;
    }

    private ItemStack createGlass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // CÔNG CỤ CHUYỂN ĐỔI ITEM <-> CHUỖI BASE64
    // ==========================================
    private String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) { return ""; }
    }

    private ItemStack itemFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) { return null; }
    }
}