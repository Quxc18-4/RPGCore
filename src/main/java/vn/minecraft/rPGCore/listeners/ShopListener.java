package vn.minecraft.rPGCore.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.CurrencyManager;
import vn.minecraft.rPGCore.managers.ShopManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopListener implements Listener {

    private final ShopManager shopManager;
    private final CurrencyManager currencyManager;
    private final PlayerManager playerManager;

    // Quản lý phiên giao dịch: Lưu ID tọa độ để không cần parse Title
    public static class ShopSession {
        public String shopId;
        public String shopName;
        public boolean isAdmin;
        public ShopSession(String id, String name, boolean admin) {
            this.shopId = id; this.shopName = name; this.isAdmin = admin;
        }
    }
    private final Map<UUID, ShopSession> activeSessions = new HashMap<>();

    public ShopListener(ShopManager shopManager, CurrencyManager currencyManager, PlayerManager playerManager) {
        this.shopManager = shopManager;
        this.currencyManager = currencyManager;
        this.playerManager = playerManager;
    }

    // ==========================================
    // 1. CLICK VÀO NPC DÂN LÀNG (PHIÊN BẢN CHỐNG ĐẨY + LƯU NBT)
    // ==========================================
    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof LivingEntity) {
            LivingEntity npc = (LivingEntity) event.getRightClicked();
            String name = npc.getCustomName();

            // Check nếu tên NPC có chứa chữ "Thương Nhân"
            if (name != null && name.contains("Thương Nhân")) {
                event.setCancelled(true);
                Player player = event.getPlayer();

                // [BẢO VỆ 1]: CHỐNG ĐẨY VẬT LÝ (Xóa va chạm)
                npc.setCollidable(false);

                // [BẢO VỆ 2]: LƯU TỌA ĐỘ VÀO BỘ NHỚ NPC (Khắc phục lỗi đẩy đi mất shop)
                org.bukkit.persistence.PersistentDataContainer pdc = npc.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "shop_id");
                String shopId;

                // Nếu NPC này ĐÃ TỪNG được click và có lưu ShopID trong người
                if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                    shopId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                }
                // Nếu là NPC mới toanh (Click lần đầu tiên)
                else {
                    Location loc = npc.getLocation();
                    shopId = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
                    // Lưu vĩnh viễn ID này vào người NPC
                    pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, shopId);
                }

                // Phân quyền OP / Admin
                boolean isAdmin = player.isOp() || player.hasPermission("rpgcore.admin");

                // Lưu phiên làm việc
                activeSessions.put(player.getUniqueId(), new ShopSession(shopId, name, isAdmin));

                if (isAdmin) {
                    player.openInventory(shopManager.openAdminGUI(shopId, name, 1));
                    player.sendMessage("§aĐang mở Chế độ Quản lý Shop (Mã cửa hàng: " + shopId + ")");
                } else {
                    // THÊM CHỮ player VÀO ĐÂY
                    player.openInventory(shopManager.openPlayerGUI(player, shopId, name, 1));
                }
            }
        }
    }

    // ==========================================
    // 2. LƯU DỮ LIỆU KHI ADMIN ĐÓNG SHOP
    // ==========================================
    @EventHandler
    public void onAdminCloseShop(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        ShopSession session = activeSessions.get(player.getUniqueId());

        if (session == null || !session.isAdmin) return;

        String title = event.getView().getTitle();
        if (!title.contains(" | Tr ")) return;

        int page = Integer.parseInt(title.substring(title.lastIndexOf("Tr ") + 3));
        Inventory inv = event.getInventory();

        for (int i = 0; i < 8; i++) {
            ItemStack coreItem = inv.getItem(i + 9);
            ItemStack sellCurrency = inv.getItem(i + 18);
            ItemStack buyCurrency = inv.getItem(i + 27);

            long sellPrice = currencyManager.getCurrencyValue(sellCurrency);
            long buyPrice = currencyManager.getCurrencyValue(buyCurrency);

            shopManager.saveShopItem(session.shopId, page, i, coreItem, sellPrice, buyPrice);
        }
        shopManager.saveShopToFile();
    }

    // ==========================================
    // 3. XỬ LÝ CLICK TRONG UI CỬA HÀNG
    // ==========================================
    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ShopSession session = activeSessions.get(player.getUniqueId());

        if (session == null) return;

        String title = event.getView().getTitle();
        if (!title.contains(" | Tr ")) return;

        int page = Integer.parseInt(title.substring(title.lastIndexOf("Tr ") + 3));

        if (!session.isAdmin) {
            event.setCancelled(true);
        } else {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().contains("GLASS_PANE")) {
                event.setCancelled(true);
            }
        }

        // ==========================================
        // CHUYỂN TRANG VÀ KIỂM TRA ĐIỀU KIỆN 8 SLOT
        // ==========================================
        if (event.getRawSlot() == 17) {
            event.setCancelled(true);

            if (event.isLeftClick()) {
                // ĐIỀU KIỆN ADMIN: Phải kín 8 ô mới cho tạo trang 2
                if (session.isAdmin) {
                    boolean isFull = true;
                    for (int i = 9; i <= 16; i++) {
                        ItemStack it = event.getInventory().getItem(i);
                        if (it == null || it.getType() == Material.AIR) {
                            isFull = false;
                            break;
                        }
                    }
                    if (!isFull) {
                        player.sendMessage("§cBạn phải đặt kín 8 vật phẩm ở trang này mới được mở Trang " + (page + 1) + "!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return; // Chặn lại ngay lập tức
                    }
                }
                // ĐIỀU KIỆN NGƯỜI CHƠI: Trang tiếp theo phải tồn tại
                else {
                    if (!shopManager.hasPage(session.shopId, page + 1)) {
                        return;
                    }
                }

                int newPage = page + 1;
                openNewPage(player, session, newPage);
            }
            else if (event.isRightClick() && page > 1) {
                openNewPage(player, session, page - 1);
            }
            return;
        }

        // ==========================================
        // XỬ LÝ MUA BÁN CỦA NGƯỜI CHƠI (BẰNG TIỀN MẶT)
        // ==========================================
        if (!session.isAdmin && event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
            int index = event.getRawSlot() - 9;
            vn.minecraft.rPGCore.managers.ShopManager.ShopItem shopItem = shopManager.getShopItem(session.shopId, page, index);
            if (shopItem == null) return;

            // 1. Đọc giá mặc định trước
            long finalSellPrice = shopItem.sellPrice;
            long finalBuyPrice = shopItem.buyPrice;

            // 2. Đọc giá đã Sale từ NBT (Nếu có Thương Nhân buff)
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = clickedItem.getItemMeta().getPersistentDataContainer();
                org.bukkit.NamespacedKey sellKey = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "shop_final_sell");
                org.bukkit.NamespacedKey buyKey = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "shop_final_buy");

                if (pdc.has(sellKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                    finalSellPrice = pdc.get(sellKey, org.bukkit.persistence.PersistentDataType.LONG);
                }
                if (pdc.has(buyKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                    finalBuyPrice = pdc.get(buyKey, org.bukkit.persistence.PersistentDataType.LONG);
                }
            }

            // BÁN VẬT PHẨM CHO SHOP -> Nhận Tiền
            if (event.isLeftClick()) {
                if (finalBuyPrice <= 0) return;

                if (player.getInventory().containsAtLeast(shopItem.item, shopItem.item.getAmount())) {
                    player.getInventory().removeItem(shopItem.item);
                    currencyManager.givePhysicalCurrency(player, finalBuyPrice); // Trả bằng giá NBT

                    player.sendMessage("§aĐã bán và nhận được " + currencyManager.formatCurrency(finalBuyPrice));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                    // [ĐÃ SỬA]: Truyền chính xác Số lượng vật phẩm bán và Số tiền kiếm được vào thanh tiến độ Quest
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SELL, shopItem.item.getAmount());
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_EARN, finalBuyPrice);
                    vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, finalBuyPrice);
                } else {
                    player.sendMessage("§cBạn không có đủ số lượng vật phẩm này để bán!");
                }
            }
            // MUA VẬT PHẨM TỪ SHOP -> Trừ Tiền Mặt
            else if (event.isRightClick()) {
                if (finalSellPrice <= 0) return;

                // Kiểm tra và trừ tiền mặt trong túi
                if (currencyManager.takePhysicalCurrency(player, finalSellPrice)) {
                    player.getInventory().addItem(shopItem.item.clone());

                    player.sendMessage("§aĐã mua thành công với giá " + currencyManager.formatCurrency(finalSellPrice));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

                    // [ĐÃ SỬA]: Truyền chính xác Số lượng vật phẩm mua và Số tiền tiêu tốn vào thanh tiến độ Quest
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_BUY, shopItem.item.getAmount());
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SPEND, finalSellPrice);
                    vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, finalSellPrice);
                } else {
                    player.sendMessage("§cBạn không có đủ tiền mặt trong túi đồ!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
    }

    private void openNewPage(Player player, ShopSession session, int newPage) {
        if (session.isAdmin) {
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), () -> {
                player.openInventory(shopManager.openAdminGUI(session.shopId, session.shopName, newPage));
            }, 2L);
        } else {
            // THÊM CHỮ player VÀO ĐÂY
            player.openInventory(shopManager.openPlayerGUI(player, session.shopId, session.shopName, newPage));
        }
    }
}