package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final JavaPlugin plugin;
    private final CurrencyManager currencyManager;
    private File file;
    private FileConfiguration config;

    // Cấu trúc mới: ShopID -> Page -> Slot -> ShopItem
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
        setupFile();
    }

    private void setupFile() {
        file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadShopFromFile();
    }

    private void loadShopFromFile() {
        shops.clear();
        if (!config.contains("shops")) return;

        for (String shopId : config.getConfigurationSection("shops").getKeys(false)) {
            shops.put(shopId, new HashMap<>());

            for (String pageStr : config.getConfigurationSection("shops." + shopId).getKeys(false)) {
                int page = Integer.parseInt(pageStr);
                shops.get(shopId).put(page, new HashMap<>());

                for (String slotStr : config.getConfigurationSection("shops." + shopId + "." + page).getKeys(false)) {
                    int slot = Integer.parseInt(slotStr);
                    ItemStack item = config.getItemStack("shops." + shopId + "." + page + "." + slot + ".item");
                    long sellPrice = config.getLong("shops." + shopId + "." + page + "." + slot + ".sellPrice");
                    long buyPrice = config.getLong("shops." + shopId + "." + page + "." + slot + ".buyPrice");

                    shops.get(shopId).get(page).put(slot, new ShopItem(item, sellPrice, buyPrice));
                }
            }
        }
    }

    public void saveShopToFile() {
        config.set("shops", null);
        for (Map.Entry<String, Map<Integer, Map<Integer, ShopItem>>> shopEntry : shops.entrySet()) {
            String shopId = shopEntry.getKey();
            for (Map.Entry<Integer, Map<Integer, ShopItem>> pageEntry : shopEntry.getValue().entrySet()) {
                int page = pageEntry.getKey();
                for (Map.Entry<Integer, ShopItem> slotEntry : pageEntry.getValue().entrySet()) {
                    int slot = slotEntry.getKey();
                    ShopItem sItem = slotEntry.getValue();

                    config.set("shops." + shopId + "." + page + "." + slot + ".item", sItem.item);
                    config.set("shops." + shopId + "." + page + "." + slot + ".sellPrice", sItem.sellPrice);
                    config.set("shops." + shopId + "." + page + "." + slot + ".buyPrice", sItem.buyPrice);
                }
            }
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
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

    // [MỚI]: Thêm hàm kiểm tra xem có trang tiếp theo không
    public boolean hasPage(String shopId, int page) {
        return shops.containsKey(shopId) && shops.get(shopId).containsKey(page) && !shops.get(shopId).get(page).isEmpty();
    }

    // SỬA: TẠO UI CHO ADMIN (Đánh dấu Hàng Mua / Bán)
    public Inventory openAdminGUI(String shopId, String shopName, int page) {
        // Cắt bớt tên nếu quá dài để tránh lỗi UI
        String safeName = shopName.length() > 20 ? shopName.substring(0, 20) : shopName;
        Inventory inv = Bukkit.createInventory(null, 45, "§0§l" + safeName + " | Tr " + page);

        ItemStack blackGlass = createGlass(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 9; i++) inv.setItem(i, blackGlass);
        for (int i = 36; i < 45; i++) inv.setItem(i, blackGlass);
        inv.setItem(17, getPaginationButton(shopId, page, true));

        // ==========================================
        // [MỚI] ĐÁNH DẤU HÀNG MUA / BÁN Ở Ô RÌA
        // ==========================================
        // Hàng 3 (Ô 26): Giá Shop BÁN cho người chơi -> Kính Đỏ
        ItemStack sellLabel = createGlass(Material.RED_STAINED_GLASS_PANE, "§c§lGIÁ BÁN (Shop Bán)");
        org.bukkit.inventory.meta.ItemMeta sellMeta = sellLabel.getItemMeta();
        sellMeta.setLore(java.util.Arrays.asList("§7Giá mà người chơi phải trả", "§7để MUA vật phẩm này."));
        sellLabel.setItemMeta(sellMeta);
        inv.setItem(26, sellLabel);

        // Hàng 4 (Ô 35): Giá Shop MUA của người chơi -> Kính Xanh
        ItemStack buyLabel = createGlass(Material.GREEN_STAINED_GLASS_PANE, "§a§lGIÁ MUA (Shop Thu Mua)");
        org.bukkit.inventory.meta.ItemMeta buyMeta = buyLabel.getItemMeta();
        buyMeta.setLore(java.util.Arrays.asList("§7Giá mà người chơi nhận được", "§7khi BÁN vật phẩm này cho Shop."));
        buyLabel.setItemMeta(buyMeta);
        inv.setItem(35, buyLabel);

        // Load vật phẩm đã lưu
        if (shops.containsKey(shopId) && shops.get(shopId).containsKey(page)) {
            for (int i = 0; i < 8; i++) {
                ShopItem sItem = shops.get(shopId).get(page).get(i);
                if (sItem != null) {
                    // Đặt vật phẩm chính vào Hàng 2
                    inv.setItem(i + 9, sItem.item);

                    // [FIX LỖI] Đặt lại Tiền vật lý vào Hàng 3 (Giá Bán) và Hàng 4 (Giá Mua)
                    if (sItem.sellPrice > 0) {
                        inv.setItem(i + 18, currencyManager.createCurrencyStackFromValue(sItem.sellPrice));
                    }
                    if (sItem.buyPrice > 0) {
                        inv.setItem(i + 27, currencyManager.createCurrencyStackFromValue(sItem.buyPrice));
                    }
                }
            }
        }
        return inv;
    }

    // SỬA: TẠO UI CHO NGƯỜI CHƠI (Hiển thị đúng tên NPC)
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
                    if (sItem.sellPrice <= 0 && sItem.buyPrice <= 0) continue;
                    ItemStack displayItem = sItem.item.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                    // Lấy % Sale của Thương nhân
                    double discount = vn.minecraft.rPGCore.jobs.types.MerchantJob.getBargainDiscount(player);

                    lore.add("");

                    // SHOP BÁN -> THƯƠNG NHÂN MUA (Được giảm giá)
                    if (sItem.sellPrice > 0) {
                        long finalSellPrice = sItem.sellPrice - (long)(sItem.sellPrice * discount);
                        if (discount > 0) {
                            lore.add("§8▶ Giá gốc: §m" + currencyManager.formatCurrency(sItem.sellPrice));
                            lore.add("§e▶ Bạn mua: " + currencyManager.formatCurrency(finalSellPrice) + " §a(-" + (int)(discount*100) + "%) §7(Click Phải)");
                        } else {
                            lore.add("§e▶ Mua: " + currencyManager.formatCurrency(sItem.sellPrice) + " §7(Click Phải)");
                        }
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_final_sell"), org.bukkit.persistence.PersistentDataType.LONG, finalSellPrice);
                    }

                    // SHOP MUA -> THƯƠNG NHÂN BÁN (Được tăng giá)
                    if (sItem.buyPrice > 0) {
                        long finalBuyPrice = sItem.buyPrice + (long)(sItem.buyPrice * discount);
                        if (discount > 0) {
                            lore.add("§8▶ Shop thu gốc: §m" + currencyManager.formatCurrency(sItem.buyPrice));
                            lore.add("§a▶ Bạn bán được: " + currencyManager.formatCurrency(finalBuyPrice) + " §a(+" + (int)(discount*100) + "%) §7(Click Trái)");
                        } else {
                            lore.add("§a▶ Bán: " + currencyManager.formatCurrency(sItem.buyPrice) + " §7(Click Trái)");
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
}