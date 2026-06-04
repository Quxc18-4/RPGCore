package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import vn.minecraft.rPGCore.managers.TrinketGenerator;

import java.util.*;

public class TrinketRandomCraftGUI implements Listener {

    public static final String GUI_NAME = "§5§lGhép Trang Sức Ngẫu Nhiên";
    private final NamespacedKey ITEM_KEY = new NamespacedKey(RPGCore.getInstance(), "rpg_item_id");
    private final NamespacedKey TRINKET_KEY = new NamespacedKey(RPGCore.getInstance(), "trinket_type");

    private final int[] FOOD_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34};
    private final String[] TIER_NAMES = {"Common", "Uncommon", "Rare", "Epic", "Mythical", "Heroic", "Legendary", "Divine"};
    private final String[] TIER_COLORS = {"§7", "§a", "§b", "§5", "§d", "§6", "§c", "§e"};

    private final double[][] TIER_RATES = {
            {70, 25, 5,  0,  0,  0,  0,  0},
            {30, 45, 20, 5,  0,  0,  0,  0},
            {10, 25, 40, 20, 5,  0,  0,  0},
            {0,  10, 25, 40, 20, 5,  0,  0},
            {0,  0,  10, 25, 40, 20, 5,  0},
            {0,  0,  0,  10, 25, 40, 20, 5},
            {0,  0,  0,  0,  10, 30, 45, 15},
            {0,  0,  0,  0,  0,  10, 40, 50}
    };

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, GUI_NAME);
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 45; i++) {
            boolean isFoodSlot = false;
            for (int s : FOOD_SLOTS) if (i == s) isFoodSlot = true;
            if (i != 10 && i != 12 && i != 28 && !isFoodSlot) inv.setItem(i, bg);
        }

        inv.setItem(10, getPlaceholder(10));
        inv.setItem(12, getPlaceholder(12));
        for (int s : FOOD_SLOTS) inv.setItem(s, getPlaceholder(s));

        updateCraftButton(inv);
        player.openInventory(inv);
    }

    private ItemStack getPlaceholder(int slot) {
        if (slot == 10) return createItem(Material.YELLOW_STAINED_GLASS_PANE, "§eÔ Phôi Trang Sức");
        if (slot == 12) return createItem(Material.PURPLE_STAINED_GLASS_PANE, "§dÔ Tinh Thể Pha Lê");
        for (int s : FOOD_SLOTS) {
            if (slot == s) return createItem(Material.GOLD_NUGGET, "§6Nguyên Liệu Hiến Tế", "§7Chỉ chấp nhận Trang Sức.");
        }
        return null;
    }

    private boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name.contains("Ô Phôi") || name.contains("Ô Tinh Thể") || name.contains("Nguyên Liệu Hiến Tế");
    }

    // Lấy cấp độ của Trang sức để tính trung bình cộng
    private int getTrinketLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return 1;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("▶ Yêu cầu cấp độ:")) {
                try {
                    return Integer.parseInt(clean.replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
            }
        }
        return 1;
    }

    // ==========================================
    // CÔNG CỤ ĐO LƯỜNG PIXEL (CHUẨN MINECRAFT FONT)
    // ==========================================
    private int getPixelWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        boolean isBold = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l') isBold = true;
                else if (code == 'r' || (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) isBold = false;
                i++;
                continue;
            }

            int charWidth = 6;
            // [ĐÃ SỬA LỖI]: Xóa số '1' khỏi nhóm 4px. Số 1 rộng 6px như các số khác!
            if (c == 'i' || c == 'l' || c == '!' || c == '.' || c == ',' || c == ':' || c == ';' || c == '|' || c == '\'') charWidth = 2;
            else if (c == ' ' || c == 'I' || c == '[' || c == ']' || c == 't') charWidth = 4;
            else if (c == 'f' || c == 'k' || c == '"' || c == '<' || c == '>' || c == '{' || c == '}' || c == '*' || c == '(' || c == ')') charWidth = 5;
            else if (c == '@' || c == '~') charWidth = 7;

            if (isBold) charWidth += 1; // Bold áp dụng cho cả khoảng trắng
            width += charWidth;
        }
        return width;
    }

    // Thuật toán của Bạn: Căn lề trái, sau đó BÙ KHOẢNG TRẮNG vào bên PHẢI cho đủ độ dài
    private String padRightExact(String text, int targetPx) {
        if (text == null) text = "";
        int width = getPixelWidth(text);
        int missing = targetPx - width;
        if (missing <= 0) return text;

        int boldSpaces = 0;
        int normalSpaces = 0;

        // Phương trình tìm kết hợp hoàn hảo giữa khoảng trắng 4px và 5px
        for (int b = 0; b <= missing / 5; b++) {
            if ((missing - b * 5) % 4 == 0) {
                boldSpaces = b;
                normalSpaces = (missing - b * 5) / 4;
                break;
            }
        }

        StringBuilder sb = new StringBuilder(text);
        sb.append("§r"); // Reset màu trước khi chèn khoảng trắng để không bị lỗi màu

        if (boldSpaces > 0) {
            sb.append("§l");
            for(int i = 0; i < boldSpaces; i++) sb.append(" ");
            sb.append("§r");
        }
        for(int i = 0; i < normalSpaces; i++) sb.append(" ");

        return sb.toString();
    }

    // ==========================================
    // CẬP NHẬT GIAO DIỆN HIỂN THỊ
    // ==========================================
    private void updateCraftButton(Inventory inv) {
        ItemStack core = inv.getItem(10);
        ItemStack crystal = inv.getItem(12);

        boolean hasCore = (core != null && !isPlaceholder(core) && core.hasItemMeta() && core.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING) != null && core.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING).startsWith("trinket_blank_"));
        boolean hasCrystal = (crystal != null && !isPlaceholder(crystal) && crystal.hasItemMeta() && "misc_trinket_crystal".equals(crystal.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING)));

        int foodCount = 0;
        int totalLevel = 0;
        double[] avgTiers = new double[8];
        Map<String, Integer> lineageCounts = new HashMap<>();

        for (int s : FOOD_SLOTS) {
            ItemStack food = inv.getItem(s);
            if (food != null && !isPlaceholder(food) && food.hasItemMeta() && food.getItemMeta().getPersistentDataContainer().has(TRINKET_KEY, PersistentDataType.STRING)) {
                foodCount++;
                totalLevel += getTrinketLevel(food);

                int tier = getTierFromItem(food);
                if (tier != -1) {
                    for (int i = 0; i < 8; i++) avgTiers[i] += TIER_RATES[tier][i];
                }
                String lineage = getLineageFromItem(food);
                if (lineage != null) lineageCounts.put(lineage, lineageCounts.getOrDefault(lineage, 0) + 1);
            }
        }

        int avgLevel = foodCount > 0 ? (totalLevel / foodCount) : 1;

        if (!hasCore || !hasCrystal || foodCount == 0) {
            inv.setItem(28, createItem(Material.BARRIER, "§cThiếu Nguyên Liệu", "§7Cần 1 Phôi, 1 Tinh thể", "§7và ít nhất 1 Trang sức hiến tế."));
            return;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8Thông số dự kiến khi ghép:");
        lore.add("");

        // Cố định độ rộng cột Phẩm Chất là 105 pixel
        int colPx = 105;

        // Căn lề theo logic: [Chữ Trái] + [Khoảng Trắng Bù Đủ 105px] + [Chữ Phải]
        lore.add("  " + padRightExact("§fPhẩm chất", colPx) + "§fDòng dõi");
        lore.add(" §8--------------------------------------");

        List<String> tierLines = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            double pct = avgTiers[i] / foodCount;
            if (pct > 0) tierLines.add(TIER_COLORS[i] + TIER_NAMES[i] + "§f: " + String.format("%.1f", pct) + "%");
        }

        List<String> lineageLines = new ArrayList<>();
        double randomLineagePct = 100.0;

        List<Map.Entry<String, Integer>> sortedLineages = new ArrayList<>(lineageCounts.entrySet());
        sortedLineages.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sortedLineages) {
            double pct = entry.getValue() * 10.0;
            randomLineagePct -= pct;
            lineageLines.add("§d" + capitalize(entry.getKey()) + "§f: " + String.format("%.1f", pct) + "%");
        }
        if (randomLineagePct > 0) {
            lineageLines.add("§7Ngẫu nhiên§f: " + String.format("%.1f", randomLineagePct) + "%");
        }

        int maxSize = Math.max(tierLines.size(), lineageLines.size());

        for (int i = 0; i < maxSize; i++) {
            String left = (i < tierLines.size()) ? tierLines.get(i) : "";
            String right = (i < lineageLines.size()) ? lineageLines.get(i) : "";

            // Ép độ dài cột trái bằng ĐÚNG colPx (105px), phần dư được tự lấp bằng khoảng trắng
            String paddedLeft = padRightExact(left, colPx);

            // Cột phải tự động nối tiếp thẳng tắp do cột trái luôn kết thúc ở 1 tọa độ cố định
            lore.add("  " + paddedLeft + right);
        }

        lore.add("");
        lore.add("§8▶ §fCấp độ trang sức dự kiến: §a" + avgLevel);
        lore.add("");
        lore.add("§eNhấp chuột để Bắt đầu Dung hợp");

        inv.setItem(28, createItem(Material.NETHER_STAR, "§5§lTiến Hành Ghép", lore.toArray(new String[0])));
    }

    private int getTierFromItem(ItemStack item) {
        String name = item.getItemMeta().getDisplayName();
        for (int i = 0; i < TIER_COLORS.length; i++) {
            if (name.contains(TIER_COLORS[i])) return i;
        }
        return 0;
    }

    private String getLineageFromItem(ItemStack item) {
        if (!item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("✦ Thuộc tính Dòng dõi (")) {
                return clean.substring(clean.indexOf("(") + 1, clean.indexOf(")")).toLowerCase();
            }
        }
        return null;
    }

    private String capitalize(String str) { return str == null || str.isEmpty() ? str : str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase(); }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        if (event.getClick().isKeyboardClick() || event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        boolean isInteractable = (slot == 10 || slot == 12);
        for (int s : FOOD_SLOTS) if (slot == s) isInteractable = true;

        if (slot < 45 && !isInteractable && slot != 28) {
            event.setCancelled(true);
            return;
        }

        if (isInteractable) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            if (isPlaceholder(current)) {
                event.setCancelled(true);
                if (cursor != null && cursor.getType() != Material.AIR) {

                    boolean isFoodSlot = false;
                    for (int s : FOOD_SLOTS) if (slot == s) isFoodSlot = true;
                    if (isFoodSlot && (!cursor.hasItemMeta() || !cursor.getItemMeta().getPersistentDataContainer().has(TRINKET_KEY, PersistentDataType.STRING))) {
                        player.sendMessage("§c§l[!] §cChỉ được phép hiến tế Trang sức vào ô này!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    // Nuốt toàn bộ đồ đang cầm trên chuột (Cho phép đặt số lượng nhiều)
                    inv.setItem(slot, cursor.clone());
                    event.getView().setCursor(null);

                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                }
            } else {
                event.setCancelled(false);
            }

            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> {
                ItemStack after = inv.getItem(slot);
                if (after == null || after.getType() == Material.AIR) inv.setItem(slot, getPlaceholder(slot));
                updateCraftButton(inv);
            }, 1L);
            return;
        }

        if (slot == 28) {
            event.setCancelled(true);
            if (inv.getItem(28).getType() == Material.BARRIER) return;

            ItemStack core = inv.getItem(10);
            String type = core.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING).replace("trinket_blank_", "");

            int foodCount = 0;
            int totalLevel = 0;
            double[] avgTiers = new double[8];
            Map<String, Integer> lineageCounts = new HashMap<>();

            for (int s : FOOD_SLOTS) {
                ItemStack food = inv.getItem(s);
                if (food != null && !isPlaceholder(food)) {
                    foodCount++;
                    totalLevel += getTrinketLevel(food);

                    int tier = getTierFromItem(food);
                    for (int i = 0; i < 8; i++) avgTiers[i] += TIER_RATES[tier][i];
                    String lineage = getLineageFromItem(food);
                    if (lineage != null) lineageCounts.put(lineage, lineageCounts.getOrDefault(lineage, 0) + 1);
                }
            }

            int avgLevel = foodCount > 0 ? (totalLevel / foodCount) : 1;

            Random rand = new Random();
            double rollTier = rand.nextDouble() * 100.0;
            double currentTierSum = 0;
            int finalTier = 0;
            for (int i = 0; i < 8; i++) {
                currentTierSum += (avgTiers[i] / foodCount);
                if (rollTier <= currentTierSum) { finalTier = i; break; }
            }

            double rollLineage = rand.nextDouble() * 100.0;
            double currentLinSum = 0;
            String finalLineage = null;
            for (Map.Entry<String, Integer> entry : lineageCounts.entrySet()) {
                currentLinSum += (entry.getValue() * 10.0);
                if (rollLineage <= currentLinSum) { finalLineage = entry.getKey(); break; }
            }

            // [ĐÃ FIX BUG]: Trừ vật phẩm an toàn mà không làm mất cả stack
            if (core.getAmount() > 1) {
                core.setAmount(core.getAmount() - 1);
            } else {
                inv.setItem(10, getPlaceholder(10));
            }

            ItemStack crystal = inv.getItem(12);
            if (crystal.getAmount() > 1) {
                crystal.setAmount(crystal.getAmount() - 1);
            } else {
                inv.setItem(12, getPlaceholder(12));
            }

            for (int s : FOOD_SLOTS) {
                ItemStack f = inv.getItem(s);
                if (f != null && !isPlaceholder(f)) {
                    if (f.getAmount() > 1) f.setAmount(f.getAmount() - 1);
                    else inv.setItem(s, getPlaceholder(s));
                }
            }

            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> updateCraftButton(inv), 1L);

            ItemStack result = TrinketGenerator.createTrinketExact(type, avgLevel, finalLineage, -1, finalTier);
            player.getInventory().addItem(result);

            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            player.sendMessage("§5§l[Ghép Đồ] §aBạn đã chế tạo ra một Trang sức Cấp " + avgLevel + " bằng Giả Kim Thuật!");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        int[] safeSlots = {10, 12, 14, 15, 16, 23, 24, 25, 32, 33, 34};
        for (int slot : safeSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !isPlaceholder(item)) player.getInventory().addItem(item);
        }
    }
}