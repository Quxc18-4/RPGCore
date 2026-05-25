package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.io.File;
import java.io.IOException;

public class HerbHarvestListener implements Listener {

    private final ItemManager itemManager;
    private final File configFile;
    private YamlConfiguration config;

    public HerbHarvestListener(ItemManager itemManager) {
        this.itemManager = itemManager;

        // Đặt file chung tại thư mục dữ liệu của RPGCore để cả 2 plugin dễ đọc/ghi
        this.configFile = new File(RPGCore.getInstance().getDataFolder(), "herb_locations.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException ignored) {}
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHerbBreak(org.bukkit.event.block.BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        org.bukkit.Location loc = block.getLocation();

        String locKey = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        loadConfig();

        if (config.contains(locKey)) {
            String herbId = config.getString(locKey);
            if (herbId == null) return;

            event.setDropItems(false);
            Player player = event.getPlayer();

            ItemStack herbItem = itemManager.buildItem(herbId, 1);
            if (herbItem != null) {
                loc.getWorld().dropItemNaturally(loc.add(0.5, 0.2, 0.5), herbItem);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0f, 1.2f);

                // =========================================================
                // ĐOẠN CODE BẮN EVENT NẰM Ở ĐÂY:
                // =========================================================
                vn.minecraft.rPGCore.managers.ItemManager.ItemData herbData = itemManager.getItemData(herbId);
                if (herbData != null) {
                    // Tạo thực thể Event và nạp sẵn dữ liệu người hái, ID thảo mộc, độ hiếm...
                    vn.minecraft.rPGCore.api.events.HerbHarvestEvent harvestEvent =
                            new vn.minecraft.rPGCore.api.events.HerbHarvestEvent(player, herbId, herbData, herbItem);

                    // Phát tín hiệu Event ra toàn server để các plugin nhiệm vụ bắt lấy
                    org.bukkit.Bukkit.getPluginManager().callEvent(harvestEvent);
                }
                // =========================================================
            }

            config.set(locKey, null);
            saveConfig();
        }
    }
}