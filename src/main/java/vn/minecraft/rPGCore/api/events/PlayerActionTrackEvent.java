package vn.minecraft.rPGCore.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Sự kiện tổng quát bắn ra từ RPGCore để RPGQuests (hoặc plugin khác) lắng nghe.
 * Chỉ đếm số lượng hành động, bỏ qua mọi logic phức tạp.
 */
public class PlayerActionTrackEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final String actionId; // Ví dụ: "COOK", "FISH", "BREW", "HARVEST", "CONSUME"
    private final int amount;

    public PlayerActionTrackEvent(Player player, String actionId, int amount) {
        this.player = player;
        this.actionId = actionId;
        this.amount = amount;
    }

    public Player getPlayer() { return player; }
    public String getActionId() { return actionId; }
    public int getAmount() { return amount; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}