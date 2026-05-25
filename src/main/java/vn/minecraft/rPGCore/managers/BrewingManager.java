package vn.minecraft.rPGCore.managers;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // Bộ đếm chống Spam Click đúp mở 2 UI
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

    public BrewingManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.engine = new BrewingEngine();

        this.folder = new File(plugin.getDataFolder(), "potions");
        if (!folder.exists()) folder.mkdirs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBrewingStandClick(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        if (event.getAction().isRightClick() && event.hasBlock() && event.getClickedBlock().getType() == Material.BREWING_STAND) {
            event.setCancelled(true);
            Player player = event.getPlayer();

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
        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).updateInventory();
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
        inv.setItem(14, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§b§lNước Nền", "§7Đặt Nước hoặc Thuốc muốn Cường hóa vào ô dưới."));
        inv.setItem(16, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§c§lDung Môi Buff", "§7Bỏ chất xúc tác vào ô bên dưới để tăng tỉ lệ."));

        updateBrewButton(inv, player);
        player.openInventory(inv);
    }

    private ItemStack createPane(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            List<String> lore = new ArrayList<>();
            for (String l : loreLines) lore.add(l);
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void updateBrewButton(Inventory inv, Player player) {
        List<ItemData> ingredients = new ArrayList<>();
        boolean isEnhancing = false;

        ItemStack waterItem = inv.getItem(WATER_SLOT);
        if (waterItem != null && waterItem.getType() != Material.AIR) {
            String wId = itemManager.getItemId(waterItem);
            if (wId != null && wId.startsWith("water_")) ingredients.add(itemManager.getItemData(wId));
            else isEnhancing = true;
        }

        int[] allMats = {10, 11, 12, 19, 20, 21, 37, 38, 39, 25};
        for (int s : allMats) {
            ItemStack i = inv.getItem(s);
            if (i != null && i.getType() != Material.AIR) {
                String id = itemManager.getItemId(i);
                if (id != null) ingredients.add(itemManager.getItemData(id));
            }
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
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }
                inv.setItem(s, null);
            }
        }
    }

    @EventHandler
    public void onBrewingInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        Player player = (Player) event.getWhoClicked();

        // Chặn mọi hành vi Shift-Click / Phím số / Double-click để không làm lọt đồ
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                event.getClick().isKeyboardClick() ||
                event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            player.updateInventory(); // Ép refresh
            return;
        }

        int slot = event.getRawSlot();
        if (slot >= 54) {
            if (event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
                player.updateInventory();
            }
            return;
        }

        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY || event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType().name().endsWith("GLASS_PANE") && slot != BUTTON_SLOT) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        Inventory inv = event.getInventory();
        ItemStack cursor = event.getCursor();

        // VALIDATION ĐẦU VÀO TỪ CON TRỎ
        if (cursor != null && cursor.getType() != Material.AIR) {
            String cursorId = itemManager.getItemId(cursor);
            ItemData data = itemManager.getItemData(cursorId);

            if (slot == WATER_SLOT) {
                boolean isWater = cursorId != null && cursorId.startsWith("water_");
                boolean isCustomPotion = cursor.hasItemMeta() && cursor.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "potion_uuid"), PersistentDataType.STRING);

                if (!isWater && !isCustomPotion) {
                    event.setCancelled(true); player.sendMessage("§cChỉ được đặt Nước hoặc Thuốc cần cường hóa vào ô này!");
                    player.updateInventory();
                    return;
                }
                if (isCustomPotion) {
                    String uuid = cursor.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "potion_uuid"), PersistentDataType.STRING);
                    PotionData old = loadPotionFromFile(uuid);
                    if (old == null || old.isEnhanced || old.isRuined) {
                        event.setCancelled(true); player.sendMessage("§cThuốc này bị lỗi, đã hỏng hoặc đã được Cường Hóa!");
                        player.updateInventory();
                        return;
                    }
                }
            } else if (slot == 10 || slot == 11 || slot == 12 || slot == 19 || slot == 20 || slot == 21) {
                if (data == null || data.category != ItemManager.Category.HERB) {
                    event.setCancelled(true); player.sendMessage("§cKhu vực này chỉ dành cho Thảo Dược!");
                    player.updateInventory();
                    return;
                }
            } else if (slot == 37 || slot == 38 || slot == 39) {
                if (data == null || (data.category != ItemManager.Category.SPICE && data.category != ItemManager.Category.HERB)) {
                    event.setCancelled(true); player.sendMessage("§cKhu vực này chỉ dành cho Gia Vị và Điều Vị!");
                    return;
                }
            } else if (slot == SOLVENT_SLOT) {
                if (data == null) { event.setCancelled(true); player.sendMessage("§cVật phẩm không hợp lệ!"); return; }
            } else if (slot != BUTTON_SLOT && slot != OUTPUT_SLOT) {
                event.setCancelled(true);
                player.updateInventory();
                return;
            }
        }

        // Cập nhật GUI 1 Tick sau khi đặt đồ (ĐÃ XÓA PLAYER.UPDATEINVENTORY ĐỂ HẾT GHOST ITEM)
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateBrewButton(inv, player), 1L);

        // BẤM NÚT NẤU THUỐC
        if (slot == BUTTON_SLOT) {
            event.setCancelled(true);
            player.updateInventory();
            if (inv.getItem(OUTPUT_SLOT) != null && inv.getItem(OUTPUT_SLOT).getType() != Material.AIR) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§c§l[!] §cTúi đồ của bạn đã đầy! Không thể đẩy thuốc cũ ra, hãy dọn dẹp túi.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                player.getInventory().addItem(inv.getItem(OUTPUT_SLOT));
                inv.setItem(OUTPUT_SLOT, null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            }

            ItemStack waterItem = inv.getItem(WATER_SLOT);
            if (waterItem == null || waterItem.getType() == Material.AIR) {
                player.sendMessage("§c§l[!] §cKhông có Nước Nền thì vạc sẽ cháy đen mất!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            List<ItemData> ingredients = new ArrayList<>();
            PotionData basePotion = null;
            String oldUuidToDelete = null;

            String waterId = itemManager.getItemId(waterItem);
            if (waterId != null && waterId.startsWith("water_")) {
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

            for (int s : SPICE_SLOTS) if (inv.getItem(s) != null) ingredients.add(itemManager.getItemData(itemManager.getItemId(inv.getItem(s))));
            if (inv.getItem(SOLVENT_SLOT) != null) ingredients.add(itemManager.getItemData(itemManager.getItemId(inv.getItem(SOLVENT_SLOT))));

            PotionData resultData = engine.evaluate(ingredients, player, basePotion);
            savePotionToFile(resultData);

            if (oldUuidToDelete != null) deletePotionFile(oldUuidToDelete);

            waterItem.setAmount(waterItem.getAmount() - 1);
            for (int s : HERB_SLOTS) if (inv.getItem(s) != null) inv.getItem(s).setAmount(inv.getItem(s).getAmount() - 1);
            for (int s : SPICE_SLOTS) if (inv.getItem(s) != null) inv.getItem(s).setAmount(inv.getItem(s).getAmount() - 1);
            if (inv.getItem(SOLVENT_SLOT) != null) inv.getItem(SOLVENT_SLOT).setAmount(inv.getItem(SOLVENT_SLOT).getAmount() - 1);

            if (resultData.isRuined) {
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
                player.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_SIGNAL_SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0);
            }

            inv.setItem(OUTPUT_SLOT, buildPotionItem(resultData));
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
        lore.add("§8[ Phân loại: Vật phẩm tiêu hao - Dược Phẩm ]");
        lore.add("");

        if (data.isRuined) {
            lore.add("§cMùi vị kinh khủng! Uống vào chắc chắn sẽ bị nôn mửa...");
        } else {
            if (data.isEnhanced) lore.add("§d✧ Đã Cường Hóa ✧");
            lore.add("§f§lThông tin chung:");

            // [SỬA]: Xử lý chuẩn logic hiển thị Tức thì & Thời gian hỗn hợp
            boolean hasDuration = !data.potionEffects.isEmpty();
            boolean hasInstant = !data.specialEffects.isEmpty();
            for (String stat : data.customStats.keySet()) {
                if (!stat.contains("tức thì")) hasDuration = true;
                else hasInstant = true;
            }

            if (hasDuration && hasInstant) {
                lore.add("§7 - Hiệu lực: §a" + data.durationMinutes + " Phút §8(& §6Tức thì§8)");
            } else if (hasDuration) {
                lore.add("§7 - Hiệu lực: §a" + data.durationMinutes + " Phút");
            } else {
                lore.add("§7 - Hiệu lực: §6Tức thì");
            }

            if (data.toxicityPoints > 0) lore.add("§7 - Độc tính (Gây Sốc): §c" + String.format("%.1f", data.toxicityPoints) + " Điểm");
            lore.add("");

            if (!data.customStats.isEmpty() || !data.specialEffects.isEmpty()) {
                lore.add("§f§lThuộc Tính Dược Liệu:");
                for (String spec : data.specialEffects) {
                    if (spec.equals("IMMORTAL")) lore.add("§f ➥ §eMiễn nhiễm tử vong trong 10s");
                    if (spec.equals("CLEANSE")) lore.add("§f ➥ §bXóa bỏ toàn bộ trạng thái xấu");
                    if (spec.equals("EXECUTE_POISON")) lore.add("§f ➥ §5Độc Kết Liễu < 5% HP");
                    if (spec.equals("RESURRECTION")) lore.add("§f ➥ §dHồi sinh tại chỗ khi chết");
                }

                for (String stat : data.customStats.keySet()) {
                    String cleanStat = stat.replace(" (%)", "");
                    boolean isPercent = stat.contains("%") || stat.contains("Tỉ lệ");
                    lore.add("§f- §7" + cleanStat + ": §a+" + String.format("%.1f", data.customStats.get(stat)) + (isPercent ? "%" : ""));
                }
            }
            if (!data.potionEffects.isEmpty()) {
                lore.add("§f§lHiệu Ứng Thể Chất:");
                Map<String, String> peNames = Map.of(
                        "WATER_BREATHING", "Thở dưới nước",
                        "JUMP",            "Nhảy cao",
                        "FIRE_RESISTANCE", "Kháng lửa",
                        "INVISIBILITY",    "Tàng Hình"
                );
                for (String pe : data.potionEffects) {
                    String name = peNames.getOrDefault(pe, pe);
                    lore.add("§f- §bNhận hiệu ứng " + name);
                }
            }
        }
        if (!data.flavorTags.isEmpty()) { lore.add(""); lore.addAll(data.flavorTags); }

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
        File file = new File(folder, data.potionUuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", data.name);
        config.set("durationMinutes", data.durationMinutes);
        config.set("toxicityPoints", data.toxicityPoints);
        config.set("isRuined", data.isRuined);
        config.set("isManaPotion", data.isManaPotion);
        config.set("isEnhanced", data.isEnhanced);
        config.set("colorR", data.colorR); config.set("colorG", data.colorG); config.set("colorB", data.colorB);
        config.set("flavorTags", data.flavorTags);
        config.set("specialEffects", data.specialEffects);
        config.set("potionEffects", data.potionEffects);
        for (String stat : data.customStats.keySet()) config.set("stats." + stat, data.customStats.get(stat));
        try { config.save(file); } catch (IOException ignored) {}
    }

    public PotionData loadPotionFromFile(String uuid) {
        File file = new File(folder, uuid + ".yml");
        if (!file.exists()) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PotionData data = new PotionData(uuid);
        data.name = config.getString("name");
        data.durationMinutes = config.getInt("durationMinutes");
        data.toxicityPoints = config.getDouble("toxicityPoints");
        data.isRuined = config.getBoolean("isRuined");
        data.isManaPotion = config.getBoolean("isManaPotion");
        data.isEnhanced = config.getBoolean("isEnhanced");
        data.colorR = config.getInt("colorR", 50); data.colorG = config.getInt("colorG", 50); data.colorB = config.getInt("colorB", 200);
        data.flavorTags = config.getStringList("flavorTags");
        data.specialEffects = config.getStringList("specialEffects");
        data.potionEffects = config.getStringList("potionEffects");
        if (config.contains("stats")) {
            for (String key : config.getConfigurationSection("stats").getKeys(false)) {
                data.customStats.put(key, config.getDouble("stats." + key));
            }
        }
        return data;
    }

    public void deletePotionFile(String uuid) {
        File file = new File(folder, uuid + ".yml");
        if (file.exists()) file.delete();
    }
}