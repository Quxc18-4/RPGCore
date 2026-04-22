package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

public interface IBlink {
    void executeBlink(Player player, RPGPlayer rpgPlayer, int skillLevel);
}