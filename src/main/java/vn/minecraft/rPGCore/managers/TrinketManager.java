package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Arrays;
import java.util.List;

public class TrinketManager implements CommandExecutor, Listener {

    public static final String GUI_NAME = "§5§lTrang Sức Kim Hoàn";

    private final int SLOT_RING_1 = 10;
    private final int SLOT_NECKLACE = 12;
    private final int SLOT_BRACELET = 14;
    private final int SLOT_RING_2 = 16;

    public TrinketManager() {
        if (RPGCore.getInstance().getCommand("trinket") != null) {
            RPGCore.getInstance().getCommand("trinket").setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        openTrinketGUI(player);
        return true;
    }

    public void openTrinketGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);

        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        inv.setItem(SLOT_RING_1, getPlaceholder(SLOT_RING_1));
        inv.setItem(SLOT_NECKLACE, getPlaceholder(SLOT_NECKLACE));
        inv.setItem(SLOT_BRACELET, getPlaceholder(SLOT_BRACELET));
        inv.setItem(SLOT_RING_2, getPlaceholder(SLOT_RING_2));

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer != null) {
            ItemStack[] trinkets = rpgPlayer.getTrinkets();
            if (isValidTrinket(trinkets[0])) inv.setItem(SLOT_RING_1, trinkets[0]);
            if (isValidTrinket(trinkets[1])) inv.setItem(SLOT_NECKLACE, trinkets[1]);
            if (isValidTrinket(trinkets[2])) inv.setItem(SLOT_BRACELET, trinkets[2]);
            if (isValidTrinket(trinkets[3])) inv.setItem(SLOT_RING_2, trinkets[3]);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
    }

    private ItemStack getPlaceholder(int slot) {
        if (slot == SLOT_RING_1 || slot == SLOT_RING_2) return createEmptySlot(Material.YELLOW_STAINED_GLASS_PANE, "§e[ Ô lắp Nhẫn ]");
        if (slot == SLOT_NECKLACE) return createEmptySlot(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b[ Ô lắp Dây Chuyền ]");
        if (slot == SLOT_BRACELET) return createEmptySlot(Material.LIME_STAINED_GLASS_PANE, "§a[ Ô lắp Vòng Tay ]");
        return new ItemStack(Material.AIR);
    }

    private ItemStack createEmptySlot(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7Kéo thả Trang sức tương ứng vào đây."));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isValidTrinket(ItemStack item) {
        return item != null && item.getType() != Material.AIR && !item.getType().name().endsWith("GLASS_PANE");
    }

    private int getTrinketIndex(int slot) {
        if (slot == SLOT_RING_1) return 0;
        if (slot == SLOT_NECKLACE) return 1;
        if (slot == SLOT_BRACELET) return 2;
        if (slot == SLOT_RING_2) return 3;
        return -1;
    }

    // [MỚI]: Dập tắt đèn Set Bonus trên Item khi nhấc nó ra khỏi người
    private void deactivateSetVisual(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        boolean updated = false;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("[Set 3 mảnh]:") || line.contains("[Set 4 mảnh]:")) {
                if (line.startsWith("§a")) {
                    lore.set(i, line.replaceFirst("§a", "§8")); // Đổi xanh thành xám
                    updated = true;
                }
            }
        }
        if (updated) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    // [MỚI]: Tự động vẽ lại UI để Load hiệu ứng Set Bonus sau khi equip đồ
    private void redrawUI(Player player, Inventory inv) {
        Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> {
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) {
                ItemStack[] trinkets = rpgPlayer.getTrinkets();
                if (isValidTrinket(trinkets[0])) inv.setItem(SLOT_RING_1, trinkets[0]);
                if (isValidTrinket(trinkets[1])) inv.setItem(SLOT_NECKLACE, trinkets[1]);
                if (isValidTrinket(trinkets[2])) inv.setItem(SLOT_BRACELET, trinkets[2]);
                if (isValidTrinket(trinkets[3])) inv.setItem(SLOT_RING_2, trinkets[3]);
            }
        }, 2L); // Đợi 2 ticks cho RPGPlayer tính toán xong
    }

    @EventHandler
    public void onInteractGrindstone(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.GRINDSTONE) return;

        if (event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            new vn.minecraft.rPGCore.ui.TrinketForgeGUI(RPGCore.getInstance().getItemManager()).openGUI(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();

        Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), player::updateInventory, 1L);

        if (event.getClick().isKeyboardClick() || event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if (slot >= 27) return;

        if (slot != SLOT_RING_1 && slot != SLOT_NECKLACE && slot != SLOT_BRACELET && slot != SLOT_RING_2) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack cursor = event.getCursor();
        ItemStack currentSlotItem = inv.getItem(slot);
        int trinketIndex = getTrinketIndex(slot);

        // TH 1: NGƯỜI CHƠI LẤY TRANG SỨC RA
        if (cursor == null || cursor.getType() == Material.AIR) {
            if (isValidTrinket(currentSlotItem)) {
                ItemStack takenOut = currentSlotItem.clone();
                deactivateSetVisual(takenOut); // Tắt đèn set
                event.getView().setCursor(takenOut);

                inv.setItem(slot, getPlaceholder(slot));
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);

                updateStatsLive(player, trinketIndex, null);
                redrawUI(player, inv); // Vẽ lại UI để cập nhật đèn set các món còn lại
            }
            return;
        }

        // TH 2: NGƯỜI CHƠI BỎ TRANG SỨC VÀO
        ItemMeta meta = cursor.getItemMeta();
        NamespacedKey key = new NamespacedKey(RPGCore.getInstance(), "trinket_type");

        if (meta == null || !meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            player.sendMessage("§c§l[!] §cVật phẩm này không phải là Trang Sức!");
            return;
        }

        String trinketType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        boolean isCorrectSlot = (trinketType.equals("ring") && (slot == SLOT_RING_1 || slot == SLOT_RING_2)) ||
                (trinketType.equals("necklace") && slot == SLOT_NECKLACE) ||
                (trinketType.equals("bracelet") && slot == SLOT_BRACELET);

        if (!isCorrectSlot) {
            player.sendMessage("§c§l[!] §cBạn không thể lắp " + trinketType.toUpperCase() + " vào ô này!");
            return;
        }

        ItemStack toEquip = cursor.clone();
        toEquip.setAmount(1);

        if (isValidTrinket(currentSlotItem)) {
            ItemStack takenOut = currentSlotItem.clone();
            deactivateSetVisual(takenOut); // Tắt đèn set món bị đẩy ra
            event.getView().setCursor(takenOut);
        } else {
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor);
            } else {
                event.getView().setCursor(new ItemStack(Material.AIR));
            }
        }

        inv.setItem(slot, toEquip);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1.5f);

        updateStatsLive(player, trinketIndex, toEquip);
        redrawUI(player, inv); // Vẽ lại UI để Bật đèn Set
    }

    private void updateStatsLive(Player player, int index, ItemStack item) {
        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer != null) {
            rpgPlayer.setTrinket(index, item);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        Player player = (Player) event.getPlayer();
        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());

        if (rpgPlayer != null) {
            Inventory inv = event.getInventory();
            rpgPlayer.setTrinket(0, isValidTrinket(inv.getItem(SLOT_RING_1)) ? inv.getItem(SLOT_RING_1) : null);
            rpgPlayer.setTrinket(1, isValidTrinket(inv.getItem(SLOT_NECKLACE)) ? inv.getItem(SLOT_NECKLACE) : null);
            rpgPlayer.setTrinket(2, isValidTrinket(inv.getItem(SLOT_BRACELET)) ? inv.getItem(SLOT_BRACELET) : null);
            rpgPlayer.setTrinket(3, isValidTrinket(inv.getItem(SLOT_RING_2)) ? inv.getItem(SLOT_RING_2) : null);
        }
    }
}