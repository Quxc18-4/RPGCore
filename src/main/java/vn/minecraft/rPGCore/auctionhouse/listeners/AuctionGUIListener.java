package vn.minecraft.rPGCore.auctionhouse.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.auctionhouse.models.AuctionItem;
import vn.minecraft.rPGCore.database.MongoManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.CustomGUI;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionGUIListener implements Listener {

    private final RPGCore plugin;

    // Các bộ nhớ đệm (Cache) để xử lý UI
    private final Map<UUID, Integer> viewerPages = new HashMap<>();
    private final Map<UUID, ItemStack> pendingSellItems = new HashMap<>();
    private final Map<UUID, Long> pendingSellPrices = new HashMap<>();
    private final Map<UUID, String> pendingBuys = new HashMap<>();

    // Cache cho tính năng Tìm kiếm (Dùng ConcurrentHashMap để an toàn với đa luồng của ChatEvent)
    private final Map<UUID, String> activeSearches = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pendingSearchInput = new ConcurrentHashMap<>();

    private final long PROTECTION_TIME = 5L * 60 * 1000; // 5 Phút (Mili-giây)
    private final NamespacedKey ahKey; // Chìa khóa giấu ID

    public AuctionGUIListener(RPGCore plugin) {
        this.plugin = plugin;
        this.ahKey = new NamespacedKey(plugin, "ah_item_id");
    }

    // ==========================================
    // 1. MỞ GIAO DIỆN CHỢ CHÍNH (/ah) - CÓ BỘ LỌC
    // ==========================================
    public void openMainMenu(Player player, int page) {
        CustomGUI gui = new CustomGUI("AH_MAIN", page);
        Inventory inv = Bukkit.createInventory(gui, 54, "§0§lCHỢ ĐEN | TRANG " + page);

        List<AuctionItem> allItems = plugin.getAuctionManager().getAllActiveAuctions();
        Collections.reverse(allItems);

        // ==========================================
        // [MỚI]: KIỂM TRA BỘ LỌC TÌM KIẾM
        // ==========================================
        String keyword = activeSearches.get(player.getUniqueId());
        if (keyword != null && !keyword.isEmpty()) {
            allItems.removeIf(ai -> {
                String itemName = (ai.getItem().hasItemMeta() && ai.getItem().getItemMeta().hasDisplayName()) ?
                        ai.getItem().getItemMeta().getDisplayName() : ai.getItem().getType().name();
                // Lọc không phân biệt hoa thường
                return !itemName.toLowerCase().contains(keyword.toLowerCase());
            });
        }

        int maxItemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) allItems.size() / maxItemsPerPage);
        if (totalPages == 0) totalPages = 1;

        int startIndex = (page - 1) * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allItems.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem ai = allItems.get(i);
            ItemStack displayItem = ai.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§f▶ Người bán: §b" + ai.getSellerName());
            lore.add("§f▶ Giá bán: §a" + plugin.getCurrencyManager().formatCurrency(ai.getPrice()));

            long timePassed = System.currentTimeMillis() - ai.getListTime();
            if (timePassed < PROTECTION_TIME) {
                long timeLeft = (PROTECTION_TIME - timePassed) / 1000 / 60;
                lore.add("§c▶ BẢO VỆ: §fĐang khóa (" + timeLeft + " phút)");
            } else {
                lore.add("§a▶ BẢO VỆ: §fĐã mở khóa, có thể mua!");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
            lore.add("§7Treo lúc: " + sdf.format(new Date(ai.getListTime())));

            // [MỚI]: XÓA DÒNG LORE ID XẤU XÍ, THAY BẰNG DỮ LIỆU NGẦM (PDC)
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(ahKey, PersistentDataType.STRING, ai.getId());

            displayItem.setItemMeta(meta);
            inv.setItem(slot++, displayItem);
        }

        // --- Thanh điều khiển bên dưới ---
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);

        if (page > 1) {
            inv.setItem(45, createItem(Material.ARROW, "§a§lTrang Trước"));
        }

        // [MỚI]: NÚT TÌM KIẾM Ở SLOT 48 (Cùng hàng với nút Làm Mới)
        if (keyword != null && !keyword.isEmpty()) {
            inv.setItem(48, createItem(Material.BARRIER, "§c§lHỦY BỘ LỌC", "§7Đang tìm: §e" + keyword, "§7Click để xóa bộ lọc tìm kiếm."));
        } else {
            inv.setItem(48, createItem(Material.NAME_TAG, "§b§lTÌM KIẾM", "§7Click để tìm kiếm vật phẩm", "§7theo tên."));
        }

        inv.setItem(49, createItem(Material.SUNFLOWER, "§e§lLàm Mới", "§7Cập nhật danh sách mới nhất."));

        if (page < totalPages) {
            inv.setItem(53, createItem(Material.ARROW, "§a§lTrang Sau"));
        }

        viewerPages.put(player.getUniqueId(), page);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    // ==========================================
    // 2. MỞ GIAO DIỆN XÁC NHẬN BÁN (/ahc)
    // ==========================================
    public void openConfirmSellGUI(Player player, ItemStack item, long price) {
        CustomGUI gui = new CustomGUI("AH_CONFIRM_SELL", 0);
        Inventory inv = Bukkit.createInventory(gui, 27, "§0§lXÁC NHẬN ĐĂNG BÁN");

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        pendingSellItems.put(player.getUniqueId(), item.clone());
        pendingSellPrices.put(player.getUniqueId(), price);
        player.getInventory().setItemInMainHand(null);

        inv.setItem(11, createItem(Material.RED_TERRACOTTA, "§c§lHỦY BỎ", "§7Lấy lại vật phẩm."));

        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add("");
        lore.add("§e§l[!] GIÁ NIÊM YẾT: §a" + plugin.getCurrencyManager().formatCurrency(price));
        meta.setLore(lore);
        display.setItemMeta(meta);
        inv.setItem(13, display);

        inv.setItem(15, createItem(Material.LIME_TERRACOTTA, "§a§lĐỒNG Ý ĐĂNG",
                "§7Đưa lên sàn giao dịch.",
                "§c(15 phút đầu không ai có thể mua)"));

        player.openInventory(inv);
    }

    // ==========================================
    // 3. MỞ GIAO DIỆN XÁC NHẬN MUA
    // ==========================================
    public void openConfirmBuyGUI(Player player, AuctionItem auctionItem) {
        CustomGUI gui = new CustomGUI("AH_CONFIRM_BUY", 0);
        Inventory inv = Bukkit.createInventory(gui, 27, "§0§lXÁC NHẬN MUA HÀNG");

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        pendingBuys.put(player.getUniqueId(), auctionItem.getId());

        inv.setItem(11, createItem(Material.RED_TERRACOTTA, "§c§lQUAY LẠI CHỢ"));

        ItemStack display = auctionItem.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add("");
        lore.add("§f▶ Tổng thanh toán: §a" + plugin.getCurrencyManager().formatCurrency(auctionItem.getPrice()));
        lore.add("§f▶ Người bán: §b" + auctionItem.getSellerName());
        meta.setLore(lore);
        display.setItemMeta(meta);
        inv.setItem(13, display);

        inv.setItem(15, createItem(Material.EMERALD_BLOCK, "§a§lXÁC NHẬN THANH TOÁN", "§7Hệ thống sẽ trừ tiền tự động."));

        player.openInventory(inv);
    }

    // ==========================================
    // 4. BẮT SỰ KIỆN CLICK TRONG GUI
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomGUI)) return;
        CustomGUI gui = (CustomGUI) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        // --- A. CHỢ CHÍNH ---
        if (gui.getGuiId().equals("AH_MAIN")) {
            int slot = event.getRawSlot();
            int page = viewerPages.getOrDefault(player.getUniqueId(), 1);

            // Điều hướng
            if (slot == 45) { openMainMenu(player, page - 1); return; }
            if (slot == 53) { openMainMenu(player, page + 1); return; }
            if (slot == 49) { openMainMenu(player, 1); return; }

            // [MỚI]: CLICK NÚT TÌM KIẾM
            if (slot == 48) {
                if (activeSearches.containsKey(player.getUniqueId())) {
                    activeSearches.remove(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    openMainMenu(player, 1);
                } else {
                    pendingSearchInput.put(player.getUniqueId(), true);
                    player.closeInventory();
                    player.sendMessage("§e§l[Chợ Đen] §fHãy gõ tên vật phẩm bạn muốn tìm vào khung Chat!");
                    player.sendMessage("§7(Gõ chữ §c'huy' §7nếu bạn muốn hủy bỏ tìm kiếm)");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                return;
            }

            // Click vào vật phẩm
            if (slot >= 0 && slot < 45 && event.getCurrentItem().getType() != Material.AIR) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta == null) return;

                // [MỚI]: LẤY ID TỪ DỮ LIỆU NGẦM (PDC) THAY VÌ LORE
                if (!meta.getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) return;
                String auctionId = meta.getPersistentDataContainer().get(ahKey, PersistentDataType.STRING);

                if (auctionId != null) {
                    AuctionItem ai = plugin.getAuctionManager().getAuctionItemById(auctionId);
                    if (ai == null || ai.getStatus() != AuctionItem.Status.ACTIVE) {
                        player.sendMessage("§c§l[!] §cMón hàng này không tồn tại hoặc đã bị ai đó nẫng tay trên!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        openMainMenu(player, page);
                        return;
                    }

                    // [MỚI]: TÍNH NĂNG THU HỒI ĐỒ CỦA CHÍNH MÌNH
                    if (ai.getSellerId().equals(player.getUniqueId())) {
                        // Ném đồ vào túi, nếu đầy thì rớt ra đất
                        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(ai.getItem());
                        for (ItemStack leftover : leftovers.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }

                        // Xóa khỏi Database
                        plugin.getAuctionManager().deleteAuctionItem(auctionId);

                        player.sendMessage("§a§l[Chợ Đen] §fBạn đã thu hồi vật phẩm thành công!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                        openMainMenu(player, page); // Reload lại trang
                        return;
                    }

                    long timePassed = System.currentTimeMillis() - ai.getListTime();
                    if (timePassed < PROTECTION_TIME) {
                        long timeLeft = (PROTECTION_TIME - timePassed) / 1000 / 60;
                        player.sendMessage("§c§l[!] §cMón hàng này đang trong thời gian bảo vệ! Vui lòng quay lại sau §e" + timeLeft + " phút§c.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    openConfirmBuyGUI(player, ai);
                }
            }
        }

        // --- B. XÁC NHẬN BÁN (/ahc) ---
        else if (gui.getGuiId().equals("AH_CONFIRM_SELL")) {
            if (event.getRawSlot() == 11) { // Hủy
                player.closeInventory();
            } else if (event.getRawSlot() == 15) { // Đồng ý
                ItemStack itemToSell = pendingSellItems.remove(player.getUniqueId());
                Long price = pendingSellPrices.remove(player.getUniqueId());

                if (itemToSell != null && price != null) {
                    AuctionItem auctionItem = new AuctionItem(player.getUniqueId(), player.getName(), itemToSell, price);
                    plugin.getAuctionManager().listNewItem(auctionItem);

                    String itemName = (itemToSell.hasItemMeta() && itemToSell.getItemMeta().hasDisplayName())
                            ? itemToSell.getItemMeta().getDisplayName() : itemToSell.getType().name();

                    player.sendMessage("§a§l[Chợ Đen] §fĐã đăng bán §e" + itemName + " §fvới giá §a" + plugin.getCurrencyManager().formatCurrency(price) + "§f!");
                    player.sendMessage("§7Vật phẩm sẽ được bảo vệ trong §c5 phút đầu tiên §7(Không ai có thể mua).");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
                }
                player.closeInventory();
            }
        }

        // --- C. XÁC NHẬN MUA ---
        else if (gui.getGuiId().equals("AH_CONFIRM_BUY")) {
            if (event.getRawSlot() == 11) { // Hủy
                pendingBuys.remove(player.getUniqueId());
                openMainMenu(player, 1);
            }
            else if (event.getRawSlot() == 15) { // Đồng ý Mua
                String aId = pendingBuys.remove(player.getUniqueId());
                if (aId == null) return;

                AuctionItem ai = plugin.getAuctionManager().getAuctionItemById(aId);
                if (ai == null || ai.getStatus() != AuctionItem.Status.ACTIVE) {
                    player.sendMessage("§c§l[!] §cRất tiếc! Món hàng này đã bị người khác mua mất hoặc gỡ xuống.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    openMainMenu(player, 1);
                    return;
                }

                RPGPlayer rpgBuyer = plugin.getPlayerManager().getRPGPlayer(player.getUniqueId());
                if (plugin.getCurrencyManager().canAffordSmart(player, rpgBuyer, ai.getPrice())) {

                    plugin.getCurrencyManager().processSmartPayment(player, rpgBuyer, ai.getPrice());

                    HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(ai.getItem());
                    for (ItemStack leftover : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }

                    plugin.getAuctionManager().moveToHistory(ai, player.getUniqueId());
                    paySellerOffline(ai.getSellerId(), ai.getPrice(), ai.getItem());

                    player.sendMessage("§a§l[Thanh Toán] §fGiao dịch thành công!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    openMainMenu(player, 1);
                } else {
                    player.sendMessage("§c§l[!] §cBạn không đủ tài sản (Tiền mặt / Ngân hàng) để mua món này!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
    }

    // ==========================================
    // 5. LẮNG NGHE CHAT ĐỂ TÌM KIẾM
    // ==========================================
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Nếu người chơi đang trong trạng thái chờ nhập liệu Tìm kiếm
        if (pendingSearchInput.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Ngăn không cho tin nhắn hiển thị lên Server
            pendingSearchInput.remove(player.getUniqueId());

            String message = event.getMessage();
            if (message.equalsIgnoreCase("huy")) {
                player.sendMessage("§c§l[!] §cĐã hủy thao tác tìm kiếm!");
            } else {
                activeSearches.put(player.getUniqueId(), message);
                player.sendMessage("§a§l[Chợ Đen] §fĐang lọc các vật phẩm có tên chứa: §e" + message);
            }

            // Mở lại GUI (Phải chạy trên Main Thread vì Bukkit API không cho phép mở UI ở Async Thread)
            Bukkit.getScheduler().runTask(plugin, () -> openMainMenu(player, 1));
        }
    }

    // ==========================================
    // 6. BẢO VỆ ĐỒ KHI ĐÓNG GUI ĐỘT NGỘT (ESC)
    // ==========================================
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CustomGUI) {
            CustomGUI gui = (CustomGUI) event.getInventory().getHolder();
            Player player = (Player) event.getPlayer();

            if (gui.getGuiId().equals("AH_CONFIRM_SELL")) {
                ItemStack pendingItem = pendingSellItems.remove(player.getUniqueId());
                pendingSellPrices.remove(player.getUniqueId());

                if (pendingItem != null) {
                    HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(pendingItem);
                    for (ItemStack leftover : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                    player.sendMessage("§eĐã hủy đăng bán. Trả lại vật phẩm!");
                }
            }

            // Dọn dẹp trạng thái tìm kiếm nếu người chơi đóng GUI đột ngột (Tùy chọn cho sạch RAM)
            if (gui.getGuiId().equals("AH_MAIN")) {
                viewerPages.remove(player.getUniqueId());
            }
        }
    }

    // ==========================================
    // 7. THUẬT TOÁN TRẢ TIỀN CHO NGƯỜI BÁN
    // ==========================================
    private void paySellerOffline(UUID sellerId, long amount, ItemStack soldItem) {
        Player target = Bukkit.getPlayer(sellerId);
        String itemName = (soldItem.hasItemMeta() && soldItem.getItemMeta().hasDisplayName())
                ? soldItem.getItemMeta().getDisplayName() : soldItem.getType().name();

        if (target != null && target.isOnline()) {
            RPGPlayer rTarget = plugin.getPlayerManager().getRPGPlayer(sellerId);
            if (rTarget != null) {
                rTarget.addBalance(amount);
                target.sendMessage("§a§l[Tin Nhắn Chợ Đen] §fMón hàng §e" + itemName + " §fcủa bạn vừa được bán!");
                target.sendMessage("§a+" + plugin.getCurrencyManager().formatCurrency(amount) + " §fđã được chuyển thẳng vào Ngân Hàng.");
                target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
            }
        } else {
            MongoCollection<Document> playerCol = MongoManager.getInstance().getDatabase().getCollection("players");
            Document pDoc = playerCol.find(Filters.eq("_id", sellerId.toString())).first();

            if (pDoc != null) {
                long currentBal = pDoc.getLong("balance") != null ? pDoc.getLong("balance") : 0L;
                playerCol.updateOne(
                        Filters.eq("_id", sellerId.toString()),
                        Updates.set("balance", currentBal + amount)
                );
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) { lore.add(line); }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}