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

        ItemStack draggedItem = event.getOldCursor(); // Vật phẩm đang được nhấn giữ để kéo
        if (draggedItem == null || draggedItem.getType() == Material.AIR) return;
        String id = itemManager.getItemId(draggedItem);

        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                // 1. Chặn kéo vật phẩm đè lên ô kính nền xám trang trí
                if (!BASE_SLOTS.contains(slot) && !BUFF_SLOTS.contains(slot) && !ULT_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }

                // 2. Chốt chặn: Kéo mồi lạ đè vào khu vực Mồi Nền
                if (BASE_SLOTS.contains(slot)) {
                    if (id == null || !id.startsWith("bait_base_")) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // 3. Chốt chặn: Kéo mồi lạ đè vào khu vực Phụ Gia Cơ Bản
                if (BUFF_SLOTS.contains(slot)) {
                    if (id == null || !id.startsWith("bait_buff_")) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // 4. Chốt chặn: Kéo đè vào ô Tối thượng
                if (ULT_SLOTS.contains(slot)) {
                    ItemStack targetItem = event.getInventory().getItem(slot);
                    // Chặn nếu ô đang bị khóa bằng Barrier
                    if (targetItem != null && targetItem.getType() == Material.BARRIER) {
                        event.setCancelled(true);
                        return;
                    }
                    // Chặn nếu vật phẩm kéo không phải mồi buff
                    if (id == null || !id.startsWith("bait_buff_")) {
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
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        Inventory inv = event.getInventory();

        // [CHỐT CHẶN PHÍM TẮT]: Chặn hoàn toàn phím số 1-9 (Hotbar Swap)
        // Việc này ngăn người chơi lách luật bypass bộ lọc vật phẩm trên chuột
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }

        // 1. CLICK VÀO CÁC Ô BÊN TRONG CỦA THỦNG Ủ MỒI (NỬA TRÊN)
        if (slot < inv.getSize()) {

            // [NÚT Ủ MỒI CÂU]: Ô Kính màu xanh lá (PROCESS_SLOT = 41)
            if (slot == PROCESS_SLOT) {
                event.setCancelled(true); // Khóa không cho lấy kính xanh ra túi đồ
                processCrafting(inv, player); // Chỉ kích hoạt nấu khi bấm nút này!
                return;
            }

            // [Ô KẾT QUẢ ĐẦU RA]: (OUTPUT_SLOT = 43)
            if (slot == OUTPUT_SLOT) {
                // Cho phép lấy mồi đã nấu ra, cấm đặt ngược vật phẩm lạ từ ngoài vào
                if (cursor != null && cursor.getType() != Material.AIR) {
                    event.setCancelled(true);
                }
                return;
            }

            // [CHỐT CHẶN Ô TỐI THƯỢNG]: Nếu bấm vào ô đặc biệt (ULT_SLOTS)
            if (ULT_SLOTS.contains(slot)) {
                vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                if (jobData == null || !"FISHERMAN".equals(jobData.getJobId()) || !jobData.hasUnlockedSkill("FISHERMAN", "FISH_ULT_2")) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[!] §cBạn cần học kỹ năng CÔNG THỨC CỔ ĐẠI để dùng ô ủ mồi này!");
                    return;
                }

                if (cursor != null && cursor.getType() != Material.AIR) {
                    String id = itemManager.getItemId(cursor);
                    if (id == null || !id.startsWith("bait_buff_")) {
                        event.setCancelled(true);
                        player.sendMessage("§cChỉ có thể đặt Phụ Gia vào ô này!");
                        return;
                    }
                }
                event.setCancelled(false); // Đủ điều kiện -> Cho thả mồi vào
                return;
            }

            // [MỚI - CHỐT CHẶN Ô MỒI BUFF CƠ BẢN]: Khóa cứng nếu cố tình đặt mồi nền vào ô phụ gia
            if (BUFF_SLOTS.contains(slot)) {
                if (cursor != null && cursor.getType() != Material.AIR) {
                    String id = itemManager.getItemId(cursor);
                    if (id == null || !id.startsWith("bait_buff_")) {
                        event.setCancelled(true);
                        player.sendMessage("§cChỉ có thể đặt Phụ Gia vào ô này!");
                        return;
                    }
                }
                event.setCancelled(false);
                return;
            }

            // [MỚI - CHỐT CHẶN Ô MỒI NỀN CƠ BẢN]: Khóa cứng nếu cố tình đặt mồi buff vào ô nền
            if (BASE_SLOTS.contains(slot)) {
                if (cursor != null && cursor.getType() != Material.AIR) {
                    String id = itemManager.getItemId(cursor);
                    if (id == null || !id.startsWith("bait_base_")) {
                        event.setCancelled(true);
                        player.sendMessage("§cChỉ có thể đặt Mồi Nền vào ô này!");
                        return;
                    }
                }
                event.setCancelled(false);
                return;
            }

            // Các ô trang trí nền xám, thông tin hướng dẫn -> Khóa cứng không cho lấy ra
            event.setCancelled(true);
        }

        // 2. CLICK VÀO KHU VỰC TÚI ĐỒ CỦA NGƯỜI CHƠI (NỬA DƯỚI)
        else {
            // [CHỐT CHẶN SHIFT-CLICK]: Khóa hoàn toàn Shift-click từ túi dưới đi lên
            // Điều này ép người chơi phải kéo thả thủ công, ngăn Minecraft tự động đẩy mồi nền vào ô phụ gia đại bừa
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
                player.sendMessage("§e[Thùng Ủ] Vui lòng nhấc vật phẩm và kéo thả thủ công vào đúng vị trí ô mong muốn!");
                return;
            }
            event.setCancelled(false); // Cho phép sắp xếp túi đồ của mình bình thường
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
        // [BUFF NGHỀ NGƯ DÂN LÊN MỒI CÂU]
        // =====================================
        PlayerJobData jobData = JobManager.getPlayerData(player);
        if (jobData != null && "FISHERMAN".equals(jobData.getJobId())) {
            int level = jobData.getLevel("FISHERMAN");
            double ultMult = jobData.hasUnlockedSkill("FISHERMAN", "FISH_ULT_1") ? 1.2 : 1.0;

            // 1. Tăng sản lượng thành phẩm
            int extraYield = level >= 50 ? 2 : (level >= 25 ? 1 : 0);
            result.setAmount(result.getAmount() + extraYield);

            // 2. Buff % chỉ số và xử lý hiển thị Lore động chuẩn RPG: Gốc (+Thưởng Nghề)
            ItemMeta meta = result.getItemMeta();
            double multi = 1.0 + ((level * 0.6 * ultMult) / 100.0);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(vn.minecraft.rPGCore.RPGCore.getInstance(), "bait_fisher_multi"), org.bukkit.persistence.PersistentDataType.DOUBLE, multi);

            List<String> lore = meta.getLore();
            if (lore != null) {
                double bonusMultiplier = multi - 1.0; // Khoảng chênh lệch tăng thêm (VD: 0.30 tức +30%)

                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    // Chỉ can thiệp vào các dòng hiển thị chỉ số có ký tự định dạng ➥
                    if (line.contains("➥")) {
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([+-]?\\d+\\.?\\d*)").matcher(line);
                        if (matcher.find()) {
                            String numStr = matcher.group(1);
                            try {
                                double baseVal = Double.parseDouble(numStr);
                                double bonusVal = baseVal * bonusMultiplier;
                                String signStr = bonusVal >= 0 ? "+" : ""; // Tự động định dạng dấu tránh hiện +-

                                // Trường hợp thuộc tính dạng Phần Trăm (%)
                                if (line.contains("%")) {
                                    String bonusText = " §a(" + signStr + String.format("%.1f", bonusVal) + "%)";
                                    int pctIdx = line.indexOf("%");
                                    if (pctIdx != -1) {
                                        // Chèn cụm bonus màu xanh ngay kế bên ký tự % gốc
                                        String newLine = line.substring(0, pctIdx + 1) + bonusText + line.substring(pctIdx + 1);
                                        lore.set(i, newLine);
                                    }
                                }
                                // Trường hợp thuộc tính dạng Mili-giây (ms) của Dung Sai Nhịp Điệu
                                else if (line.contains("ms")) {
                                    String bonusText = " §a(" + signStr + String.format("%.0f", bonusVal) + "ms)";
                                    int msIdx = line.indexOf("ms");
                                    if (msIdx != -1) {
                                        // Chèn cụm bonus màu xanh ngay kế bên ký tự ms gốc
                                        String newLine = line.substring(0, msIdx + 2) + bonusText + line.substring(msIdx + 2);
                                        lore.set(i, newLine);
                                    }
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                meta.setLore(lore);
            }
            result.setItemMeta(meta);

            // 3. Rớt Nước Mắt Đại Dương theo tỉ lệ
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
                item.setAmount(item.getAmount() - 1);
            }
        }
        for (int s : combinedBuffSlots) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.BARRIER) {
                item.setAmount(item.getAmount() - 1);
            }
        }

        // THU HỒI MỒI CŨ TRONG Ô ĐẦU RA (OUTPUT) CHỐNG BỊ GHI ĐÈ KHI BẤM LẦN NỮA
        ItemStack existingBait = inv.getItem(OUTPUT_SLOT);
        if (existingBait != null && existingBait.getType() != Material.AIR) {
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(existingBait);
            if (!leftover.isEmpty()) {
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }
            player.sendMessage("§e[Thùng Ủ] Mồi câu cũ ở ô đầu ra đã được tự động cất vào túi đồ.");
        }

        inv.setItem(OUTPUT_SLOT, result);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
        player.updateInventory();
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