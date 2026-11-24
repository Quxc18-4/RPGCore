package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CurrencyManager;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Arrays;

public class AttrGUI implements Listener {

    private final PlayerManager playerManager;
    private final String GUI_TITLE = "§8§lNÂNG CẤP CHỈ SỐ";

    // 10 Vàng = 10 * 4,096 Đồng = 40,960 Đồng
    private static final long FULL_RESET_COST_COPPER = 10L * 4096L;

    public AttrGUI(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void openGUI(Player player) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 27; i++) {
            if (i < 10 || i > 16) {
                inv.setItem(i, border);
            }
        }

        int unspent = rpgPlayer.getAttributeData().getUnspentAttrPoints();
        CurrencyManager currencyManager = (RPGCore.getInstance() != null) ? RPGCore.getInstance().getCurrencyManager() : null;
        String balanceText = (currencyManager != null)
                ? currencyManager.formatCurrency(rpgPlayer.getBalance())
                : "§e" + (rpgPlayer.getBalance() / 4096L) + " Vàng";

        inv.setItem(10, createItem(Material.RED_STAINED_GLASS_PANE, "§c§lSỨC MẠNH (STR)",
                "§7Tăng Sát thương vật lý & Thể lực tối đa & khả năng đỡ đòn.",
                "§fĐã cộng: §c" + rpgPlayer.getAttributeData().getAttribute("STR"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        inv.setItem(11, createItem(Material.PURPLE_STAINED_GLASS_PANE, "§5§lTRÍ TUỆ (INT)",
                "§7Tăng Sát thương phép & Mana tối đa & Giảm hồi chiêu.",
                "§fĐã cộng: §5" + rpgPlayer.getAttributeData().getAttribute("INT"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        inv.setItem(12, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§lTHỂ LỰC (VIT)",
                "§7Tăng Máu tối đa & Tỉ lệ hồi phục.",
                "§fĐã cộng: §a" + rpgPlayer.getAttributeData().getAttribute("VIT"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        inv.setItem(13, createItem(Material.BLUE_STAINED_GLASS_PANE, "§9§lNHANH NHẸN (AGI)",
                "§7Tăng tốc độ chạy & Tỉ lệ né tránh.",
                "§fĐã cộng: §9" + rpgPlayer.getAttributeData().getAttribute("AGI"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        inv.setItem(14, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lMAY MẮN (LUK)",
                "§7Tăng tỉ lệ rơi đồ & Tỉ lệ chí mạng.",
                "§fĐã cộng: §e" + rpgPlayer.getAttributeData().getAttribute("LUK"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        // Ô 15: Tái lập 100% AP bằng 10 Vàng (40,960 Đồng) hoặc Cuộn Giấy Tái Lập Chỉ Số
        inv.setItem(15, createItem(Material.ENCHANTED_BOOK, "§b§lTÁI LẬP CHỈ SỐ (100%)",
                "§7Đặt lại toàn bộ 5 chỉ số cơ bản về 0.",
                "§aHoàn trả 100% số điểm đã cộng (không hao hụt).",
                "§7Chi phí: §e10 Vàng §7(hoặc §b1x Cuộn Giấy Tái Lập Chỉ Số§7)",
                "§7Ngân hàng hiện có: " + balanceText,
                "", "§b▶ Nhấp để tái lập toàn bộ chỉ số!"));

        // Ô 16: Reset thường (80% AP)
        inv.setItem(16, createItem(Material.BARRIER, "§c§lRESET CHỈ SỐ THƯỜNG",
                "§7Đặt lại toàn bộ điểm chỉ số.",
                "§cChỉ hoàn trả 80% số điểm đã cộng.",
                "", "§eNhấp để tiến hành!"));

        player.openInventory(inv);
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        int slot = event.getSlot();
        boolean upgraded = false;

        if (slot == 10) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("STR");
        else if (slot == 11) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("INT");
        else if (slot == 12) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("VIT");
        else if (slot == 13) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("AGI");
        else if (slot == 14) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("LUK");
        else if (slot == 15) {
            handleFullAttributeReset(player, rpgPlayer);
            return;
        } else if (slot == 16) {
            rpgPlayer.getAttributeData().resetAttributes();
            openGUI(player);
            return;
        }

        if (upgraded) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            openGUI(player);
        } else if (slot >= 10 && slot <= 14) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cBạn không có đủ điểm tiềm năng!");
        }
    }

    private void handleFullAttributeReset(Player player, RPGPlayer rpgPlayer) {
        String[] stats = {"STR", "INT", "VIT", "AGI", "LUK"};
        int totalInvested = 0;
        for (String stat : stats) {
            totalInvested += rpgPlayer.getAttributeData().getAttribute(stat);
        }

        if (totalInvested <= 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§eBạn chưa cộng điểm nào vào 5 chỉ số cơ bản!");
            return;
        }

        RPGCore core = RPGCore.getInstance();
        ItemManager itemManager = (core != null) ? core.getItemManager() : null;
        CurrencyManager currencyManager = (core != null) ? core.getCurrencyManager() : null;
        ItemStack resetScroll = (itemManager != null) ? itemManager.buildItem("attr_change", 1) : null;

        boolean usedScroll = (resetScroll != null && itemManager.takeItem(player, resetScroll, 1));
        if (!usedScroll) {
            boolean paid = (currencyManager != null)
                    ? currencyManager.processSmartPayment(player, rpgPlayer, FULL_RESET_COST_COPPER)
                    : rpgPlayer.withdraw(FULL_RESET_COST_COPPER);

            if (!paid) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cBạn cần §e10 Vàng §c(hoặc §b1x Cuộn Giấy Tái Lập Chỉ Số§c) để tái lập 100% điểm tiềm năng!");
                return;
            }
        }

        for (String stat : stats) {
            rpgPlayer.getAttributeData().setAttribute(stat, 0);
        }
        int currentUnspent = rpgPlayer.getAttributeData().getUnspentAttrPoints();
        rpgPlayer.getAttributeData().setUnspentAttrPoints(currentUnspent + totalInvested);

        rpgPlayer.updateMaxStats();
        rpgPlayer.updateEquipmentStats();
        if (core != null && core.getBossBarManager() != null) {
            core.getBossBarManager().updateBars(player, rpgPlayer);
        }
        playerManager.savePlayer(player.getUniqueId(), true, false);

        String costText = usedScroll ? "§b1x Cuộn Giấy Tái Lập Chỉ Số" : "§e10 Vàng";
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage("§a§l[THÀNH CÔNG] §fĐã tiêu hao " + costText + " §fđể tái lập và hoàn trả nguyên vẹn §e" + totalInvested + " §fđiểm tiềm năng!");
        openGUI(player);
    }
}