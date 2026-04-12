package vn.minecraft.rPGCore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ConsumableListener implements Listener {

    private final ItemManager itemManager;

    public ConsumableListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String itemId = itemManager.getItemId(item);
        if (itemId == null) return;

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        boolean consumed = false;

        // 1. Logic Bình Máu
        if (itemId.equals("potion_hp_1")) {
            double healAmount = rpgPlayer.getTotalMaxHealth() * 0.10; // Hồi 10%
            rpgPlayer.heal(healAmount);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            consumed = true;
        }

        // 2. Logic Tinh Thể Nguyên Tố
        else if (itemId.startsWith("crystal_")) {
            if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("mage")) {
                player.sendMessage("§cChỉ Pháp Sư mới có thể hấp thụ Tinh Thể Nguyên Tố!");
                return;
            }

            String elementToUnlock = itemId.replace("crystal_", "").toUpperCase();

            if (rpgPlayer.hasElement(elementToUnlock)) {
                player.sendMessage("§eBạn đã sở hữu Nguyên Tố này rồi!");
                return;
            }

            rpgPlayer.addElement(elementToUnlock);
            player.sendMessage("§a§l[!] §fBạn đã hấp thụ thành công §dNguyên Tố " + elementToUnlock + "§f!");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            consumed = true;
        }

        // 3. Trừ Item sau khi dùng thành công
        if (consumed) {
            event.setCancelled(true); // Cản sự kiện đặt block (nếu item là block)
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}