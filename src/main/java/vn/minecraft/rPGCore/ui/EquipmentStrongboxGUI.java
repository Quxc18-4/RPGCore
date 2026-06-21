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
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.commands.EquipmentCommand;

import java.util.*;

public class EquipmentStrongboxGUI implements Listener {

    public static final String GUI_NAME = "§6§lDung Hợp Trang Bị";

    // [ĐÃ SỬA]: Đồng bộ hóa 8 Bậc Phẩm Chất y hệt EquipmentCommand
    private final String[] TIER_NAMES = {"Common", "Uncommon", "Rare", "Epic", "Mythical", "Heroic", "Legendary", "Divine"};
    private final String[] TIER_COLORS = {"§7", "§a", "§b", "§5", "§d", "§6", "§c", "§e"};

    // [ĐÃ SỬA]: Cập nhật Ma trận Tỉ lệ lên 8x8
    private final double[][] BASE_RATES = {
            {70, 25,  5,  0,  0,  0,  0,  0}, // 0: Common Avg
            {30, 45, 20,  5,  0,  0,  0,  0}, // 1: Uncommon Avg
            {10, 25, 40, 20,  5,  0,  0,  0}, // 2: Rare Avg
            { 0, 10, 25, 40, 20,  5,  0,  0}, // 3: Epic Avg
            { 0,  0, 10, 25, 40, 20,  5,  0}, // 4: Mythical Avg
            { 0,  0,  0, 10, 25, 40, 20,  5}, // 5: Heroic Avg
            { 0,  0,  0,  0, 10, 30, 45, 15}, // 6: Legendary Avg
            { 0,  0,  0,  0,  0, 10, 40, 50}  // 7: Divine Avg
    };

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, GUI_NAME);

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) {
            if (i != 40) inv.setItem(i, glass);
        }

        updateButton(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    private int getTierFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        String name = item.getItemMeta().getDisplayName();
        for (int i = 0; i < TIER_COLORS.length; i++) {
            if (name.contains(TIER_COLORS[i])) return i;
        }
        return 0; // Default Common
    }

    private int getLevelFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return 1;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("▶ Yêu cầu cấp độ:")) {
                try { return Integer.parseInt(clean.replaceAll("[^0-9]", "")); }
                catch (Exception ignored) {}
            }
        }
        return 1;
    }

    private boolean isValidEquipment(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        org.bukkit.NamespacedKey trinketKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "trinket_type");
        if (meta.getPersistentDataContainer().has(trinketKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            return false;
        }

        for (String line : meta.getLore()) {
            String clean = ChatColor.stripColor(line).trim();
            if (clean.startsWith("- Sát thương vật lý:") ||
                    clean.startsWith("- Sát thương phép thuật:") ||
                    clean.startsWith("- Giáp:") ||
                    clean.startsWith("- Kháng phép:") ||
                    clean.startsWith("- Máu tối đa:")) {
                return true;
            }
        }
        return false;
    }

    private void updateButton(Inventory inv) {
        List<ItemStack> validItems = new ArrayList<>();
        int totalLevel = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (isValidEquipment(item)) {
                validItems.add(item);
                totalLevel += getLevelFromItem(item);
            }
        }

        int count = validItems.size();

        if (count == 0) {
            inv.setItem(40, createItem(Material.BARRIER, "§c§lChưa có vật phẩm", "§7Hãy đặt trang bị rác vào các ô trống bên trên."));
            return;
        }

        if (count % 3 != 0) {
            inv.setItem(40, createItem(Material.RED_STAINED_GLASS_PANE, "§c§lSố lượng không hợp lệ",
                    "§7Số lượng trang bị hiến tế phải",
                    "§7là bội số của 3 (3, 6, 9... 36).",
                    "§cĐang có: §e" + count + " §cmón."));
            return;
        }

        int crafts = count / 3;
        int avgLevel = totalLevel / count;

        List<String> lore = new ArrayList<>();
        lore.add("§8Số lượt dung hợp: §a" + crafts + " Lần");
        lore.add("§8Cấp độ thành phẩm dự kiến: §e~" + avgLevel);
        lore.add("");

        double[] finalRates = calculateBulkRates(validItems);
        lore.add("§fDự kiến Phẩm chất nhận được:");
        // [ĐÃ SỬA]: Vòng lặp từ 7 xuống 0
        for (int i = 7; i >= 0; i--) {
            if (finalRates[i] > 0.01) {
                lore.add(" " + TIER_COLORS[i] + TIER_NAMES[i] + "§f: " + String.format("%.1f", finalRates[i]) + "%");
            }
        }

        lore.add("");
        lore.add("§e▶ Nhấp chuột để Dung hợp toàn bộ");

        inv.setItem(40, createItem(Material.ENDER_EYE, "§6§l[★] TIẾN HÀNH DUNG HỢP", lore.toArray(new String[0])));
    }

    private double[] calculateBulkRates(List<ItemStack> items) {
        // [ĐÃ SỬA]: Mảng 8 Bậc
        double[] globalRates = new double[8];
        int chunks = items.size() / 3;

        items.sort(Comparator.comparingInt(this::getTierFromItem));

        for (int c = 0; c < chunks; c++) {
            ItemStack i1 = items.get(c * 3);
            ItemStack i2 = items.get(c * 3 + 1);
            ItemStack i3 = items.get(c * 3 + 2);

            int t1 = getTierFromItem(i1);
            int t2 = getTierFromItem(i2);
            int t3 = getTierFromItem(i3);

            int avgTierIndex = Math.round((t1 + t2 + t3) / 3.0f);
            double[] chunkRates = Arrays.copyOf(BASE_RATES[avgTierIndex], 8);

            // Combo Đột Phá khi bỏ 3 món cùng phẩm chất
            // Kích hoạt Combo Đột Phá khi 3 món cùng phẩm chất
            if (t1 == t2 && t2 == t3) {
                int low1 = -1, low2 = -1;

                // Tìm 2 mốc phẩm chất thấp nhất đang có tỉ lệ trong cụm
                for (int i = 0; i < 8; i++) {
                    if (chunkRates[i] > 0.0) {
                        if (low1 == -1) low1 = i;
                        else if (low2 == -1) { low2 = i; break; }
                    }
                }

                double totalBoost = 0;

                // Giảm nhẹ tỉ lệ của mốc cùi nhất (Tối đa trừ 5%)
                if (low1 != -1) {
                    double sub1 = Math.min(chunkRates[low1], 5.0);
                    chunkRates[low1] -= sub1;
                    totalBoost += sub1;
                }

                // Giảm nhiều hơn ở mốc cùi thứ hai (Tối đa trừ 10%)
                if (low2 != -1) {
                    double sub2 = Math.min(chunkRates[low2], 10.0);
                    chunkRates[low2] -= sub2;
                    totalBoost += sub2;
                }

                // Bù trừ lượng tỉ lệ vừa thu được dồn lên 2 cấp cao hơn
                if (totalBoost > 0) {
                    int up1 = Math.min(7, t1 + 1); // Cấp cao hơn 1 bậc
                    int up2 = Math.min(7, t1 + 2); // Cấp cao hơn 2 bậc

                    if (up1 != up2) {
                        chunkRates[up1] += totalBoost * 0.4; // Cộng 40% cho cấp +1
                        chunkRates[up2] += totalBoost * 0.6; // Cộng 60% cho cấp +2 (Nổ hũ)
                    } else {
                        chunkRates[up1] += totalBoost; // Kịch khung (Divine) thì dồn hết
                    }
                }
            }
            for (int i = 0; i < 8; i++) globalRates[i] += chunkRates[i];
        }
        for (int i = 0; i < 8; i++) globalRates[i] /= chunks;
        return globalRates;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND ||
                event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK ||
                event.getClick().isKeyboardClick()) {
            event.setCancelled(true);
            return;
        }

        if (slot >= 45) {
            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> updateButton(inv), 1L);
            return;
        }

        if (slot >= 0 && slot < 36) {
            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> updateButton(inv), 1L);
            return;
        }

        event.setCancelled(true);

        if (slot == 40) {
            ItemStack btn = inv.getItem(40);
            if (btn == null || btn.getType() != Material.ENDER_EYE) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            List<ItemStack> validItems = new ArrayList<>();
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                if (isValidEquipment(item)) validItems.add(item);
            }

            int count = validItems.size();
            if (count % 3 != 0) return;

            int chunks = count / 3;
            validItems.sort(Comparator.comparingInt(this::getTierFromItem));

            Random rand = new Random();
            for (int c = 0; c < chunks; c++) {
                ItemStack i1 = validItems.get(c * 3);
                ItemStack i2 = validItems.get(c * 3 + 1);
                ItemStack i3 = validItems.get(c * 3 + 2);

                int t1 = getTierFromItem(i1);
                int t2 = getTierFromItem(i2);
                int t3 = getTierFromItem(i3);

                int avgLevel = (getLevelFromItem(i1) + getLevelFromItem(i2) + getLevelFromItem(i3)) / 3;
                avgLevel += (rand.nextInt(5) - 2);
                if (avgLevel < 1) avgLevel = 1;

                int avgTierIndex = Math.round((t1 + t2 + t3) / 3.0f);
                double[] chunkRates = Arrays.copyOf(BASE_RATES[avgTierIndex], 8);

                // Kích hoạt Combo Đột Phá khi 3 món cùng phẩm chất
                if (t1 == t2 && t2 == t3) {
                    int low1 = -1, low2 = -1;

                    // Tìm 2 mốc phẩm chất thấp nhất đang có tỉ lệ trong cụm
                    for (int i = 0; i < 8; i++) {
                        if (chunkRates[i] > 0.0) {
                            if (low1 == -1) low1 = i;
                            else if (low2 == -1) { low2 = i; break; }
                        }
                    }

                    double totalBoost = 0;

                    // Giảm nhẹ tỉ lệ của mốc cùi nhất (Tối đa trừ 5%)
                    if (low1 != -1) {
                        double sub1 = Math.min(chunkRates[low1], 5.0);
                        chunkRates[low1] -= sub1;
                        totalBoost += sub1;
                    }

                    // Giảm nhiều hơn ở mốc cùi thứ hai (Tối đa trừ 10%)
                    if (low2 != -1) {
                        double sub2 = Math.min(chunkRates[low2], 10.0);
                        chunkRates[low2] -= sub2;
                        totalBoost += sub2;
                    }

                    // Bù trừ lượng tỉ lệ vừa thu được dồn lên 2 cấp cao hơn
                    if (totalBoost > 0) {
                        int up1 = Math.min(7, t1 + 1); // Cấp cao hơn 1 bậc
                        int up2 = Math.min(7, t1 + 2); // Cấp cao hơn 2 bậc

                        if (up1 != up2) {
                            chunkRates[up1] += totalBoost * 0.4; // Cộng 40% cho cấp +1
                            chunkRates[up2] += totalBoost * 0.6; // Cộng 60% cho cấp +2 (Nổ hũ)
                        } else {
                            chunkRates[up1] += totalBoost; // Kịch khung (Divine) thì dồn hết
                        }
                    }
                }

                double roll = rand.nextDouble() * 100.0;
                double sum = 0;
                int finalTier = 0;
                for (int i = 0; i < 8; i++) {
                    sum += chunkRates[i];
                    if (roll <= sum) { finalTier = i; break; }
                }

                String eqType = rand.nextBoolean() ? "weapon" : "armor";
                ItemStack result = EquipmentCommand.createGachaItem(eqType, avgLevel, null, finalTier, null, -1, -1);
                player.getInventory().addItem(result);
            }

            for (int i = 0; i < 36; i++) inv.setItem(i, null);
            updateButton(inv);

            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            player.sendMessage("§6§l[Tráp Phân Rã] §aThành công phân rã " + (chunks * 3) + " trang bị rác thành " + chunks + " trang bị mới tinh!");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}