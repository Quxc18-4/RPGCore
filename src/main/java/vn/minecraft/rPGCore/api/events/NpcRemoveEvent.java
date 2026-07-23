package vn.minecraft.rPGCore.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import vn.minecraft.rPGCore.managers.NPCManager.ShopNPC;

public class NpcRemoveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final ShopNPC npc;

    public NpcRemoveEvent(Player player, ShopNPC npc) {
        this.player = player;
        this.npc = npc;
    }

    public Player getPlayer() { return player; }
    public ShopNPC getNpc() { return npc; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}