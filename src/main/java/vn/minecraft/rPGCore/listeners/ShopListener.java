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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.CurrencyManager;
import vn.minecraft.rPGCore.managers.ShopManager;
import vn.minecraft.rPGCore.player.PlayerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopListener implements Listener {

    private final ShopManager shopManager;
    private final CurrencyManager currencyManager;

    public static class ShopSession {
        public String shopId;
        public String shopName;
        public boolean isAdmin;
        public ShopSession(String id, String name, boolean admin) {
            this.shopId = id; this.shopName = name; this.isAdmin = admin;
        }
    }
    private final Map<UUID, ShopSession> activeSessions = new HashMap<>();

    public ShopListener(ShopManager shopManager, CurrencyManager currencyManager) {
        this.shopManager = shopManager;
        this.currencyManager = currencyManager;
    }

    // ==========================================
    // 1. CLICK VÀO NPC (ĐÃ CẬP NHẬT TÍNH NĂNG XÓA CHO ADMIN)
    // ==========================================
    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        // ------------------------------------------
        // [MỚI]: KIỂM TRA SHIFT + CLICK (THU HỒI NPC TAY KHÔNG)
        // ------------------------------------------
        if (player.isSneaking()) {
            if (event.getRightClicked() instanceof LivingEntity) {
                // Lấy dữ liệu NPC từ Core để kiểm tra ID Chủ sở hữu
                vn.minecraft.rPGCore.managers.NPCManager.ShopNPC npcData =
                        vn.minecraft.rPGCore.RPGCore.getInstance().getNpcManager().getNPCByEntityUUID(event.getRightClicked().getUniqueId());

                // NẾU ĐÂY LÀ NPC HỆ THỐNG (Không có ownerId)
                if (npcData != null && npcData.ownerId == null) {
                    if (player.hasPermission("rpgcore.admin")) {
                        ItemStack mainHand = player.getInventory().getItemInMainHand();

                        // Yêu cầu Admin phải CẦM TAY KHÔNG mới được xóa (Chống trượt tay)
                        if (mainHand == null || mainHand.getType() == Material.AIR) {
                            event.setCancelled(true);

                            // 1. Xóa sạch dữ liệu Shop khỏi MongoDB
                            try {
                                org.bson.Document query = new org.bson.Document("_id", npcData.shopId);
                                vn.minecraft.rPGCore.database.MongoManager.getInstance().getDatabase().getCollection("shops").deleteOne(query);
                            } catch (Exception e) {
                                vn.minecraft.rPGCore.RPGCore.getInstance().getLogger().warning("Không thể xóa MongoDB Shop: " + e.getMessage());
                            }

                            // 2. Xóa Entity NPC khỏi Server
                            vn.minecraft.rPGCore.RPGCore.getInstance().getNpcManager().removeNPC(npcData.id);

                            player.sendMessage("§a§l[Hệ thống] §fĐã thu hồi NPC Server và dọn dẹp dữ liệu cửa hàng!");
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                        }
                    }
                    // Đã là NPC Hệ thống thì kết thúc luôn, không đẩy sang cho RPGDens xử lý
                    return;
                }
            }

            // Nếu là NPC của người chơi (Có ownerId) -> Nhường quyền 100% cho NpcInteractListener của RPGDens!
            return;
        }

        // ------------------------------------------
        // [CŨ]: CLICK PHẢI BÌNH THƯỜNG -> MỞ GIAO DIỆN
        // ------------------------------------------
        if (event.getRightClicked() instanceof LivingEntity) {
            LivingEntity npc = (LivingEntity) event.getRightClicked();
            String name = npc.getCustomName();

            org.bukkit.persistence.PersistentDataContainer pdc = npc.getPersistentDataContainer();
            String shopId = null;

            org.bukkit.NamespacedKey newManagerKey = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "npc_shop_id");
            if (pdc.has(newManagerKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                shopId = pdc.get(newManagerKey, org.bukkit.persistence.PersistentDataType.STRING);
                // [NÉ NGÂN HÀNG]
                if (shopId != null && shopId.startsWith("bank_")) {
                    return;
                }
            }
            else if (name != null && name.contains("Thương Nhân")) {
                org.bukkit.NamespacedKey oldKey = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "shop_id");
                if (pdc.has(oldKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    shopId = pdc.get(oldKey, org.bukkit.persistence.PersistentDataType.STRING);
                } else {
                    Location loc = npc.getLocation();
                    shopId = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
                    pdc.set(oldKey, org.bukkit.persistence.PersistentDataType.STRING, shopId);
                }
            }

            if (shopId != null) {
                event.setCancelled(true);
                npc.setCollidable(false);

                String shopName = name != null ? name : "Cửa Hàng";
                boolean isAdmin = player.isOp() || player.hasPermission("rpgcore.admin");

                activeSessions.put(player.getUniqueId(), new ShopSession(shopId, shopName, isAdmin));

                if (isAdmin) {
                    player.openInventory(shopManager.openAdminGUI(shopId, shopName, 1));
                    player.sendMessage("§aĐang mở Chế độ Quản lý Shop (Mã cửa hàng: " + shopId + ")");
                } else {
                    player.openInventory(shopManager.openPlayerGUI(player, shopId, shopName, 1));
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

        // [SỬA LỖI UI 9 Ô]: Chỉ quét 8 ô đầu tiên (0 đến 7)
        for (int i = 0; i < 8; i++) {
            ItemStack coreItem = inv.getItem(i);

            // Ép giá sellPrice và buyPrice về 0 vì nó đã được quản lý ở GlobalEconomyManager
            shopManager.saveShopItem(session.shopId, page, i, coreItem, 0, 0);
        }

        shopManager.saveShopToDB(session.shopId);
        player.sendMessage("§a[Hệ thống] Đã tự động lưu dữ liệu cửa hàng thành công!");
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

        // ==========================================
        // [MỚI] KIỂM SOÁT ĐẦU VÀO CỦA ADMIN
        // ==========================================
        if (session.isAdmin) {
            // Cấm lấy Nút chuyển trang ra ngoài
            if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().contains("GLASS_PANE")) {
                event.setCancelled(true);
            }

            // Bắt sự kiện Admin đặt Item vào Shop (ô 0 đến 7)
            Inventory topInv = event.getView().getTopInventory();
            ItemStack addedItem = null;

            // Trường hợp 1: Admin cầm Item click thẳng vào ô Shop
            if (event.getClickedInventory() == topInv && event.getSlot() < 8) {
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    addedItem = event.getCursor();
                }
            }
            // Trường hợp 2: Admin Shift-Click item từ túi đồ lên Shop
            else if (event.getClickedInventory() != topInv && event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY")) {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    addedItem = event.getCurrentItem();
                }
            }

            // XÁC THỰC: Có trong YAML hay không?
            if (addedItem != null) {
                String itemId = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().getItemId(addedItem);

                if (itemId == null || !vn.minecraft.rPGCore.RPGCore.getInstance().getGlobalEconomyManager().isItemInEconomy(itemId)) {
                    event.setCancelled(true); // Ngăn không cho đặt vào
                    player.sendMessage("§c§l[!] §cVật phẩm này chưa được định giá trong hệ thống Kinh tế (economy_base.yml)!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        } else {
            // Người chơi thường không được thay đổi các slot
            event.setCancelled(true);
        }

        // --- XỬ LÝ NÚT CHUYỂN TRANG (Ô 8 với Admin, Ô 17 với Player) ---
        int nextBtnSlot = session.isAdmin ? 8 : 17;

        if (event.getRawSlot() == nextBtnSlot) {
            event.setCancelled(true);

            if (event.isLeftClick()) {
                if (session.isAdmin) {
                    boolean isFull = true;
                    // [SỬA LỖI UI 9 Ô]: Quét ô 0 đến 7
                    for (int i = 0; i < 8; i++) {
                        ItemStack it = event.getInventory().getItem(i);
                        if (it == null || it.getType() == Material.AIR) {
                            isFull = false;
                            break;
                        }
                    }
                    if (!isFull) {
                        player.sendMessage("§cBạn phải đặt kín 8 vật phẩm ở trang này mới được mở Trang " + (page + 1) + "!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                }
                else {
                    if (!shopManager.hasPage(session.shopId, page + 1)) return;
                }

                int newPage = page + 1;
                openNewPage(player, session, newPage);
            }
            else if (event.isRightClick() && page > 1) {
                openNewPage(player, session, page - 1);
            }
            return;
        }

        // --- XỬ LÝ NGƯỜI CHƠI MUA/BÁN ---
        if (!session.isAdmin && event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
            int index = event.getRawSlot() - 9;
            vn.minecraft.rPGCore.managers.ShopManager.ShopItem shopItem = shopManager.getShopItem(session.shopId, page, index);
            if (shopItem == null || shopItem.item == null) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            long finalSellPrice = 0;
            long finalBuyPrice = 0;

            org.bukkit.persistence.PersistentDataContainer pdc = clickedItem.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey sellKey = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "shop_final_sell");
            org.bukkit.NamespacedKey buyKey = new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "shop_final_buy");

            if (pdc.has(sellKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                finalSellPrice = pdc.get(sellKey, org.bukkit.persistence.PersistentDataType.LONG);
            }
            if (pdc.has(buyKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                finalBuyPrice = pdc.get(buyKey, org.bukkit.persistence.PersistentDataType.LONG);
            }

            if (event.isLeftClick()) { // BÁN CHO SHOP
                if (finalBuyPrice <= 0) return;

                if (player.getInventory().containsAtLeast(shopItem.item, shopItem.item.getAmount())) {
                    player.getInventory().removeItem(shopItem.item);
                    currencyManager.givePhysicalCurrency(player, finalBuyPrice);

                    player.sendMessage("§aĐã bán và nhận được " + currencyManager.formatCurrency(finalBuyPrice));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                    // Ghi nhận biến động kinh tế
                    vn.minecraft.rPGCore.RPGCore.getInstance().getGlobalEconomyManager().recordTransaction(shopItem.item, shopItem.item.getAmount(), false);

                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SELL, shopItem.item.getAmount());
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_EARN, finalBuyPrice);
                    vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, finalBuyPrice);
                } else {
                    player.sendMessage("§cBạn không có đủ số lượng vật phẩm này để bán!");
                }
            }
            else if (event.isRightClick()) { // MUA TỪ SHOP
                if (finalSellPrice <= 0) return;

                if (currencyManager.takePhysicalCurrency(player, finalSellPrice)) {
                    player.getInventory().addItem(shopItem.item.clone());

                    player.sendMessage("§aĐã mua thành công với giá " + currencyManager.formatCurrency(finalSellPrice));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

                    // Ghi nhận biến động kinh tế
                    vn.minecraft.rPGCore.RPGCore.getInstance().getGlobalEconomyManager().recordTransaction(shopItem.item, shopItem.item.getAmount(), true);

                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_BUY, shopItem.item.getAmount());
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SPEND, finalSellPrice);
                    vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, finalSellPrice);
                } else {
                    player.sendMessage("§cBạn không có đủ tiền mặt trong túi đồ!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }

            // Load lại UI để cập nhật giá trị mới nếu bị ảnh hưởng
            openNewPage(player, session, page);
        }
    }

    private void openNewPage(Player player, ShopSession session, int newPage) {
        if (session.isAdmin) {
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), () -> {
                player.openInventory(shopManager.openAdminGUI(session.shopId, session.shopName, newPage));
            }, 2L);
        } else {
            player.openInventory(shopManager.openPlayerGUI(player, session.shopId, session.shopName, newPage));
        }
    }
}