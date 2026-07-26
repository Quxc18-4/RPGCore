package vn.minecraft.rPGCore.playercommunication.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
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
import java.util.Set;

public class TradeGUIListener implements Listener {

    private final RPGCore plugin;

    public TradeGUIListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeSession)) return;

        TradeSession session = (TradeSession) event.getInventory().getHolder();
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (event.getClick() == ClickType.NUMBER_KEY ||
                event.getClick() == ClickType.DOUBLE_CLICK ||
                event.getClick() == ClickType.DROP ||
                event.getClick() == ClickType.CONTROL_DROP ||
                event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            clicker.updateInventory();
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            clicker.sendMessage("§c§l[!] §fVui lòng KÉO THẢ vật phẩm thủ công (Không Shift-Click)!");
            clicker.updateInventory();
            return;
        }

        boolean isPlayer1 = clicker.getUniqueId().equals(session.getPlayer1Id());
        boolean isPlayer2 = clicker.getUniqueId().equals(session.getPlayer2Id());
        if (!isPlayer1 && !isPlayer2) return;

        if (session.getCountdownTask() != null) {
            event.setCancelled(true);
            clicker.updateInventory();
            return;
        }

        if (slot >= 54) {
            if ((isPlayer1 && session.isP1Ready()) || (isPlayer2 && session.isP2Ready())) {
                event.setCancelled(true);
                clicker.updateInventory();
                return;
            }
            cancelReadyState(session);
            scheduleSummaryUpdate(session);
            return;
        }

        boolean isP1Slot = TradeSession.P1_SLOTS.contains(slot);
        boolean isP2Slot = TradeSession.P2_SLOTS.contains(slot);

        if (!isP1Slot && !isP2Slot) {
            event.setCancelled(true);
            clicker.updateInventory();

            // Cho phép thao tác với nút của chính mình
            if (isPlayer1 && slot == TradeSession.P1_READY_SLOT) {
                toggleReady(session, clicker, 1);
            } else if (isPlayer2 && slot == TradeSession.P2_READY_SLOT) {
                toggleReady(session, clicker, 2);
            }
            return;
        }

        if (isPlayer1) {
            if (isP2Slot) {
                event.setCancelled(true);
                clicker.updateInventory();
            } else if (isP1Slot) {
                if (session.isP1Ready()) {
                    event.setCancelled(true);
                    clicker.updateInventory();
                } else {
                    cancelReadyState(session);
                    scheduleSummaryUpdate(session);
                }
            }
        } else {
            if (isP1Slot) {
                event.setCancelled(true);
                clicker.updateInventory();
            } else if (isP2Slot) {
                if (session.isP2Ready()) {
                    event.setCancelled(true);
                    clicker.updateInventory();
                } else {
                    cancelReadyState(session);
                    scheduleSummaryUpdate(session);
                }
            }
        }
    }

    @EventHandler
    public void onTradeDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeSession)) return;
        TradeSession session = (TradeSession) event.getInventory().getHolder();
        Player clicker = (Player) event.getWhoClicked();

        if (session.getCountdownTask() != null) {
            event.setCancelled(true);
            clicker.updateInventory();
            return;
        }

        boolean isPlayer1 = clicker.getUniqueId().equals(session.getPlayer1Id());

        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                if (isPlayer1 && !TradeSession.P1_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    break;
                }
                if (!isPlayer1 && !TradeSession.P2_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    break;
                }
            }
        }

        if (event.isCancelled() || ((isPlayer1 && session.isP1Ready()) || (!isPlayer1 && session.isP2Ready()))) {
            event.setCancelled(true);
            clicker.updateInventory();
        } else {
            cancelReadyState(session);
            scheduleSummaryUpdate(session);
        }
    }

    @EventHandler
    public void onTradeClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeSession)) return;
        TradeSession session = (TradeSession) event.getInventory().getHolder();

        // [FIX DUP LOG]: Nếu session đã hoàn tất thì câm nín, không chạy logic Hủy
        if (session.isCompleted()) return;

        // [QUAN TRỌNG]: Đánh dấu hủy ngay lập tức để người thứ 2 đóng UI không kích hoạt lại
        session.setCompleted(true);

        if (session.getCountdownTask() != null) {
            session.getCountdownTask().cancel();
            session.setCountdownTask(null);
        }

        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());

        returnItemsTo(p1, session.getInventory(), TradeSession.P1_SLOTS);
        returnItemsTo(p2, session.getInventory(), TradeSession.P2_SLOTS);

        plugin.getTradeManager().getActiveTrades().remove(session.getPlayer1Id());
        plugin.getTradeManager().getActiveTrades().remove(session.getPlayer2Id());

        // [FIX LỖI 1 NGƯỜI ĐÓNG 1 NGƯỜI MỞ]: Ép đối phương phải tắt rương theo
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p1 != null && p1.isOnline()) {
                if (p1.getOpenInventory().getTopInventory().getHolder() instanceof TradeSession) {
                    p1.closeInventory();
                }
                p1.sendMessage("§c§l[!] §cGiao dịch đã bị hủy!");
                p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            }
            if (p2 != null && p2.isOnline()) {
                if (p2.getOpenInventory().getTopInventory().getHolder() instanceof TradeSession) {
                    p2.closeInventory();
                }
                p2.sendMessage("§c§l[!] §cGiao dịch đã bị hủy!");
                p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            }
        });
    }

    // ==========================================
    // LOGIC NÚT ĐỎ - VÀNG - XANH
    // ==========================================
    private void toggleReady(TradeSession session, Player clicker, int playerNum) {
        // [FIX LỖI NÚT SẴN SÀNG]: Đảo trạng thái True <-> False
        if (playerNum == 1) session.setP1Ready(!session.isP1Ready());
        else session.setP2Ready(!session.isP2Ready());

        clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // HỦY ĐẾM NGƯỢC NẾU CÓ 1 NGƯỜI TẮT READY
        if (!session.isP1Ready() || !session.isP2Ready()) {
            if (session.getCountdownTask() != null) {
                session.getCountdownTask().cancel();
                session.setCountdownTask(null);
                resetCenterColumn(session); // Nhả lại vách ngăn xanh lá
            }
        }

        updateReadyButtons(session);

        // Nếu cả 2 cùng bật Ready thì lại đếm ngược
        if (session.isP1Ready() && session.isP2Ready()) {
            startCountdown(session);
        }
    }

    private void cancelReadyState(TradeSession session) {
        if (!session.isP1Ready() && !session.isP2Ready()) return; // Tối ưu, tránh update màu thừa
        session.setP1Ready(false);
        session.setP2Ready(false);

        if (session.getCountdownTask() != null) {
            session.getCountdownTask().cancel();
            session.setCountdownTask(null);
            resetCenterColumn(session);
        }
        updateReadyButtons(session);
    }

    private void updateReadyButtons(TradeSession session) {
        Inventory inv = session.getInventory();
        boolean p1Ready = session.isP1Ready();
        boolean p2Ready = session.isP2Ready();

        inv.setItem(TradeSession.P1_READY_SLOT, getReadyBtnState(p1Ready, p2Ready));
        inv.setItem(TradeSession.P2_READY_SLOT, getReadyBtnState(p2Ready, p1Ready));
    }

    private ItemStack getReadyBtnState(boolean myReady, boolean otherReady) {
        if (myReady && otherReady) {
            return createItem(Material.LIME_STAINED_GLASS_PANE, "§a§lĐÃ XÁC NHẬN", "§7Giao dịch chuẩn bị diễn ra...");
        } else if (myReady) {
            return createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lĐANG CHỜ ĐỐI PHƯƠNG...", "§7Click lại để Hủy khóa đồ.");
        } else {
            return createItem(Material.RED_STAINED_GLASS_PANE, "§c§lCHƯA SẴN SÀNG", "§7Click để Khóa đồ và Xác nhận.");
        }
    }

    // ==========================================
    // LOGIC ĐẾM NGƯỢC ĐA MÀU
    // ==========================================
    private void startCountdown(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());

        session.setCountdownSeconds(4);

        session.setCountdownTask(new BukkitRunnable() {
            @Override
            public void run() {
                int sec = session.getCountdownSeconds();
                if (sec > 0) {
                    Material glassMat;
                    String title;

                    if (sec == 4) { glassMat = Material.WHITE_STAINED_GLASS_PANE; title = "§f§lCHUẨN BỊ..."; }
                    else if (sec == 3) { glassMat = Material.RED_STAINED_GLASS_PANE; title = "§c§l3..."; }
                    else if (sec == 2) { glassMat = Material.YELLOW_STAINED_GLASS_PANE; title = "§e§l2..."; }
                    else { glassMat = Material.LIME_STAINED_GLASS_PANE; title = "§a§l1..."; }

                    ItemStack tickGlass = createItem(glassMat, title);
                    for (int slot : TradeSession.COUNTDOWN_SLOTS) {
                        session.getInventory().setItem(slot, tickGlass);
                    }

                    // [FIX MÀU KÍNH]: Ép Client tải lại màu sắc để chống Desync
                    if (p1 != null) {
                        p1.playSound(p1.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                        p1.updateInventory();
                    }
                    if (p2 != null) {
                        p2.playSound(p2.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                        p2.updateInventory();
                    }

                    session.setCountdownSeconds(sec - 1);
                } else {
                    executeTrade(session);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 20L));
    }

    private void resetCenterColumn(TradeSession session) {
        ItemStack greenGlass = createItem(Material.LIME_STAINED_GLASS_PANE, "§a§lVách Ngăn");
        for (int slot : TradeSession.COUNTDOWN_SLOTS) {
            session.getInventory().setItem(slot, greenGlass);
        }

        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());
        if (p1 != null) p1.updateInventory();
        if (p2 != null) p2.updateInventory();
    }

    private void executeTrade(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1Id());
        Player p2 = Bukkit.getPlayer(session.getPlayer2Id());

        // ==========================================
        // [TASK MỚI]: PRE-CHECK CHỐNG TRÀN TÚI ĐỒ (INVENTORY FULL)
        // ==========================================
        if (p1 != null && p2 != null) {
            int p1IncomingCount = getIncomingItemCount(session.getInventory(), TradeSession.P2_SLOTS);
            int p2IncomingCount = getIncomingItemCount(session.getInventory(), TradeSession.P1_SLOTS);

            if (getFreeSlots(p1) < p1IncomingCount || getFreeSlots(p2) < p2IncomingCount) {
                session.setCompleted(true); // Đánh dấu hoàn tất để ngắt log phụ

                // Trả đồ của ai về nhà nấy
                returnItemsTo(p1, session.getInventory(), TradeSession.P1_SLOTS);
                returnItemsTo(p2, session.getInventory(), TradeSession.P2_SLOTS);

                plugin.getTradeManager().getActiveTrades().remove(session.getPlayer1Id());
                plugin.getTradeManager().getActiveTrades().remove(session.getPlayer2Id());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    p1.closeInventory();
                    p2.closeInventory();
                    p1.sendMessage("§c§l[!] §cTúi đồ của một trong hai người không đủ chỗ trống để nhận đồ!");
                    p2.sendMessage("§c§l[!] §cTúi đồ của một trong hai người không đủ chỗ trống để nhận đồ!");
                    p1.playSound(p1.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    p2.playSound(p2.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                });
                return; // Ngắt ngang, không giao dịch nữa
            }
        }

        session.setCompleted(true);
        plugin.getTradeManager().saveTradeHistory(session);

        returnItemsTo(p2, session.getInventory(), TradeSession.P1_SLOTS);
        returnItemsTo(p1, session.getInventory(), TradeSession.P2_SLOTS);

        plugin.getTradeManager().getActiveTrades().remove(session.getPlayer1Id());
        plugin.getTradeManager().getActiveTrades().remove(session.getPlayer2Id());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p1 != null && p1.isOnline()) {
                p1.closeInventory();
                p1.sendMessage("§a§l[Giao Dịch] §fTrao đổi thành công!");
                p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            if (p2 != null && p2.isOnline()) {
                p2.closeInventory();
                p2.sendMessage("§a§l[Giao Dịch] §fTrao đổi thành công!");
                p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
        });
    }

    // ==========================================
    // HÀM HỖ TRỢ ĐẾM Ô TRỐNG
    // ==========================================
    private int getFreeSlots(Player player) {
        int free = 0;
        // Chỉ đếm 36 ô chính của túi đồ, không đếm ô giáp
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                free++;
            }
        }
        return free;
    }

    private int getIncomingItemCount(Inventory inv, Set<Integer> slots) {
        int count = 0;
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) count++;
        }
        return count;
    }

    // ... (Giữ nguyên các hàm scheduleSummaryUpdate và returnItemsTo ở dưới)

    // ==========================================
    // HÓA ĐƠN & TIỆN ÍCH
    // ==========================================
    private void scheduleSummaryUpdate(TradeSession session) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            long p1Total = calculateTotalCopper(session.getInventory(), TradeSession.P1_SLOTS);
            long p2Total = calculateTotalCopper(session.getInventory(), TradeSession.P2_SLOTS);

            ItemStack summary = createItem(Material.PAPER, "§a§lHÓA ĐƠN GIAO DỊCH",
                    "§e<========",
                    "§f▶ §b" + session.getPlayer1Name() + "§f: §a" + plugin.getCurrencyManager().formatCurrency(p1Total),
                    "§f▶ §c" + session.getPlayer2Name() + "§f: §a" + plugin.getCurrencyManager().formatCurrency(p2Total),
                    "§e=========>"
            );
            session.getInventory().setItem(TradeSession.SUMMARY_SLOT, summary);
        }, 1L);
    }

    private long calculateTotalCopper(Inventory inv, Set<Integer> slots) {
        long total = 0;
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
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

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}