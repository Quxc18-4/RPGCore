package vn.minecraft.rPGCore.skills.mage.focus;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.player.RPGPlayer;

public interface IFocusAttack {
    void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel);
}