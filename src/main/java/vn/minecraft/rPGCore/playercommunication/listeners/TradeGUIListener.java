package vn.minecraft.rPGCore.playercommunication.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.playercommunication.models.TradeSession;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TradeGUIListener implements Listener {

    private final RPGCore plugin;

    public TradeGUIListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTradeClick(InventoryClickEvent event) {
        // ==========================================
        // [ANTI-EXPLOIT TỐI THƯỢNG] - CHẶN MỌI ĐƯỜNG BUG BUKKIT
        // ==========================================
        // 1. Chặn bấm phím số 1-9, ném đồ, click đúp (Tránh tráo đồ lén)
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY ||
                event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK ||
                event.getClick() == org.bukkit.event.inventory.ClickType.DROP ||
                event.getClick() == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }

        // 2. Chặn Shift-Click tà đạo
        // Bắt buộc người chơi phải giữ chuột trái và kéo thả thủ công!
        if (event.isShiftClick()) {
            event.setCancelled(true);
            clicker.sendMessage("§c§l[!] §fĐể đảm bảo an toàn, vui lòng KÉO THẢ vật phẩm thủ công (Không Shift-Click)!");
            return;
        }
        
        if (!(event.getInventory().getHolder() instanceof TradeSession)) return;

        TradeSession session = (TradeSession) event.getInventory().getHolder();
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        boolean isPlayer1 = clicker.getUniqueId().equals(session.getPlayer1Id());
        boolean isPlayer2 = clicker.getUniqueId().equals(session.getPlayer2Id());

        if (!isPlayer1 && !isPlayer2) return; // Người lạ đang soi mói?

        // 1. NGĂN CHẶN MỌI TÁC ĐỘNG NẾU ĐANG TRONG QUÁ TRÌNH ĐẾM NGƯỢC
        if (session.getCountdownTask() != null) {
            event.setCancelled(true);
            return;
        }

        // 2. XỬ LÝ CLICK VÀO TÚI ĐỒ (BOTTOM INVENTORY)
        if (slot >= 54) {
            // Nếu người chơi đã Ready mà vẫn cố ném đồ từ túi lên -> Chặn!
            if ((isPlayer1 && session.isP1Ready()) || (isPlayer2 && session.isP2Ready())) {
                event.setCancelled(true);
                return;
            }
            // Hủy trạng thái Ready của CẢ HAI nếu có sự thay đổi đồ
            cancelReadyState(session);
            scheduleSummaryUpdate(session);
            return;
        }

        // 3. XỬ LÝ CLICK VÀO BÀN GIAO DỊCH (TOP INVENTORY)
        // Mặc định hủy event đối với các ô Kính đen / Giao diện
        if (!TradeSession.P1_SLOTS.contains(slot) && !TradeSession.P2_SLOTS.contains(slot)
                && slot != TradeSession.P1_READY_SLOT && slot != TradeSession.P2_READY_SLOT) {
            event.setCancelled(true);
        }

        // A. Nút Xác Nhận (Ready)
        if (isPlayer1 && slot == TradeSession.P1_READY_SLOT) {
            toggleReady(session, clicker, 1);
            return;
        }
        if (isPlayer2 && slot == TradeSession.P2_READY_SLOT) {
            toggleReady(session, clicker, 2);
            return;
        }

        // B. Khu vực Bỏ Đồ
        if (isPlayer1) {
            if (TradeSession.P2_SLOTS.contains(slot)) {
                event.setCancelled(true); // Không cho sờ vào đồ người khác
            } else if (TradeSession.P1_SLOTS.contains(slot)) {
                if (session.isP1Ready()) {
                    event.setCancelled(true); // Đã khóa đồ thì không được lấy ra
                } else {
                    cancelReadyState(session); // Đổi đồ -> Hủy Ready của người kia
                    scheduleSummaryUpdate(session);
                }
            }
        } else {
            if (TradeSession.P1_SLOTS.contains(slot)) {
                event.setCancelled(true);
            } else if (TradeSession.P2_SLOTS.contains(slot)) {
                if (session.isP2Ready()) {
                    event.setCancelled(true);
                } else {
                    cancelReadyState(session);
                    scheduleSummaryUpdate(session);
                }
            }
        }
    }

    // Khóa tính năng kéo rê chuột (Drag) trên các ô không hợp lệ
    @EventHandler
    public void onTradeDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TradeSession) {
            TradeSession session = (TradeSession) event.getInventory().getHolder();
            boolean isPlayer1 = event.getWhoClicked().getUniqueId().equals(session.getPlayer1Id());

            for (int slot : event.getRawSlots()) {
                if (slot < 54) {
                    if (isPlayer1 && !TradeSession.P1_SLOTS.contains(slot)) event.setCancelled(true);
                    if (!isPlayer1 && !TradeSession.P2_SLOTS.contains(slot)) event.setCancelled(true);
                }
            }
            if (!event.isCancelled()) {
                cancelReadyState(session);
                scheduleSummaryUpdate(session);
            }
        }
    }

    // Đóng GUI đột ngột -> Hủy Giao dịch và trả đồ
    @EventHandler
    public void onTradeClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeSession)) return;
        TradeSession session = (TradeSession) event.getInventory().getHolder();

        // Nếu Task đếm ngược đang chạy, hủy nó đi
        if (session.getCountdownTask() != null) {
            session.getCountdownTask().cancel();
        }

        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());

        // Lấy lại đồ thả vào túi (Nếu đầy thì rớt ra đất)
        returnItemsTo(p1, session.getInventory(), TradeSession.P1_SLOTS);
        returnItemsTo(p2, session.getInventory(), TradeSession.P2_SLOTS);

        if (p1 != null && p1.isOnline()) {
            p1.sendMessage("§c§l[!] §cGiao dịch đã bị hủy!");
            p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
        if (p2 != null && p2.isOnline()) {
            p2.sendMessage("§c§l[!] §cGiao dịch đã bị hủy!");
            p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }

        // Xóa khỏi Manager (Bác có thể gọi qua TradeManager nếu đã viết)
         plugin.getTradeManager().getActiveTrades().remove(session.getPlayer1Id());
         plugin.getTradeManager().getActiveTrades().remove(session.getPlayer2Id());
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ LOGIC (TOGGLE, COUNTDOWN, SUMMARY)
    // ==========================================

    private void toggleReady(TradeSession session, Player clicker, int playerNum) {
        Inventory inv = session.getInventory();
        ItemStack readyItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = readyItem.getItemMeta();
        meta.setDisplayName("§a§lĐÃ SẴN SÀNG");
        meta.setLore(Arrays.asList("§7Click lại để Hủy khóa đồ."));
        readyItem.setItemMeta(meta);

        ItemStack notReadyItem = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta2 = notReadyItem.getItemMeta();
        meta2.setDisplayName("§e§lCHƯA SẴN SÀNG");
        meta2.setLore(Arrays.asList("§7Click để Khóa đồ và Xác nhận."));
        notReadyItem.setItemMeta(meta2);

        if (playerNum == 1) {
            session.setP1Ready(!session.isP1Ready());
            inv.setItem(TradeSession.P1_READY_SLOT, session.isP1Ready() ? readyItem : notReadyItem);
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else {
            session.setP2Ready(!session.isP2Ready());
            inv.setItem(TradeSession.P2_READY_SLOT, session.isP2Ready() ? readyItem : notReadyItem);
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }

        // Nếu CẢ 2 ĐỀU SẴN SÀNG -> Bắt đầu đếm ngược!
        if (session.isP1Ready() && session.isP2Ready()) {
            startCountdown(session);
        }
    }

    private void cancelReadyState(TradeSession session) {
        session.setP1Ready(false);
        session.setP2Ready(false);

        ItemStack notReadyItem = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = notReadyItem.getItemMeta();
        meta.setDisplayName("§e§lCHƯA SẴN SÀNG");
        meta.setLore(Arrays.asList("§7Click để Khóa đồ và Xác nhận."));
        notReadyItem.setItemMeta(meta);

        session.getInventory().setItem(TradeSession.P1_READY_SLOT, notReadyItem);
        session.getInventory().setItem(TradeSession.P2_READY_SLOT, notReadyItem);
    }

    private void startCountdown(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());
        if (p1 != null) p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        if (p2 != null) p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

        session.setCountdownSeconds(3);

        session.setCountdownTask(new BukkitRunnable() {
            @Override
            public void run() {
                int sec = session.getCountdownSeconds();

                if (sec > 0) {
                    // Update Kính đếm ngược (Tím -> Xanh)
                    ItemStack tickGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    ItemMeta meta = tickGlass.getItemMeta();
                    meta.setDisplayName("§a§lGIAO DỊCH SAU " + sec + " GIÂY...");
                    tickGlass.setItemMeta(meta);

                    // Slot mảng: {48, 49, 50}. Giây 3 -> Kính ở index 0 (Slot 48) đổi màu.
                    int indexToChange = 3 - sec;
                    session.getInventory().setItem(TradeSession.COUNTDOWN_SLOTS[indexToChange], tickGlass);

                    if (p1 != null) p1.playSound(p1.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                    if (p2 != null) p2.playSound(p2.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);

                    session.setCountdownSeconds(sec - 1);
                } else {
                    // KẾT THÚC ĐẾM NGƯỢC -> GIAO DỊCH THÀNH CÔNG!
                    executeTrade(session);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    private void executeTrade(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());

        // Đẩy đồ của P1 (Bên Trái) vào túi của P2
        returnItemsTo(p2, session.getInventory(), TradeSession.P1_SLOTS);

        // Đẩy đồ của P2 (Bên Phải) vào túi của P1
        returnItemsTo(p1, session.getInventory(), TradeSession.P2_SLOTS);

        if (p1 != null) {
            p1.closeInventory();
            p1.sendMessage("§a§l[Giao Dịch] §fTrao đổi thành công!");
            p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        if (p2 != null) {
            p2.closeInventory();
            p2.sendMessage("§a§l[Giao Dịch] §fTrao đổi thành công!");
            p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

         plugin.getTradeManager().saveTradeHistory(session);
    }

    // ==========================================
    // THUẬT TOÁN HÓA ĐƠN TIỀN VẬT LÝ (HYBRID)
    // ==========================================
    private void scheduleSummaryUpdate(TradeSession session) {
        // Trì hoãn 1 Tick để Inventory kịp cập nhật Item mới đặt vào
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            long p1Total = calculateTotalCopper(session.getInventory(), TradeSession.P1_SLOTS);
            long p2Total = calculateTotalCopper(session.getInventory(), TradeSession.P2_SLOTS);

            ItemStack p1Sum = new ItemStack(Material.PAPER);
            ItemMeta m1 = p1Sum.getItemMeta();
            m1.setDisplayName("§a§lBiên Lai Của Trái");
            m1.setLore(Arrays.asList("§fTổng tiền mặt: §e" + plugin.getCurrencyManager().formatCurrency(p1Total)));
            p1Sum.setItemMeta(m1);

            ItemStack p2Sum = new ItemStack(Material.PAPER);
            ItemMeta m2 = p2Sum.getItemMeta();
            m2.setDisplayName("§a§lBiên Lai Của Phải");
            m2.setLore(Arrays.asList("§fTổng tiền mặt: §e" + plugin.getCurrencyManager().formatCurrency(p2Total)));
            p2Sum.setItemMeta(m2);

            session.getInventory().setItem(TradeSession.P1_SUMMARY_SLOT, p1Sum);
            session.getInventory().setItem(TradeSession.P2_SUMMARY_SLOT, p2Sum);
        }, 1L);
    }

    private long calculateTotalCopper(Inventory inv, Set<Integer> slots) {
        long total = 0;
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            // Tận dụng ngay hàm getCurrencyValue có sẵn để lấy giá trị chính xác
            total += plugin.getCurrencyManager().getCurrencyValue(item);
        }
        return total;
    }

    private void returnItemsTo(Player player, Inventory inv, Set<Integer> slots) {
        if (player == null || !player.isOnline()) return;
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> left = player.getInventory().addItem(item.clone());
                for (ItemStack drop : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                inv.setItem(slot, null);
            }
        }
    }
}