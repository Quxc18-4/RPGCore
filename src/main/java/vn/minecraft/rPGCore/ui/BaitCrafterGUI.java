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
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
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
    private final int HINT_SLOT = 49; // Ô hiển thị hướng dẫn

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

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        // KIỂM TRA ĐIỀU KIỆN NGƯ DÂN CẤP 50
        boolean hasUltAccess = false;
        PlayerJobData jobData = JobManager.getPlayerData(player);
        if (jobData != null && "fisherman".equalsIgnoreCase(jobData.getJobId()) && jobData.getLevel("fisherman") >= 50) {
            hasUltAccess = true;
        }

        ItemStack barrier = createGuiItem(Material.BARRIER, "§cÔ bị khóa");
        ItemMeta bMeta = barrier.getItemMeta();
        bMeta.setLore(Arrays.asList("§7Yêu cầu:", "§7- Nghề: Ngư Dân", "§7- Cấp độ: 50+"));
        barrier.setItemMeta(bMeta);

        // Nút Hướng Dẫn (Hint)
        ItemStack hintBtn = createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lThông tin Hướng dẫn");
        ItemMeta hMeta = hintBtn.getItemMeta();
        hMeta.setLore(Arrays.asList(
                "§7Khu vực 3x3 bên trái:",
                "§7- Đặt tối thiểu 3 Mồi Nền để ủ.",
                "§7- Tối đa 9 Mồi Nền để nhận nhiều thành phẩm.",
                "",
                "§7Khu vực 6 ô bên phải:",
                "§7- Cho phép đặt BẤT KỲ loại Mồi Buff/Tối thượng nào.",
                "§7- Mặc định có 3 ô mở sẵn để sử dụng.",
                "§7- Đạt Ngư Dân Cấp 50 sẽ mở khóa thêm 3 ô bên dưới",
                "§7  (cho phép nhồi tới 6 Buff vào một mẻ mồi!)."
        ));
        hintBtn.setItemMeta(hMeta);

        // Nút Nấu (Process)
        ItemStack processBtn = createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§l[ NẤU MỒI CÂU ]");
        ItemMeta pMeta = processBtn.getItemMeta();
        pMeta.setLore(Arrays.asList("§7Click vào đây để bắt đầu ủ mồi."));
        processBtn.setItemMeta(pMeta);

        // Vẽ GUI
        for (int i = 0; i < 54; i++) {
            if (BASE_SLOTS.contains(i) || BUFF_SLOTS.contains(i) || i == OUTPUT_SLOT) {
                // Để trống hoàn toàn
                continue;
            } else if (ULT_SLOTS.contains(i)) {
                if (!hasUltAccess) inv.setItem(i, barrier);
            } else if (i == PROCESS_SLOT) {
                inv.setItem(i, processBtn);
            } else if (i == HINT_SLOT) {
                inv.setItem(i, hintBtn);
            } else {
                inv.setItem(i, bg);
            }
        }

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

    // Chặn kéo đồ vào nền xám
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                if (!BASE_SLOTS.contains(slot) && !BUFF_SLOTS.contains(slot) && !ULT_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }
                // Nếu kéo vào ô Barrier
                if (ULT_SLOTS.contains(slot)) {
                    ItemStack targetItem = event.getInventory().getItem(slot);
                    if (targetItem != null && targetItem.getType() == Material.BARRIER) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // 1. Chặn click vào nền xám
        if (slot < 54 && !BASE_SLOTS.contains(slot) && !BUFF_SLOTS.contains(slot) && !ULT_SLOTS.contains(slot)
                && slot != PROCESS_SLOT && slot != OUTPUT_SLOT && slot != HINT_SLOT) {
            event.setCancelled(true);
            return;
        }

        // 2. Chặn shift-click để tránh lỗi sync
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        // 3. Xử lý nút Nấu (PROCESS_SLOT)
        if (slot == PROCESS_SLOT) {
            event.setCancelled(true);
            processCrafting(event.getInventory(), player);
            // Ép Client làm mới lại giao diện ngay lập tức
            Bukkit.getScheduler().runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), player::updateInventory, 1L);
            return;
        }

        // 4. Chặn Output Slot không cho đặt đồ vào
        if (slot == OUTPUT_SLOT && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            event.setCancelled(true);
        }
    }

    private void processCrafting(Inventory inv, Player player) {
        List<String> baseIds = new ArrayList<>();
        List<String> buffIds = new ArrayList<>();

        // Kiểm tra nguyên liệu nền
        for (int s : BASE_SLOTS) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR) {
                String id = itemManager.getItemId(item);
                if (id != null && id.startsWith("bait_base_")) baseIds.add(id);
            }
        }

        // Kiểm tra nguyên liệu buff
        List<Integer> combinedBuffSlots = new ArrayList<>(BUFF_SLOTS);
        combinedBuffSlots.addAll(ULT_SLOTS);
        for (int s : combinedBuffSlots) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.BARRIER) {
                String id = itemManager.getItemId(item);
                if (id != null && id.startsWith("bait_buff_")) buffIds.add(id);
            }
        }

        // VALIDATE: Kiểm tra công thức trước khi trừ
        ItemStack result = baitManager.craftCustomBait(baseIds, buffIds);
        if (result == null) {
            player.sendMessage("§cCông thức không hợp lệ! Cần tối thiểu 3 Mồi Nền.");
            return;
        }

        // =====================================
        // [FIX CHÍNH]: BUFF NGHỀ NGƯ DÂN LÊN MỒI
        // =====================================
        PlayerJobData jobData = JobManager.getPlayerData(player);
        if (jobData != null && "FISHERMAN".equals(jobData.getJobId())) {
            int level = jobData.getLevel("FISHERMAN");
            double ultMult = jobData.hasUnlockedSkill("FISHERMAN", "FISH_ULT_1") ? 1.2 : 1.0;

            // 1. Tăng sản lượng
            int extraYield = level >= 50 ? 2 : (level >= 25 ? 1 : 0);
            result.setAmount(result.getAmount() + extraYield);

            // 2. Buff % chỉ số bằng cách nhân Lore NBT (Code này khá nâng cao, có thể bỏ qua nếu khó,
            // nhưng giải pháp đơn giản nhất là set 1 cờ buff_multiplier vào NBT của Item để lúc FishingManager đọc nó tự nhân).
            ItemMeta meta = result.getItemMeta();
            double multi = 1.0 + ((level * 0.6 * ultMult) / 100.0);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "bait_fisher_multi"), org.bukkit.persistence.PersistentDataType.DOUBLE, multi);

            List<String> lore = meta.getLore();
            lore.add(0, "§b✦ Pha chế bởi Ngư Dân Bậc Thầy (+" + String.format("%.0f", (multi-1)*100) + "%)");
            meta.setLore(lore);
            result.setItemMeta(meta);

            // 3. Rớt Nước Mắt Đại Dương (Công thức từ bạn)
            int bases = baseIds.size();
            int buffs = buffIds.size();
            double tearChance = 8.0 - ((9 - bases) * (4.0 / 3.0)) + (buffs * 3.0);

            if (Math.random() * 100.0 <= tearChance) {
                player.getInventory().addItem(itemManager.buildItem("misc_ocean_tear", 1));
                player.sendMessage("§b[Rơi Đồ] Bạn nhận được 1 Nước Mắt Đại Dương từ quá trình ủ mồi!");
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.COLLECT_OCEAN_TEAR);
            }
        }

        // Báo cáo Job Quest (Craft có 9 Base)
        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.CRAFT_BAIT, baseIds.size());

        // THỰC HIỆN TRỪ NGUYÊN LIỆU (Đảm bảo ID khớp mới trừ)
        for (int s : BASE_SLOTS) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR) {
                item.setAmount(item.getAmount() - 1); // Trừ 1
            }
        }
        for (int s : combinedBuffSlots) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.BARRIER) {
                item.setAmount(item.getAmount() - 1); // Trừ 1
            }
        }

        inv.setItem(OUTPUT_SLOT, result);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
        player.updateInventory(); // FIX GHOST ITEM
    }

    // Nhả lại nguyên liệu thừa khi đóng GUI
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
                if (item != null && item.getType() != Material.AIR && item.getType() != Material.BARRIER) {
                    player.getInventory().addItem(item);
                }
            }
        }
    }
}