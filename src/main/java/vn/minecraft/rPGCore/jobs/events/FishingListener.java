package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.FishingManager;
import vn.minecraft.rPGCore.managers.ItemManager;

public class FishingListener implements Listener {

    private final FishingManager fishingManager;
    private final ItemManager itemManager;

    public FishingListener(FishingManager fishingManager, ItemManager itemManager) {
        this.fishingManager = fishingManager;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        if (fishingManager.isInMinigame(player)) {
            event.setCancelled(true);
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String rodId = itemManager.getItemId(mainHand);
        if (rodId == null || !rodId.startsWith("tool_fishing_rod_")) return;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        String baitId = itemManager.getItemId(offHand);

        // 1. ÉP THỜI GIAN CÁ CẮN CÂU (SIÊU NHANH)
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            org.bukkit.entity.FishHook hook = event.getHook();
            hook.setApplyLure(false);

            int minWait = 100;
            int maxWait = 300;

            if (baitId != null && offHand.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = offHand.getItemMeta().getPersistentDataContainer();

                if (pdc.has(fishingManager.KEY_INSTANT, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    hook.setMinWaitTime(1);
                    hook.setMaxWaitTime(2);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 5);
                    return;
                }

                double reduction = 0.0;
                String currentBiome = fishingManager.getCurrentBiomeType(hook.getLocation());

                reduction += pdc.getOrDefault(fishingManager.getKey("bait_bite_time", "ALL"), org.bukkit.persistence.PersistentDataType.DOUBLE, 0.0);
                reduction += pdc.getOrDefault(fishingManager.getKey("bait_bite_time", currentBiome), org.bukkit.persistence.PersistentDataType.DOUBLE, 0.0);

                if (reduction > 0) {
                    double multi = Math.max(0.1, 1.0 - (reduction / 100.0));
                    minWait = (int) (minWait * multi);
                    maxWait = (int) (maxWait * multi);
                }
            }
            hook.setMinWaitTime(minWait);
            hook.setMaxWaitTime(maxWait);
            return;
        }

        // 2. KHI CÁ CẮN CÂU -> BẬT MINIGAME (VÀ GIỮ PHAO)
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Hủy sự kiện mặc định để không kéo cá Vanilla lên
            event.setCancelled(true);

            // Xóa điểm kinh nghiệm rơi ra từ Vanilla
            event.setExpToDrop(0);

            // LƯU Ý QUAN TRỌNG: Chúng ta KHÔNG remove event.getHook() ở đây nữa.
            // Điều này sẽ giữ cái phao câu nổi lềnh bềnh trên mặt nước.

            ItemStack baitToUse = null;
            if (baitId != null && baitId.startsWith("bait_")) {
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    offHand.setAmount(offHand.getAmount() - 1);
                }
                baitToUse = offHand.clone();
            }

            org.bukkit.Location hookLoc = event.getHook().getLocation();

            // Truyền event.getHook() vào FishingManager để quản lý việc gỡ phao sau này
            fishingManager.startMinigame(player, rodId, baitToUse, hookLoc, event.getHook());
        }
    }

    // Các sự kiện tương tác khác giữ nguyên
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (fishingManager.isInMinigame(player)) {
            Action a = event.getAction();
            if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                fishingManager.handleInteract(player, false);
            } else if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                fishingManager.handleInteract(player, true);
            }
        }
    }

    @EventHandler
    public void onSwing(org.bukkit.event.player.PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (fishingManager.isInMinigame(player)) {
            fishingManager.handleInteract(player, true);
        }
    }

    @EventHandler public void onItemSwitch(PlayerItemHeldEvent event) { fishingManager.cancelMinigame(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { fishingManager.cancelMinigame(event.getPlayer()); }
}