package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.managers.BaitManager;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaitCrafterGUI implements Listener {

    public static final String GUI_NAME = "§8Thùng Ủ Mồi Câu";

    // Các khu vực tương tác
    private final List<Integer> BASE_SLOTS = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30);
    private final List<Integer> BUFF_SLOTS = Arrays.asList(14, 15, 16);
    private final List<Integer> ULT_SLOTS = Arrays.asList(23, 24, 25);
    private final int PROCESS_SLOT = 41;
    private final int OUTPUT_SLOT = 43;

    private final ItemManager itemManager;
    private final BaitManager baitManager;

    public BaitCrafterGUI(ItemManager itemManager, BaitManager baitManager) {
        this.itemManager = itemManager;
        this.baitManager = baitManager;
    }

    @EventHandler
    public void onInteractComposter(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.COMPOSTER) {
                if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR) {
                    event.setCancelled(true);
                    openGUI(event.getPlayer());
                }
            }
        }
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_NAME);

        // Tạo các loại kính để làm Hướng dẫn (Hint)
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack hintBase = createGuiItem(Material.WHITE_STAINED_GLASS_PANE, "§f§l[ Đặt Mồi Nền Ở Đây ]");
        ItemStack hintBuff = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§l[ Đặt Mồi Buff Ở Đây ]");
        ItemStack hintUlt = createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c§l[ Đặt Buff Tối Thượng Ở Đây ]");

        // Vẽ GUI
        for (int i = 0; i < 54; i++) {
            if (BASE_SLOTS.contains(i)) inv.setItem(i, hintBase.clone());
            else if (BUFF_SLOTS.contains(i)) inv.setItem(i, hintBuff.clone());
            else if (ULT_SLOTS.contains(i)) inv.setItem(i, hintUlt.clone());
            else if (i != PROCESS_SLOT && i != OUTPUT_SLOT) inv.setItem(i, bg);
        }

        // Nút Process (Màu Xanh lá)
        ItemStack processBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta pMeta = processBtn.getItemMeta();
        pMeta.setDisplayName("§a§l[ NẤU MỒI CÂU ]");
        pMeta.setLore(Arrays.asList("§7Click để bắt đầu ủ mồi.", "§7Yêu cầu tối thiểu 3 Mồi Nền."));
        processBtn.setItemMeta(pMeta);
        inv.setItem(PROCESS_SLOT, processBtn);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_COMPOSTER_FILL, 1f, 1f);
    }

    private ItemStack createGuiItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    // Chặn kéo thả đồ (Drag) vào nền để tránh lỗi item ảo
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < 54) { // Nếu kéo vào khu vực của GUI
                if (!BASE_SLOTS.contains(slot) && !BUFF_SLOTS.contains(slot) && !ULT_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // 1. Chặn Shift-Click từ kho đồ người chơi vào GUI (gây lỗi item ảo)
        if (event.isShiftClick() && slot >= 54) {
            event.setCancelled(true);
            player.sendMessage("§cVui lòng kéo thả vật phẩm vào đúng ô thay vì dùng Shift-Click.");
            return;
        }

        // Click trong túi người chơi thì bỏ qua
        if (slot >= 54) return;

        // Nếu click vào một ô rỗng mà ô đó có kính màu (hint) -> Xóa kính đi trước khi đặt đồ vào
        ItemStack clickedItem = event.getCurrentItem();
        boolean isHintGlass = clickedItem != null && clickedItem.getType().toString().contains("STAINED_GLASS_PANE");

        if (BASE_SLOTS.contains(slot) || BUFF_SLOTS.contains(slot) || ULT_SLOTS.contains(slot)) {
            // Cho phép thao tác, nhưng nếu là kính hint thì đừng cản
            if (isHintGlass && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCurrentItem(null); // Xóa kính
                // Spigot sẽ tự động đặt event.getCursor() vào ô trống này
            }
            return; // Đã nằm trong vùng hợp lệ
        }


        // 2. Xử lý Output
        if (slot == OUTPUT_SLOT) {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCancelled(true); // Không cho đặt đồ vào output
            }
            return;
        }

        // 3. Xử lý nút Nấu (PROCESS_SLOT)
        if (slot == PROCESS_SLOT) {
            event.setCancelled(true);

            if (event.getInventory().getItem(OUTPUT_SLOT) != null && event.getInventory().getItem(OUTPUT_SLOT).getType() != Material.AIR) {
                player.sendMessage("§cHãy lấy thành phẩm ra trước khi ủ mẻ mới!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            List<String> baseIds = new ArrayList<>();
            List<String> buffIds = new ArrayList<>();

            for (int s : BASE_SLOTS) {
                ItemStack item = event.getInventory().getItem(s);
                // Bỏ qua nếu là kính trắng
                if (item != null && item.getType() != Material.AIR && item.getType() != Material.WHITE_STAINED_GLASS_PANE) {
                    String id = itemManager.getItemId(item);
                    if (id != null && id.startsWith("bait_base_")) baseIds.add(id);
                }
            }

            List<Integer> combinedBuffSlots = new ArrayList<>(BUFF_SLOTS);
            combinedBuffSlots.addAll(ULT_SLOTS);

            for (int s : combinedBuffSlots) {
                ItemStack item = event.getInventory().getItem(s);
                // Bỏ qua nếu là kính màu
                if (item != null && item.getType() != Material.AIR && !item.getType().toString().contains("GLASS_PANE")) {
                    String id = itemManager.getItemId(item);
                    if (id != null && id.startsWith("bait_buff_")) {
                        buffIds.add(id);
                    }
                }
            }

            ItemStack result = baitManager.craftCustomBait(baseIds, buffIds);

            if (result == null) {
                player.sendMessage("§cỦ mồi thất bại! Cần tối thiểu 3 Mồi Nền.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Dọn sạch nguyên liệu đã nấu
            for (int s : BASE_SLOTS) event.getInventory().setItem(s, null);
            for (int s : combinedBuffSlots) event.getInventory().setItem(s, null);

            // Xuất thành phẩm
            event.getInventory().setItem(OUTPUT_SLOT, result);
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
            player.sendMessage("§a§l[!] §aThùng ủ đã cho ra Mồi Câu Đặc Chế!");
            return;
        }

        // Khóa các ô còn lại (nền xám)
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) {
            Inventory inv = event.getInventory();
            Player player = (Player) event.getPlayer();

            List<Integer> allReturnSlots = new ArrayList<>(BASE_SLOTS);
            allReturnSlots.addAll(BUFF_SLOTS);
            allReturnSlots.addAll(ULT_SLOTS);
            allReturnSlots.add(OUTPUT_SLOT);

            for (int slot : allReturnSlots) {
                ItemStack item = inv.getItem(slot);
                // Trả lại nếu nó không phải là đồ trống và không phải kính màu
                if (item != null && item.getType() != Material.AIR && !item.getType().toString().contains("GLASS_PANE")) {
                    player.getInventory().addItem(item);
                }
            }
        }
    }
}