package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.ItemManager;

public class AlchemistToolListener implements Listener {

    private final ItemManager itemManager;

    public AlchemistToolListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onFlaskFill(PlayerInteractEvent event) {
        // Kiểm tra xem có phải hành động click chuột phải không
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        String id = itemManager.getItemId(item);
        if ("alch_thermal_flask_empty".equals(id)) {
            // Lấy block nước người chơi đang nhìn thẳng vào (FluidCollisionMode để bắt được nước)
            org.bukkit.block.Block target = event.getPlayer().getTargetBlockExact(5, org.bukkit.FluidCollisionMode.SOURCE_ONLY);

            if (target != null && target.getType() == Material.WATER) {
                event.setCancelled(true);

                // Trừ đi 1 bình rỗng và trả lại 1 bình đầy nước
                item.setAmount(item.getAmount() - 1);
                ItemStack filledFlask = itemManager.buildItem("alch_thermal_flask_water", 1);

                if (filledFlask != null) {
                    Player player = event.getPlayer();
                    player.getInventory().addItem(filledFlask);
                    player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
                    player.sendMessage("§b§l[!] §fBạn đã nạp đầy Nước vào Bình Sứ Cách Nhiệt.");
                }
            }
        }
    }
}