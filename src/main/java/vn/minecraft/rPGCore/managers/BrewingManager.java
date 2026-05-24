package vn.minecraft.rPGCore.managers;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.brewing.BrewingEngine;
import vn.minecraft.rPGCore.brewing.PotionData;
import vn.minecraft.rPGCore.managers.ItemManager.ItemData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BrewingManager implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final BrewingEngine engine;
    private final File folder;

    public static final String GUI_NAME = "§5§lVạc Pha Chế Thần Bí";

    // [FIXED] Tọa độ chuẩn 100% theo bản đồ màu mới nhất
    private final int[] HERB_SLOTS = {10, 11, 12, 19, 20, 21};
    private final int[] SPICE_SLOTS = {28, 29, 30};
    private final int WATER_SLOT = 14;
    private final int SOLVENT_SLOT = 16;
    private final int BUTTON_SLOT = 32;
    private final int OUTPUT_SLOT = 34;

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
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BREWING_STAND) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
            openBrewingGUI(player);
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

        // Gợi ý chuẩn theo cột
        inv.setItem(9, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§e§lThảo Dược Chính", "§7Đặt tối đa 6 loại thảo dược vào các ô bên phải."));
        inv.setItem(27, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§d§lChất Điều Vị", "§7Đặt tối đa 3 loại gia vị/thảo mộc khử vị."));
        inv.setItem(23, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§b§lNước Nền", "§7Bắt buộc: 1 Bình Nước ở ô bên trên."));
        inv.setItem(25, createPane(Material.YELLOW_STAINED_GLASS_PANE, "§c§lDung Môi Buff", "§7Bỏ chất xúc tác vào ô bên trên để tăng tỉ lệ."));

        ItemStack brewBtn = createPane(Material.PURPLE_STAINED_GLASS_PANE, "§d§l♨ NẤU THUỐC ♨", "§fClick vào đây để luyện dược!");
        inv.setItem(BUTTON_SLOT, brewBtn);

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

    @EventHandler
    public void onBrewingInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        ItemStack clicked = event.getCurrentItem();

        if (clicked != null && clicked.getType().name().endsWith("GLASS_PANE") && slot != BUTTON_SLOT) {
            event.setCancelled(true);
            return;
        }

        if (slot == BUTTON_SLOT) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            Inventory inv = event.getInventory();

            if (inv.getItem(OUTPUT_SLOT) != null) {
                player.sendMessage("§c§l[!] §cVui lòng lấy bình thuốc thành phẩm ra trước khi nấu mẻ mới!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ItemStack waterItem = inv.getItem(WATER_SLOT);
            if (waterItem == null || waterItem.getType() == Material.AIR) {
                player.sendMessage("§c§l[!] §cKhông có nước thì vạc sẽ cháy đen mất! Hãy cho 1 lọ nước vào ô Xanh Dương.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            List<ItemData> ingredients = new ArrayList<>();

            String waterId = itemManager.getItemId(waterItem);
            if (waterId != null) ingredients.add(itemManager.getItemData(waterId));

            boolean hasHerb = false;
            for (int s : HERB_SLOTS) {
                ItemStack item = inv.getItem(s);
                if (item != null && item.getType() != Material.AIR) {
                    String id = itemManager.getItemId(item);
                    if (id != null) {
                        ItemData data = itemManager.getItemData(id);
                        if (data.category == ItemManager.Category.HERB) {
                            ingredients.add(data);
                            hasHerb = true;
                        } else {
                            player.sendMessage("§c§l[!] §cKhu vực Xanh Lá chỉ dành cho Thảo Dược!");
                            return;
                        }
                    }
                }
            }

            if (!hasHerb) {
                player.sendMessage("§c§l[!] §cVui lòng bỏ ít nhất 1 Thảo dược vào ô Xanh Lá!");
                return;
            }

            int[] extraSlots = new int[]{28, 29, 30, 16};
            for (int s : extraSlots) {
                ItemStack item = inv.getItem(s);
                if (item != null && item.getType() != Material.AIR) {
                    String id = itemManager.getItemId(item);
                    if (id != null) ingredients.add(itemManager.getItemData(id));
                }
            }

            PotionData resultData = engine.evaluate(ingredients, player);
            savePotionToFile(resultData);

            if (waterItem != null) waterItem.setAmount(waterItem.getAmount() - 1);
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
        }
    }

    private ItemStack buildPotionItem(PotionData data) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(data.name);

        org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "potion_uuid");
        meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, data.potionUuid);

        List<String> lore = new ArrayList<>();
        lore.add("§8[ Phân loại: Vật phẩm tiêu hao - Dược Phẩm ]");
        lore.add("");

        if (data.isRuined) {
            lore.add("§cMùi vị kinh khủng! Uống vào chắc chắn sẽ bị nôn mửa...");
        } else {
            lore.add("§f§lThông tin chung:");
            lore.add("§7 - Hiệu lực: §a" + (data.durationMinutes > 0 ? data.durationMinutes + " Phút" : "§6Tức thì"));

            if (data.toxicityPoints > 0) {
                lore.add("§7 - Độc tính (Gây Sốc): §c" + String.format("%.1f", data.toxicityPoints) + " Điểm");
            }
            lore.add("");

            if (!data.customStats.isEmpty() || !data.specialEffects.isEmpty()) {
                lore.add("§f§lThuộc Tính Dược Liệu:");
                for (String spec : data.specialEffects) {
                    if (spec.equals("BẤT_TỬ")) lore.add("§f ➥ §eMiễn nhiễm tử vong trong 10s");
                    if (spec.equals("THANH_TẨY")) lore.add("§f ➥ §bXóa bỏ toàn bộ trạng thái xấu");
                    if (spec.equals("EXECUTE_POISON")) lore.add("§f ➥ §5Độc Kết Liễu < 5% HP");
                    if (spec.equals("RESURRECTION")) lore.add("§f ➥ §dHồi sinh tại chỗ khi chết");
                }

                for (String stat : data.customStats.keySet()) {
                    lore.add("§f- §7" + stat + ": §a+" + String.format("%.1f", data.customStats.get(stat)) + (stat.contains("%") || stat.contains("Tỉ lệ") ? "%" : ""));
                }
            }
            if (!data.potionEffects.isEmpty()) {
                lore.add("§f§lHiệu Ứng Thể Chất:");
                for (String pe : data.potionEffects) lore.add("§f- §bNhận hiệu ứng " + pe);
            }
        }
        if (!data.flavorTags.isEmpty()) { lore.add(""); lore.addAll(data.flavorTags); }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
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