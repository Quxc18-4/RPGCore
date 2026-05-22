package vn.minecraft.rPGCore.mounts.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import vn.minecraft.rPGCore.mounts.data.MountData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MountStorageGUI implements Listener {

    public static final String STORAGE_TITLE = "§8Túi Đồ Thú Cưỡi";

    // Lưu tạm data thú cưỡi đang được mở rương (Để khi đóng rương sẽ biết lưu cho con nào)
    private final Map<UUID, MountData> openStorages = new HashMap<>();

    public void openStorage(Player player, MountData data) {
        int unlockedSlots = data.getTierStorage() * 3; // Ví dụ Bậc 1 có 3 ô

        // Làm tròn lên bội số của 9 (Ví dụ 3 ô -> Tạo rương 9 ô)
        int invSize = (int) Math.ceil(unlockedSlots / 9.0) * 9;
        if (invSize == 0) invSize = 9;

        Inventory inv = Bukkit.createInventory(null, invSize, STORAGE_TITLE);

        // 1. Giải mã Base64 -> Đổ item vào Rương
        if (data.getInventoryBase64() != null && !data.getInventoryBase64().isEmpty()) {
            ItemStack[] items = decodeItems(data.getInventoryBase64());
            for (int i = 0; i < items.length && i < unlockedSlots; i++) {
                if (items[i] != null) {
                    inv.setItem(i, items[i]);
                }
            }
        }

        // 2. Chèn Kính đỏ vào các ô bị khóa
        ItemStack locked = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = locked.getItemMeta();
        meta.setDisplayName("§c§lÔ chưa mở khóa");
        locked.setItemMeta(meta);

        for (int i = unlockedSlots; i < invSize; i++) {
            inv.setItem(i, locked);
        }

        openStorages.put(player.getUniqueId(), data);
        player.openInventory(inv);
    }

    // ==========================================
    // CHẶN NGƯỜI CHƠI LẤY KÍNH ĐỎ RA NGOÀI
    // ==========================================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(STORAGE_TITLE)) {
            Player player = (Player) event.getWhoClicked();
            MountData data = openStorages.get(player.getUniqueId());
            if (data == null) return;

            int unlockedSlots = data.getTierStorage() * 3;

            // Nếu họ click trực tiếp vào ô khóa
            if (event.getRawSlot() >= unlockedSlots && event.getRawSlot() < event.getInventory().getSize()) {
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // LƯU KHO ĐỒ KHI ĐÓNG RƯƠNG
    // ==========================================
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(STORAGE_TITLE)) {
            Player player = (Player) event.getPlayer();
            MountData data = openStorages.remove(player.getUniqueId());

            if (data != null) {
                int unlockedSlots = data.getTierStorage() * 3;
                ItemStack[] itemsToSave = new ItemStack[unlockedSlots];

                for (int i = 0; i < unlockedSlots; i++) {
                    itemsToSave[i] = event.getInventory().getItem(i);
                }

                // Mã hóa mảng Item thành chuỗi Base64 và lưu vào RAM
                data.setInventoryBase64(encodeItems(itemsToSave));
                player.sendMessage("§a[Hệ thống] Đã lưu kho đồ thú cưỡi!");
            }
        }
    }

    // ==========================================
    // HỆ THỐNG MÃ HÓA BUKKIT -> BASE64
    // ==========================================
    public String encodeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public ItemStack[] decodeItems(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack[0];
        }
    }
}