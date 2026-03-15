package vn.minecraft.rPGCore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.BankManager;
import vn.minecraft.rPGCore.managers.CurrencyManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class BankListener implements Listener {

    private final BankManager bankManager;
    private final CurrencyManager currencyManager;
    private final PlayerManager playerManager;
    private final RPGCore plugin;

    public BankListener(RPGCore plugin, BankManager bankManager, CurrencyManager currencyManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.playerManager = playerManager;
    }

    // ==========================================
    // 1. CLICK VÀO NPC NGÂN HÀNG
    // ==========================================
    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof LivingEntity) {
            LivingEntity npc = (LivingEntity) event.getRightClicked();
            String name = npc.getCustomName();

            // Check nếu tên NPC là "Thủ Quỹ" hoặc "Ngân Hàng"
            if (name != null && (name.contains("Thủ Quỹ") || name.contains("Ngân Hàng"))) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

                if (rpgPlayer != null) {
                    player.openInventory(bankManager.openBankMainUI(player, rpgPlayer.getBalance()));
                }
            }
        }
    }

    // ==========================================
    // 2. XỬ LÝ CLICK TRONG GUI NGÂN HÀNG
    // ==========================================
    @EventHandler
    public void onBankClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§0§lNGÂN HÀNG") && !title.startsWith("§0§lRÚT TIỀN")) return;

        event.setCancelled(true); // Ngăn lấy đồ trong GUI
        if (event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        int slot = event.getRawSlot();

        // --- MENU CHÍNH ---
        if (title.equals("§0§lNGÂN HÀNG TRUNG ƯƠNG")) {
            if (slot == 11) { // NÚT NẠP TẤT CẢ
                currencyManager.depositAll(player, rpgPlayer);
                player.openInventory(bankManager.openBankMainUI(player, rpgPlayer.getBalance())); // Reload UI
            }
            else if (slot == 15) { // NÚT RÚT TIỀN (Mở UI Rút)
                player.openInventory(bankManager.openWithdrawUI(rpgPlayer.getBalance()));
            }
        }
        // --- MENU RÚT TIỀN ---
        else if (title.equals("§0§lRÚT TIỀN MẶT")) {
            if (slot == 26) { // Quay lại
                player.openInventory(bankManager.openBankMainUI(player, rpgPlayer.getBalance()));
                return;
            }

            long cost = 0;
            String type = "";
            if (slot == 11) { cost = 4096; type = "gold"; }
            else if (slot == 13) { cost = 64; type = "iron"; }
            else if (slot == 15) { cost = 1; type = "copper"; }

            if (cost > 0) {
                if (rpgPlayer.withdraw(cost)) {
                    currencyManager.giveCurrencySafe(player, type, 1);
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                    player.sendMessage("§aBạn đã rút thành công 1 " + (type.equals("gold") ? "Xu Vàng" : type.equals("iron") ? "Xu Bạc" : "Xu Đồng"));
                } else {
                    player.sendMessage("§cSố dư của bạn không đủ!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
    }

    // ==========================================
    // 3. AUTO-COMPRESSION KHI NHẶT TIỀN
    // ==========================================
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack item = event.getItem().getItemStack();
        // Nếu nhặt trúng Vật phẩm Tiền tệ
        if (currencyManager.getCurrencyValue(item) > 0) {
            Player player = (Player) event.getEntity();

            // Delay 1 tick để item thực sự chạy vào rương rồi mới nén
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                currencyManager.compressCurrency(player);
            }, 1L);
        }
    }
}