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
    // 1. CLICK VÀO NPC DÂN LÀNG
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

                // TẠO SHOP ID TỪ TỌA ĐỘ CỦA NPC (VD: world_10_65_20)
                Location loc = npc.getLocation();
                String shopId = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();

                // Phân quyền OP / Admin
                boolean isAdmin = player.isOp() || player.hasPermission("rpgcore.admin");

                // Lưu phiên làm việc
                activeSessions.put(player.getUniqueId(), new ShopSession(shopId, name, isAdmin));

                if (isAdmin) {
                    player.openInventory(shopManager.openAdminGUI(shopId, name, 1));
                    player.sendMessage("§aĐang mở Chế độ Quản lý Shop (Tọa độ: " + shopId + ")");
                } else {
                    player.openInventory(shopManager.openPlayerGUI(shopId, name, 1));
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
        // XỬ LÝ MUA BÁN CỦA NGƯỜI CHƠI
        // ==========================================
        if (!session.isAdmin && event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
            int index = event.getRawSlot() - 9;
            ShopManager.ShopItem shopItem = shopManager.getShopItem(session.shopId, page, index);
            if (shopItem == null) return;

            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

            if (event.isLeftClick()) {
                if (shopItem.buyPrice <= 0) return;
                if (player.getInventory().containsAtLeast(shopItem.item, shopItem.item.getAmount())) {
                    player.getInventory().removeItem(shopItem.item);
                    rpgPlayer.addBalance(shopItem.buyPrice);
                    player.sendMessage("§aĐã bán và nhận được " + currencyManager.formatCurrency(shopItem.buyPrice));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    player.sendMessage("§cBạn không có đủ vật phẩm để bán!");
                }
            } else if (event.isRightClick()) {
                if (shopItem.sellPrice <= 0) return;
                if (rpgPlayer.getBalance() >= shopItem.sellPrice) {
                    rpgPlayer.withdraw(shopItem.sellPrice);
                    player.getInventory().addItem(shopItem.item.clone());
                    player.sendMessage("§aĐã mua với giá " + currencyManager.formatCurrency(shopItem.sellPrice));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                } else {
                    player.sendMessage("§cBạn không đủ tiền!");
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
            player.openInventory(shopManager.openPlayerGUI(session.shopId, session.shopName, newPage));
        }
    }
}