package vn.minecraft.rPGCore.managers;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.brewing.BrewingEngine;
import vn.minecraft.rPGCore.brewing.PotionData;
import vn.minecraft.rPGCore.managers.ItemManager.ItemData;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BrewingManager implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final BrewingEngine engine;
    private final File folder;

    public static final String GUI_NAME = "§5§lVạc Pha Chế Thần Bí";

    private final int[] HERB_SLOTS = {10, 11, 12, 19, 20, 21};
    private final int[] SPICE_SLOTS = {37, 38, 39};
    private final int WATER_SLOT = 23;
    private final int SOLVENT_SLOT = 25;
    private final int BUTTON_SLOT = 41;
    private final int OUTPUT_SLOT = 43;

    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

    public BrewingManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.engine = new BrewingEngine();

        this.folder = new File(plugin.getDataFolder(), "potions");
        if (!folder.exists()) folder.mkdirs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- HÀM HELPER: TRỪ ĐỘ BỀN NBT ĐẠO CỤ TRONG VẠC ---
    private void reduceToolDurability(Player player, ItemStack item, int maxUses) {
        ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "alch_uses");
        int uses = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
        uses++;
        if (uses >= maxUses) {
            item.setAmount(0);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage("§c§l[!] §cMuôi Trộn Bạc Cổ của bạn đã vỡ nát sau thời gian dài sử dụng!");
        } else {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, uses);
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.removeIf(l -> l.contains("Độ bền:"));
            lore.add("§fĐộ bền: §a" + (maxUses - uses) + "§8/§a" + maxUses);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onBrewingStandClick(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        if (event.getAction().isRightClick() && event.hasBlock() && event.getClickedBlock().getType() == Material.BREWING_STAND) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            // [FIX GHOST ITEM KHI MỞ VẠC]
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);

            long now = System.currentTimeMillis();
            if (clickCooldowns.getOrDefault(player.getUniqueId(), 0L) > now) return;
            clickCooldowns.put(player.getUniqueId(), now + 200L);

            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
            openBrewingGUI(player);
        }
    }

    @EventHandler
    public void onBrewingInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        // [FIX GHOST ITEM KHI KÉO THẢ NHIỀU Ô]
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> ((Player) event.getWhoClicked()).updateInventory(), 1L);

        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void openBrewingGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_NAME);

        ItemStack bg = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        for (int s : HERB_SLOTS) inv.setItem(s, null);
        for (int s : SPICE_SLOTS) inv.setItem(s, null);
        inv.setItem(WATER_SLOT, null);
        inv.setItem(SOLVENT_SLOT, null);
        inv.setItem(OUTPUT_SLOT, null);

        inv.setItem(9, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§e§lThảo Dược Chính", "§7Đặt tối đa 6 loại thảo dược vào các ô bên phải."));
        inv.setItem(36, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§d§lChất Điều Vị", "§7Đặt tối đa 3 loại gia vị/thảo mộc khử vị."));
        inv.setItem(14, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§b§lNước Nền", "§7Đặt Nước, Bình Sứ hoặc Thuốc muốn Cường hóa."));
        inv.setItem(16, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§c§lDung Môi Buff", "§7Bỏ chất xúc tác vào ô bên dưới để tăng tỉ lệ."));

        updateBrewButton(inv, player);
        player.openInventory(inv);
    }

    private ItemStack createPane(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) meta.setLore(Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    private void updateBrewButton(Inventory inv, Player player) {
        List<ItemData> ingredients = new ArrayList<>();
        boolean isEnhancing = false;

        ItemStack waterItem = inv.getItem(WATER_SLOT);
        if (waterItem != null && waterItem.getType() != Material.AIR) {
            String wId = itemManager.getItemId(waterItem);
            // [CẬP NHẬT] Hỗ trợ nhận diện Bình Sứ Nạp Nước
            if (wId != null && (wId.startsWith("water_") || wId.startsWith("alch_thermal_flask_water"))) {
                ingredients.add(itemManager.getItemData(wId));
            } else {
                isEnhancing = true;
            }
        }

        // 1. Quét 6 ô Thảo dược chính
        for (int s : HERB_SLOTS) {
            ItemStack i = inv.getItem(s);
            if (i != null && i.getType() != Material.AIR) {
                String id = itemManager.getItemId(i);
                if (id != null) ingredients.add(itemManager.getItemData(id));
            }
        }

        // 2. Quét 3 ô Điều vị (Ép kiểu SPICE để lách hệ thống trừ tỉ lệ)
        for (int s : SPICE_SLOTS) {
            ItemStack i = inv.getItem(s);
            if (i != null && i.getType() != Material.AIR) {
                ItemData original = itemManager.getItemData(itemManager.getItemId(i));
                if (original != null) {
                    ItemData spiceClone = new ItemData(original.id, original.rarity, ItemManager.Category.SPICE, original.points, original.material, original.name, original.effect, original.description.toArray(new String[0]));
                    ingredients.add(spiceClone);
                }
            }
        }

        // 3. Quét Dung môi
        ItemStack solvItem = inv.getItem(SOLVENT_SLOT);
        if (solvItem != null && solvItem.getType() != Material.AIR) {
            String id = itemManager.getItemId(solvItem);
            if (id != null) ingredients.add(itemManager.getItemData(id));
        }

        double successRate = engine.getSuccessRate(ingredients, player, isEnhancing);
        String rateColor = successRate >= 80 ? "§a" : (successRate >= 40 ? "§e" : "§c");

        ItemStack brewBtn = createPane(Material.PURPLE_STAINED_GLASS_PANE, "§d§l[ NẤU THUỐC ]",
                "§fClick vào đây để luyện dược!", "", "§7Tỉ lệ thành công: " + rateColor + String.format("%.1f", successRate) + "%");
        inv.setItem(BUTTON_SLOT, brewBtn);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        int[] safeSlots = {10, 11, 12, 19, 20, 21, 37, 38, 39, 23, 25, 43};
        for (int s : safeSlots) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR) {
                if (player.getInventory().firstEmpty() == -1) player.getWorld().dropItemNaturally(player.getLocation(), item);
                else player.getInventory().addItem(item);
                inv.setItem(s, null);
            }
        }
    }

    @EventHandler
    public void onBrewingInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        Player player = (Player) event.getWhoClicked();

        // [FIX GHOST ITEM TỔNG LỰC CHO MỌI THAO TÁC CLICK TRONG GUI]
        // Đặt ở ngay đầu hàm, mọi thao tác trả về (return) bên dưới đều sẽ được đồng bộ an toàn!
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getClick().isKeyboardClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true); player.updateInventory(); return;
        }

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType().name().endsWith("GLASS_PANE") && slot != BUTTON_SLOT) {
            event.setCancelled(true); player.updateInventory(); return;
        }

        Inventory inv = event.getInventory();
        ItemStack cursor = event.getCursor();

        if (cursor != null && cursor.getType() != Material.AIR) {
            String cursorId = itemManager.getItemId(cursor);
            ItemData data = itemManager.getItemData(cursorId);

            if (slot == WATER_SLOT) {
                // [CẬP NHẬT] Nhận diện Bình sứ
                boolean isWater = cursorId != null && (cursorId.startsWith("water_") || cursorId.startsWith("alch_thermal_flask_water"));
                boolean isCustomPotion = cursor.hasItemMeta() && cursor.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "potion_uuid"), PersistentDataType.STRING);

                if (!isWater && !isCustomPotion) {
                    event.setCancelled(true); player.sendMessage("§c§l[!] §cChỉ được đặt Nước, Bình Sứ hoặc Thuốc vào ô này!");
                    player.updateInventory(); return;
                }
                if (isCustomPotion) {
                    String uuid = cursor.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "potion_uuid"), PersistentDataType.STRING);
                    PotionData old = loadPotionFromFile(uuid);
                    if (old == null || old.isEnhanced || old.isRuined) {
                        event.setCancelled(true); player.sendMessage("§c§l[!] §cThuốc bị lỗi, hỏng hoặc đã được Cường Hóa!");
                        player.updateInventory(); return;
                    }
                }
            } else if (slot == 10 || slot == 11 || slot == 12 || slot == 19 || slot == 20 || slot == 21) {
                // [SỬA LỖI]: Báo lỗi nếu đặt sai đồ, CẤM đặt Dung Môi vào ô Thảo Dược
                if (data == null || data.category != ItemManager.Category.HERB || data.id.startsWith("herb_solvent_")) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[!] §cÔ này chỉ dành riêng cho Thảo Dược Chính!");
                    player.updateInventory(); return;
                }
            } else if (slot == 37 || slot == 38 || slot == 39) {
                // [SỬA LỖI]: Báo lỗi nếu đặt sai, CẤM đặt Dung Môi vào ô Gia Vị
                if (data == null || (data.category != ItemManager.Category.SPICE && data.category != ItemManager.Category.HERB) || data.id.startsWith("herb_solvent_")) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[!] §cÔ này chỉ dành cho Gia vị hoặc Thảo mộc khử vị!");
                    player.updateInventory(); return;
                }
            } else if (slot == SOLVENT_SLOT) {
                // [SỬA LỖI]: CHỐT CỨNG chỉ nhận các món có ID Dung Môi
                if (data == null || !data.id.startsWith("herb_solvent_")) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[!] §cÔ này chỉ dành riêng cho Dung Môi!");
                    player.updateInventory(); return;
                }
            } else if (slot != BUTTON_SLOT && slot != OUTPUT_SLOT) {
                event.setCancelled(true); player.updateInventory(); return;
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateBrewButton(inv, player), 1L);

        // NÚT BẤM NẤU THUỐC
        if (slot == BUTTON_SLOT) {
            event.setCancelled(true);
            player.updateInventory();
            if (inv.getItem(OUTPUT_SLOT) != null && inv.getItem(OUTPUT_SLOT).getType() != Material.AIR) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§c§l[!] §cTúi đồ của bạn đã đầy!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); return;
                }
                player.getInventory().addItem(inv.getItem(OUTPUT_SLOT));
                inv.setItem(OUTPUT_SLOT, null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            }

            ItemStack waterItem = inv.getItem(WATER_SLOT);
            if (waterItem == null || waterItem.getType() == Material.AIR) {
                player.sendMessage("§c§l[!] §cKhông có Nước Nền thì vạc sẽ cháy đen mất!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); return;
            }

            List<ItemData> ingredients = new ArrayList<>();
            PotionData basePotion = null;
            String oldUuidToDelete = null;

            String waterId = itemManager.getItemId(waterItem);
            boolean isThermalFlask = waterId != null && waterId.startsWith("alch_thermal_flask_water");

            if (waterId != null && (waterId.startsWith("water_") || isThermalFlask)) {
                ingredients.add(itemManager.getItemData(waterId));
            } else {
                oldUuidToDelete = waterItem.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "potion_uuid"), PersistentDataType.STRING);
                basePotion = loadPotionFromFile(oldUuidToDelete);
            }

            boolean hasHerb = false;
            for (int s : HERB_SLOTS) {
                if (inv.getItem(s) != null && inv.getItem(s).getType() != Material.AIR) {
                    ingredients.add(itemManager.getItemData(itemManager.getItemId(inv.getItem(s))));
                    hasHerb = true;
                }
            }

            if (!hasHerb && basePotion == null) {
                player.sendMessage("§c§l[!] §cVui lòng bỏ ít nhất 1 Thảo dược!"); return;
            }

            // BẰNG ĐOẠN CODE ÉP KIỂU NÀY:
            for (int s : SPICE_SLOTS) {
                if (inv.getItem(s) != null && inv.getItem(s).getType() != Material.AIR) {
                    ItemData original = itemManager.getItemData(itemManager.getItemId(inv.getItem(s)));
                    if (original != null) {
                        ItemData spiceClone = new ItemData(original.id, original.rarity, ItemManager.Category.SPICE, original.points, original.material, original.name, original.effect, original.description.toArray(new String[0]));
                        ingredients.add(spiceClone);
                    }
                }
            }
            if (inv.getItem(SOLVENT_SLOT) != null && inv.getItem(SOLVENT_SLOT).getType() != Material.AIR) {
                ingredients.add(itemManager.getItemData(itemManager.getItemId(inv.getItem(SOLVENT_SLOT))));
            }

            PotionData resultData = engine.evaluate(ingredients, player, basePotion);

            // ==========================================
            // LOGIC ÁP DỤNG ĐẠO CỤ CHUYÊN NGÀNH
            // ==========================================
            if (isThermalFlask) {
                resultData.toxicityPoints *= 0.8;
                // [CẬP NHẬT MỚI]: Nhồi thông tin Bình Sứ vào file cấu hình của Potion
                resultData.specialEffects.add("THERMAL_FLASK");
            }

            ItemStack spoon = null;
            for (ItemStack i : player.getInventory().getContents()) {
                if (i != null && "alch_silver_spoon".equals(itemManager.getItemId(i))) { spoon = i; break; }
            }

            if (spoon != null) {
                // Muôi Bạc buff 5% sức mạnh toàn bộ Stats
                for (String stat : resultData.customStats.keySet()) {
                    resultData.customStats.put(stat, resultData.customStats.get(stat) * 1.05);
                }
                reduceToolDurability(player, spoon, 30); // Giới hạn 30 lần
            }

            savePotionToFile(resultData);

            vn.minecraft.rPGCore.jobs.core.JobManager.processAlchemistBrewExp(player, resultData, ingredients);

            if (oldUuidToDelete != null) deletePotionFile(oldUuidToDelete);

            // [CẬP NHẬT MỚI]: Xử lý tiêu hao Nước Nền (Tiêu hao luôn bình sứ, không trả lại Vạc)
            waterItem.setAmount(waterItem.getAmount() - 1);

            // Xử lý tiêu hao Nước Nền
            if (isThermalFlask) {
                // Biến đổi lại thành bình sứ rỗng và trả lại Vạc
                ItemStack emptyFlask = itemManager.buildItem("alch_thermal_flask_empty", 1);
                inv.setItem(WATER_SLOT, emptyFlask);
            } else {
                waterItem.setAmount(waterItem.getAmount() - 1);
            }

            // Xử lý tiêu hao các nguyên liệu khác
            for (int s : HERB_SLOTS) if (inv.getItem(s) != null) inv.getItem(s).setAmount(inv.getItem(s).getAmount() - 1);
            for (int s : SPICE_SLOTS) if (inv.getItem(s) != null) inv.getItem(s).setAmount(inv.getItem(s).getAmount() - 1);
            if (inv.getItem(SOLVENT_SLOT) != null) inv.getItem(SOLVENT_SLOT).setAmount(inv.getItem(SOLVENT_SLOT).getAmount() - 1);

            // Hiệu ứng và Cập nhật Quest
            if (resultData.isRuined) {
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
                player.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_SIGNAL_SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0);
            }

            // Xử lý Dung Môi Song Sinh (Nhân đôi kết quả)
            ItemStack outputItem = buildPotionItem(resultData);
            if (resultData.flavorTags.contains("§d[Song Sinh]")) {
                outputItem.setAmount(2);
            }
            inv.setItem(OUTPUT_SLOT, outputItem);
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateBrewButton(inv, player), 1L);
        }
    }

    private ItemStack buildPotionItem(PotionData data) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(org.bukkit.Color.fromRGB(data.colorR, data.colorG, data.colorB));
        meta.setDisplayName(data.name);

        org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "potion_uuid");
        meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, data.potionUuid);

        List<String> lore = new ArrayList<>();
        lore.add("§8[ Phân loại: Dược Phẩm ]");
        lore.add("");

        List<String> bottomTags = new ArrayList<>();

        if (data.specialEffects.contains("THERMAL_FLASK")) {
            bottomTags.add("§b[Đựng trong Bình Sứ]");
        }

        if (data.isRuined) {
            lore.add("§7Mùi vị kinh khủng, dung môi phản ứng tệ...");
            bottomTags.add("§c[Kinh tởm]");
        } else {
            if (data.isEnhanced && !data.name.contains("Cường Hóa") && !data.name.contains("Siêu Cấp")) {
                bottomTags.add("§d[Cường hóa]");
            }
            if (data.toxicityPoints < 2.0 && !data.customStats.isEmpty() && data.durationMinutes >= 15) {
                bottomTags.add("§a[Hoàn hảo]");
            }

            lore.add("§f§lThông tin chung:");

            boolean hasDuration = !data.potionEffects.isEmpty();
            boolean hasInstant = !data.specialEffects.isEmpty();
            for (String stat : data.customStats.keySet()) {
                if (!stat.contains("tức thì")) hasDuration = true;
                else hasInstant = true;
            }

            if (hasDuration && hasInstant) lore.add("§7 - Hiệu lực: §a" + data.durationMinutes + " Phút §8(& §6Tức thì§8)");
            else if (hasDuration) lore.add("§7 - Hiệu lực: §a" + data.durationMinutes + " Phút");
            else lore.add("§7 - Hiệu lực: §6Tức thì");

            if (data.toxicityPoints > 0) lore.add("§7 - Độc tính (Sốc): §c" + String.format("%.1f", data.toxicityPoints) + " Điểm");
            lore.add("");

            if (!data.customStats.isEmpty() || !data.specialEffects.isEmpty()) {
                lore.add("§f§lThuộc Tính Dược Liệu:");
                for (String spec : data.specialEffects) {
                    if (spec.equals("THERMAL_FLASK")) continue;
                    if (spec.equals("IMMORTAL")) lore.add("§f ➥ §eMiễn nhiễm tử vong trong 10s");
                    if (spec.equals("CLEANSE")) lore.add("§f ➥ §bXóa bỏ toàn bộ trạng thái xấu");
                    if (spec.equals("EXECUTE_POISON")) lore.add("§f ➥ §5Độc Kết Liễu < 5% HP");
                    if (spec.equals("RESURRECTION")) lore.add("§f ➥ §dHồi sinh tại chỗ khi chết");
                }

                for (String stat : data.customStats.keySet()) {
                    String cleanStat = stat.replace(" (%)", "").replace("Tỉ lệ", "Tỉ lệ").trim();
                    boolean isFlat = cleanStat.equalsIgnoreCase("Máu tối đa");
                    String sign = isFlat ? "" : "%";
                    lore.add("§f- §7" + cleanStat + ": §a+" + String.format("%.1f", data.customStats.get(stat)) + sign);
                }
            }

            if (!data.potionEffects.isEmpty()) {
                lore.add("§f§lHiệu Ứng Thể Chất:");
                Map<String, String> peNames = new HashMap<>();
                peNames.put("WATER_BREATHING", "Thở Dưới Nước"); peNames.put("JUMP", "Nhảy Cao");
                peNames.put("FIRE_RESISTANCE", "Kháng Lửa"); peNames.put("INVISIBILITY", "Tàng Hình");
                peNames.put("SPEED", "Tốc Độ"); peNames.put("SLOW", "Làm Chậm");
                peNames.put("FAST_DIGGING", "Nhanh Nhẹn"); peNames.put("SLOW_DIGGING", "Mệt Mỏi");
                peNames.put("INCREASE_DAMAGE", "Sức Mạnh"); peNames.put("HEAL", "Hồi Máu");
                peNames.put("HARM", "Sát Thương"); peNames.put("CONFUSION", "Buồn Nôn");
                peNames.put("REGENERATION", "Hồi Sinh Lực"); peNames.put("DAMAGE_RESISTANCE", "Kháng Cự");
                peNames.put("BLINDNESS", "Mù Lòa"); peNames.put("NIGHT_VISION", "Nhìn Trong Đêm");
                peNames.put("HUNGER", "Đói Khát"); peNames.put("WEAKNESS", "Yếu Ớt");
                peNames.put("POISON", "Trúng Độc"); peNames.put("WITHER", "Khô Héo");
                peNames.put("HEALTH_BOOST", "Tăng Máu"); peNames.put("ABSORPTION", "Hấp Thụ");
                peNames.put("SATURATION", "No Nê"); peNames.put("GLOWING", "Phát Sáng");
                peNames.put("LEVITATION", "Lơ Lửng"); peNames.put("LUCK", "May Mắn");
                peNames.put("UNLUCK", "Xui Xẻo"); peNames.put("SLOW_FALLING", "Rơi Chậm");
                peNames.put("CONDUIT_POWER", "Sức Mạnh Vỏ Ốc"); peNames.put("DOLPHINS_GRACE", "Duyên Dáng");
                peNames.put("BAD_OMEN", "Điềm Gở"); peNames.put("HERO_OF_THE_VILLAGE", "Anh Hùng Làng");

                for (String pe : data.potionEffects) {
                    String[] parts = pe.split(":");
                    String name = peNames.getOrDefault(parts[0], parts[0]);
                    String levelText = "";
                    if (parts.length > 1) {
                        int lvl = Integer.parseInt(parts[1]);
                        if (lvl > 1) levelText = " " + getRomanNumeral(lvl);
                    }
                    lore.add("§f- §bNhận hiệu ứng " + name + levelText);
                }
            }
        }

        // ==========================================
        // [FIX CHÍNH THỨC]: GỘP TAGS KHÔNG CÁCH DÒNG
        // ==========================================
        List<String> cleanTags = new ArrayList<>();
        for (String t : data.flavorTags) {
            if (t != null && !t.trim().isEmpty()) {
                cleanTags.add(t.trim()); // Dọn dẹp các khoảng trắng bị thừa/ẩn
            }
        }

        if (!cleanTags.isEmpty() || !bottomTags.isEmpty()) {
            lore.add(""); // Tạo duy nhất 1 dòng trống để tách phần Tags với phần Chỉ Số

            if (!cleanTags.isEmpty()) {
                lore.addAll(cleanTags); // Add [Cân bằng hoàn hảo]
            }

            if (!bottomTags.isEmpty()) {
                lore.add(String.join(" ", bottomTags)); // Add [Cường hóa] ngay sát bên dưới
            }
        }

        meta.setLore(lore);

        if (!data.isRuined && (data.isEnhanced || !data.specialEffects.isEmpty() || data.name.contains("§6") || data.name.contains("§c") || data.name.contains("§d"))) {
            org.bukkit.enchantments.Enchantment unbreaking = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (unbreaking != null) meta.addEnchant(unbreaking, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public void savePotionToFile(PotionData data) {
        // [Giữ nguyên code save cũ]
        File file = new File(folder, data.potionUuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", data.name); config.set("durationMinutes", data.durationMinutes);
        config.set("toxicityPoints", data.toxicityPoints); config.set("isRuined", data.isRuined);
        config.set("isManaPotion", data.isManaPotion); config.set("isEnhanced", data.isEnhanced);
        config.set("colorR", data.colorR); config.set("colorG", data.colorG); config.set("colorB", data.colorB);
        config.set("flavorTags", data.flavorTags); config.set("specialEffects", data.specialEffects); config.set("potionEffects", data.potionEffects);
        for (String stat : data.customStats.keySet()) config.set("stats." + stat, data.customStats.get(stat));
        try { config.save(file); } catch (IOException ignored) {}
    }

    public PotionData loadPotionFromFile(String uuid) {
        // [Giữ nguyên code load cũ]
        File file = new File(folder, uuid + ".yml");
        if (!file.exists()) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PotionData data = new PotionData(uuid);
        data.name = config.getString("name"); data.durationMinutes = config.getInt("durationMinutes");
        data.toxicityPoints = config.getDouble("toxicityPoints"); data.isRuined = config.getBoolean("isRuined");
        data.isManaPotion = config.getBoolean("isManaPotion"); data.isEnhanced = config.getBoolean("isEnhanced");
        data.colorR = config.getInt("colorR", 50); data.colorG = config.getInt("colorG", 50); data.colorB = config.getInt("colorB", 200);
        data.flavorTags = config.getStringList("flavorTags"); data.specialEffects = config.getStringList("specialEffects"); data.potionEffects = config.getStringList("potionEffects");
        if (config.contains("stats")) {
            for (String key : config.getConfigurationSection("stats").getKeys(false)) data.customStats.put(key, config.getDouble("stats." + key));
        }
        return data;
    }

    public void deletePotionFile(String uuid) { File file = new File(folder, uuid + ".yml"); if (file.exists()) file.delete(); }

    // ==========================================
    // HÀM HELPER: DỊCH SỐ SANG CHỮ LA MÃ
    // ==========================================
    private String getRomanNumeral(int level) {
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (level > 0 && level <= 10) return romans[level];
        return String.valueOf(level);
    }
}