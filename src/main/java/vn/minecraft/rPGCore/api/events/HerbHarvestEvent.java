package vn.minecraft.rPGCore.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.ItemManager.ItemData;

public class HerbHarvestEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String herbId;
    private final ItemData herbData;
    private final ItemStack itemStack;

    public HerbHarvestEvent(Player player, String herbId, ItemData herbData, ItemStack itemStack) {
        this.player = player;
        this.herbId = herbId;
        this.herbData = herbData;
        this.itemStack = itemStack;
    }

    public Player getPlayer() { return player; }
    public String getHerbId() { return herbId; }
    public ItemData getHerbData() { return herbData; }
    public ItemStack getItemStack() { return itemStack; }

    // Lấy nhanh độ hiếm phục vụ điều kiện nhiệm vụ (COMMON, UNCOMMON, RARE, EPIC, MYTHIC, LEGENDARY)
    public String getRarityName() {
        return herbData.rarity != null ? herbData.rarity.name() : "COMMON";
    }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}