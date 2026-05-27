package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.ItemManager;

public class WaterFetchListener implements Listener {
    private final ItemManager itemManager;

    public WaterFetchListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onFillBottle(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;

        boolean isVanillaBottle = item.getType() == Material.GLASS_BOTTLE;
        boolean isThermalFlask = "alch_thermal_flask_empty".equals(itemManager.getItemId(item));

        // Chỉ xử lý nếu cầm Chai Thủy Tinh hoặc Bình Sứ Cách Nhiệt Rỗng
        if ((isVanillaBottle || isThermalFlask) && event.getAction().isRightClick()) {

            Block targetBlock = player.getTargetBlockExact(5, org.bukkit.FluidCollisionMode.SOURCE_ONLY);

            if (targetBlock != null && targetBlock.getType() == Material.WATER) {
                event.setCancelled(true);

                Biome biome = targetBlock.getBiome();
                String biomeName = biome.getKey().getKey().toUpperCase();

                // Xác định ID nước dựa trên loại bình (Vanilla hoặc Bình Sứ)
                String waterId = isThermalFlask ? "alch_thermal_flask_tasteless" : "water_tasteless";

                if (biomeName.contains("OCEAN") || biomeName.contains("BEACH")) {
                    waterId = isThermalFlask ? "alch_thermal_flask_salty" : "water_salty";
                } else if (biomeName.contains("RIVER")) {
                    waterId = isThermalFlask ? "alch_thermal_flask_sweet" : "water_sweet";
                } else if (biomeName.contains("CAVE") || biomeName.contains("DEEP") || biomeName.contains("DRIPSTONE")) {
                    waterId = isThermalFlask ? "alch_thermal_flask_cave" : "water_cave";
                }

                item.setAmount(item.getAmount() - 1);
                ItemStack waterItem = itemManager.buildItem(waterId, 1);

                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), waterItem);
                } else {
                    player.getInventory().addItem(waterItem);
                }

                player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
                if (isThermalFlask) {
                    player.sendMessage("§b§l[!] §fBạn đã nạp đầy Nước vào Bình Sứ Cách Nhiệt.");
                }
            }
        }
    }
}