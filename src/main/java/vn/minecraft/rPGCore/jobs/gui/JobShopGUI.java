package vn.minecraft.rPGCore.jobs.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.RPGJob;

public class JobShopGUI implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        RPGJob currentJob = JobManager.getPlayerJob(player);
        if (currentJob == null) return;

        // Nếu Tiêu đề GUI khớp với Tiêu đề Shop của Nghề hiện tại -> Đá event về cho Nghề xử lý
        if (event.getView().getTitle().equals(currentJob.getShopGUITitle())) {
            event.setCancelled(true);
            currentJob.handleShopClick(event);
        }
    }
}