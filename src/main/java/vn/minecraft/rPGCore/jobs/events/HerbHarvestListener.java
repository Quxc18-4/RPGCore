package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HerbHarvestListener implements Listener {

    private final ItemManager itemManager;
    private final File configFile;
    private YamlConfiguration config;

    public HerbHarvestListener(ItemManager itemManager) {
        this.itemManager = itemManager;
        this.configFile = new File(RPGCore.getInstance().getDataFolder(), "herb_locations.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            try { configFile.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfig() {
        try { config.save(configFile); } catch (IOException ignored) {}
    }

    // --- HÀM HELPER: TRỪ ĐỘ BỀN NBT ĐẠO CỤ ---
    private void reduceToolDurability(Player player, ItemStack item, int maxUses) {
        ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "alch_uses");
        int uses = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
        uses++;

        if (uses >= maxUses) {
            item.setAmount(0); // Phá hủy vật phẩm
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage("§c§l[!] §cMột đạo cụ Dược Sư của bạn đã vỡ nát do sử dụng quá giới hạn!");
        } else {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, uses);
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.removeIf(l -> l.contains("Độ bền:"));
            lore.add("§fĐộ bền: §a" + (maxUses - uses) + "§8/§a" + maxUses);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHerbBreak(BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        Location loc = block.getLocation();

        String locKey = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        loadConfig();

        if (config.contains(locKey)) {
            String herbId = config.getString(locKey);
            if (herbId == null) return;

            event.setDropItems(false);
            Player player = event.getPlayer();

            ItemStack herbItem = itemManager.buildItem(herbId, 1);
            if (herbItem != null) {
                int dropAmount = 1;

                // [NHÚNG LOGIC DƯỢC SƯ]: Kính lúp, Tinh chất và Nội tại thu hoạch
                vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                if (jobData != null && jobData.getJobId().equals("ALCHEMIST")) {
                    int level = jobData.getLevel("ALCHEMIST");

                    // Xử lý Nội tại 4 (Thu hoạch đại trà) & Ultimate 3
                    double bonusChance = level * 0.6;
                    boolean hasUlt3 = vn.minecraft.rPGCore.jobs.types.AlchemistJob.hasUltimate(player, "ALCH_ULT_3");
                    if (hasUlt3) bonusChance += 50.0;

                    if (Math.random() * 100.0 <= bonusChance) {
                        dropAmount++;
                        if (hasUlt3 && Math.random() * 100.0 <= 15.0) dropAmount++; // Bạo kích x3
                    }

                    // Tỉ lệ rớt Tinh Chất Ma Lực
                    vn.minecraft.rPGCore.jobs.core.JobManager.tryDropManaEssence(player, 5.0); // 5% gốc

                    // Quét Kính Lúp trong túi đồ để trừ độ bền (50 lần)
                    ItemStack glass = null;
                    for (ItemStack i : player.getInventory().getContents()) {
                        if (i != null && "alch_magnifying_glass".equals(itemManager.getItemId(i))) {
                            glass = i; break;
                        }
                    }
                    if (glass != null) {
                        reduceToolDurability(player, glass, 50);
                    }
                }

                herbItem.setAmount(dropAmount);
                loc.getWorld().dropItemNaturally(loc.add(0.5, 0.2, 0.5), herbItem);
                player.playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0f, 1.2f);

                ItemManager.ItemData herbData = itemManager.getItemData(herbId);
                if (herbData != null) {
                    vn.minecraft.rPGCore.api.events.HerbHarvestEvent harvestEvent =
                            new vn.minecraft.rPGCore.api.events.HerbHarvestEvent(player, herbId, herbData, herbItem);
                    org.bukkit.Bukkit.getPluginManager().callEvent(harvestEvent);

                    // [BỔ SUNG CHỖ NÀY]
                    vn.minecraft.rPGCore.jobs.core.JobManager.processAlchemistHarvestExp(player, harvestEvent.getRarityName());
                }
            }

            config.set(locKey, null);
            saveConfig();
        }
    }
}