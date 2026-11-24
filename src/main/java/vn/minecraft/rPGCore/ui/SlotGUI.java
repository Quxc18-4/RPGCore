package vn.minecraft.rPGCore.ui;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SlotGUI implements InventoryHolder, Listener {
    private final RPGCore plugin;
    private final Inventory inventory;

    // Vị trí trung tâm của 3 Slot trong GUI 27 ô: Slot 1 (ô 11), Slot 2 (ô 13), Slot 3 (ô 15)
    private static final int[] SLOT_POSITIONS = {11, 13, 15};

    // Constructor dùng để đăng ký Listener trong RPGCore.onEnable()
    public SlotGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.inventory = null;
    }

    private SlotGUI(RPGCore plugin, int activeSlot, int maxSlots, Document slotsDoc) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, "§8§lQUẢN LÝ SLOT NHÂN VẬT");
        renderGUI(activeSlot, maxSlots, slotsDoc);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Lưu đồng bộ dữ liệu hiện tại rồi mở GUI bất đồng bộ để hiển thị thông số chính xác của cả 3 slot
     */
    public static void open(Player player, RPGCore plugin) {
        PlayerManager playerManager = plugin.getPlayerManager();
        UUID uuid = player.getUniqueId();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
        if (rpgPlayer == null) return;

        // Lưu đồng bộ slot đang chơi để số liệu hiển thị trong GUI luôn mới nhất
        playerManager.savePlayer(uuid, false, false);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int maxSlots = playerManager.getMaxSlots(uuid);
            Document playerDoc = playerManager.getPlayerCollection().find(Filters.eq("_id", uuid.toString())).first();
            Document slotsDoc = (playerDoc != null) ? playerDoc.get("slots", Document.class) : new Document();
            int activeSlot = rpgPlayer.getActiveSlot();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                SlotGUI gui = new SlotGUI(plugin, activeSlot, maxSlots, slotsDoc);
                player.openInventory(gui.getInventory());
            });
        });
    }

    private void renderGUI(int activeSlot, int maxSlots, Document slotsDoc) {
        ItemStack background = createPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, background);
        }

        for (int slotNum = 1; slotNum <= 3; slotNum++) {
            int centerIndex = SLOT_POSITIONS[slotNum - 1];
            Document slotData = (slotsDoc != null) ? slotsDoc.get(String.valueOf(slotNum), Document.class) : null;

            if (slotNum == activeSlot) {
                // Slot đang chơi: Viền xanh lá bao quanh
                drawActiveGreenBorder(centerIndex);
                inventory.setItem(centerIndex, buildActiveSlotItem(slotNum, slotData));
            } else if (slotNum > maxSlots) {
                // Slot 3 bị khóa (max_slots < 3)
                inventory.setItem(centerIndex, buildLockedSlotItem(slotNum));
            } else {
                // Slot đã mở khóa nhưng chưa chọn
                inventory.setItem(centerIndex, buildUnlockedSlotItem(slotNum, slotData));
            }
        }
    }

    private void drawActiveGreenBorder(int center) {
        ItemStack greenPane = createPane(Material.LIME_STAINED_GLASS_PANE, "§a§lĐang Sử Dụng");
        int[] offsets = {-10, -9, -8, -1, 1, 8, 9, 10};
        for (int offset : offsets) {
            int idx = center + offset;
            if (idx >= 0 && idx < 27) {
                inventory.setItem(idx, greenPane);
            }
        }
    }

    private ItemStack buildActiveSlotItem(int slotNum, Document data) {
        String classId = (data != null && data.getString("class") != null) ? data.getString("class") : "commoner";
        int level = (data != null && data.get("level") instanceof Number) ? ((Number) data.get("level")).intValue() : 1;
        long balance = (data != null && data.get("balance") instanceof Number) ? ((Number) data.get("balance")).longValue() : 0L;

        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lSLOT " + slotNum + " §7(§aĐang chơi§7)");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7Hệ phái: " + resolveClassDisplayName(classId));
            lore.add("§7Cấp độ: §bLv." + level);
            lore.add("§7Số dư: " + formatCurrency(balance));
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§a✔ Nhân vật hiện tại của bạn");
            meta.setLore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildUnlockedSlotItem(int slotNum, Document data) {
        boolean isEmpty = (data == null || data.isEmpty());
        ItemStack item = new ItemStack(isEmpty ? Material.ARMOR_STAND : Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            if (isEmpty) {
                meta.setDisplayName("§e§lSLOT " + slotNum + " §7(§fSlot Trống§7)");
                lore.add("§7Trạng thái: §fChưa có nhân vật");
                lore.add("§7Khởi đầu: §eCommoner §7(§bLv.1§7)");
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§a▶ Slot Trống - Nhấp để tạo nhân vật mới");
            } else {
                String classId = data.getString("class") != null ? data.getString("class") : "commoner";
                int level = (data.get("level") instanceof Number) ? ((Number) data.get("level")).intValue() : 1;
                long balance = (data.get("balance") instanceof Number) ? ((Number) data.get("balance")).longValue() : 0L;

                meta.setDisplayName("§e§lSLOT " + slotNum + " §7(§fSẵn sàng§7)");
                lore.add("§7Hệ phái: " + resolveClassDisplayName(classId));
                lore.add("§7Cấp độ: §bLv." + level);
                lore.add("§7Số dư: " + formatCurrency(balance));
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§e▶ Nhấp để chuyển sang nhân vật này");
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildLockedSlotItem(int slotNum) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lSLOT " + slotNum + " §7(§cĐã Khóa§7)");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§c✖ Slot nhân vật này chưa được mở khóa.");
            lore.add("");
            lore.add("§eMua gói VIP (100 Vàng hoặc 50k trên Web)");
            lore.add("§eđể mở khóa vĩnh viễn Slot nhân vật thứ 3");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SlotGUI)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof SlotGUI)) return;

        int rawSlot = event.getRawSlot();
        int targetSlot = -1;
        for (int i = 0; i < SLOT_POSITIONS.length; i++) {
            if (SLOT_POSITIONS[i] == rawSlot) {
                targetSlot = i + 1;
                break;
            }
        }

        if (targetSlot == -1) return;

        player.closeInventory();
        plugin.getPlayerManager().switchSlot(player, targetSlot);
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String resolveClassDisplayName(String classId) {
        if (plugin.getClassManager() != null && classId != null) {
            RPGClass rpgClass = plugin.getClassManager().getClassById(classId);
            if (rpgClass != null) {
                return rpgClass.getDisplayName();
            }
        }
        return "§fCommoner";
    }

    private String formatCurrency(long totalCopper) {
        long gold = totalCopper / 10000L;
        long silver = (totalCopper % 10000L) / 100L;
        long copper = totalCopper % 100L;
        return "§6" + gold + " Vàng §7" + silver + " Bạc §c" + copper + " Đồng";
    }
}