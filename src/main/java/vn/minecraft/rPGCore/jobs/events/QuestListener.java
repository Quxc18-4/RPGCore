package vn.minecraft.rPGCore.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobQuestManager;
import vn.minecraft.rPGCore.jobs.quests.QuestAction;

public class QuestListener implements Listener {

    private final NamespacedKey dropKey;

    public QuestListener() {
        this.dropKey = new NamespacedKey(RPGCore.getInstance(), "player_dropped_item");
    }

    // 1. Khi người chơi vứt đồ ra -> Đóng dấu "Đồ rác"
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(dropKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
            event.getItemDrop().setItemStack(item);
        }
    }

    // 2. Khi người chơi lụm đồ -> Check dấu
    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack pickedItem = event.getItem().getItemStack();

        if (pickedItem.hasItemMeta() && pickedItem.getItemMeta().getPersistentDataContainer().has(dropKey, PersistentDataType.BYTE)) {
            // Đồ này là do người chơi vứt ra -> KHÔNG TÍNH VÀO NHIỆM VỤ
            return;
        }

        // Nếu là đồ rớt từ Mobs, Block, Câu cá... -> Tính vào nhiệm vụ nhặt đồ!
        JobQuestManager.handleAction(player, QuestAction.GATHER_ITEM, pickedItem.getType().name());
    }
}