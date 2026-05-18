package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CropProtectionListener implements Listener {

    @EventHandler
    public void onCropProtect(PlayerInteractEvent event) {
        // 1. Chặn giẫm đạp làm hỏng đất cày (Farmland)
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
            return;
        }

        // 2. Chặn Vanilla hái quả mọng/dây leo bằng tay không
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Material type = block.getType();
                if (type == Material.SWEET_BERRY_BUSH || type == Material.CAVE_VINES || type == Material.CAVE_VINES_PLANT) {
                    event.setCancelled(true);
                }
            }
        }
    }
}