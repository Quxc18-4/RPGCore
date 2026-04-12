package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.player.RPGPlayer;

public interface IMageBasicAttack {
    void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon);
}