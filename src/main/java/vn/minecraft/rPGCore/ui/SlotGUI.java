package vn.minecraft.rPGCore.ui;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    // 10 Slot nhân vật: Hàng 2 bắt đầu từ ô 10 (10..14), Hàng 3 từ ô 19 (19..23)
    private static final int[] SLOT_POSITIONS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};
    private static final int INFO_SLOT = 16;

    public SlotGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.inventory = null;
    }

    private SlotGUI(RPGCore plugin, int activeSlot, int maxSlots, long lastSlotSwitch, boolean isAdmin, Document slotsDoc) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 36, "§8§lQUẢN LÝ SLOT NHÂN VẬT");
        renderGUI(activeSlot, maxSlots, lastSlotSwitch, isAdmin, slotsDoc);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(Player player, RPGCore plugin) {
        PlayerManager playerManager = plugin.getPlayerManager();
        UUID uuid = player.getUniqueId();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
        if (rpgPlayer == null) return;

        // Lưu đồng bộ slot đang chơi để số liệu hiển thị luôn chính xác
        playerManager.savePlayer(uuid, false, false);

        boolean isAdmin = player.isOp() || player.hasPermission("rpgcore.admin");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int maxSlots = playerManager.getMaxSlots(uuid);
            Document playerDoc = playerManager.getPlayerCollection().find(Filters.eq("_id", uuid.toString())).first();
            Document slotsDoc = (playerDoc != null) ? playerDoc.get("slots", Document.class) : new Document();
            int activeSlot = rpgPlayer.getActiveSlot();
            long lastSwitch = rpgPlayer.getLastSlotSwitch();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                SlotGUI gui = new SlotGUI(plugin, activeSlot, maxSlots, lastSwitch, isAdmin, slotsDoc);
                player.openInventory(gui.getInventory());
            });
        });
    }

    private void renderGUI(int activeSlot, int maxSlots, long lastSlotSwitch, boolean isAdmin, Document slotsDoc) {
        ItemStack background = createPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, background);
        }

        // Đặt ô thông tin tổng quan ở vị trí 16
        inventory.setItem(INFO_SLOT, buildInfoItem(maxSlots, lastSlotSwitch, isAdmin));

        for (int slotNum = 1; slotNum <= PlayerManager.MAX_ALLOWED_SLOTS; slotNum++) {
            int guiIndex = SLOT_POSITIONS[slotNum - 1];
            Document slotData = (slotsDoc != null) ? slotsDoc.get(String.valueOf(slotNum), Document.class) : null;

            if (slotNum == activeSlot) {
                inventory.setItem(guiIndex, buildActiveSlotItem(slotNum, slotData));
            } else if (slotNum > maxSlots) {
                inventory.setItem(guiIndex, buildLockedSlotItem(slotNum, maxSlots));
            } else {
                inventory.setItem(guiIndex, buildUnlockedSlotItem(slotNum, slotData));
            }
        }
    }

    private ItemStack buildInfoItem(int maxSlots, long lastSlotSwitch, boolean isAdmin) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lTHÔNG TIN TÀI KHOẢN");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7Số Slot đã mở: §a" + maxSlots + "§7/§e" + PlayerManager.MAX_ALLOWED_SLOTS);

            long cooldownMs = 15L * 60L * 1000L;
            long elapsed = System.currentTimeMillis() - lastSlotSwitch;
            if (isAdmin || elapsed >= cooldownMs) {
                lore.add("§7Trạng thái đổi Slot: §aSẵn sàng");
            } else {
                long remainSec = (cooldownMs - elapsed) / 1000L;
                lore.add("§7Hồi chiêu đổi Slot: §c" + (remainSec / 60L) + "p " + (remainSec % 60L) + "s");
            }

            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7• Mặc định mở sẵn §f2 Slot §7đầu tiên.");
            lore.add("§7• Tối đa §e10 Slot §7cho mỗi tài khoản.");
            lore.add("§7• Mở thêm từ Web: mỗi Slot tiếp theo");
            lore.add("§7  tăng thêm §6+100.000đ §7(100k → 800k).");
            lore.add("§7• Thời gian chờ giữa 2 lần đổi: §e15 phút§7.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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

    private ItemStack buildLockedSlotItem(int slotNum, int currentMaxSlots) {
        int costVnd = plugin.getPlayerManager().getSlotUnlockCostVnd(slotNum);
        String formattedCost = String.format("%,d", costVnd) + "đ";

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lSLOT " + slotNum + " §7(§cĐã Khóa§7)");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§c✖ Slot nhân vật này chưa được mở khóa.");
            lore.add("§7Giá mở khóa: §6" + formattedCost);
            if (slotNum == 3) {
                lore.add("§7Ưu đãi: §eMiễn phí khi mua gói VIP");
            }
            if (slotNum > currentMaxSlots + 1) {
                lore.add("§c⚠ Cần mở khóa Slot " + (slotNum - 1) + " trước.");
            } else {
                lore.add("§e▶ Truy cập Web Portal để mở khóa vĩnh viễn!");
            }
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
            if (rpgClass != null && rpgClass.getDisplayName() != null) {
                return ChatColor.translateAlternateColorCodes('&', rpgClass.getDisplayName());
            }
        }
        return "§fCommoner";
    }

    private String formatCurrency(long totalCopper) {
        if (plugin.getCurrencyManager() != null) {
            return plugin.getCurrencyManager().formatCurrency(totalCopper);
        }
        if (totalCopper <= 0) return "§60 Đồng";
        long gold = totalCopper / 4096L;
        long iron = (totalCopper % 4096L) / 64L;
        long copper = totalCopper % 64L;

        StringBuilder sb = new StringBuilder();
        if (gold > 0) sb.append("§e").append(gold).append(" Vàng ");
        if (iron > 0) sb.append("§f").append(iron).append(" Bạc ");
        if (copper > 0) sb.append("§6").append(copper).append(" Đồng");
        return sb.toString().trim();
    }
}