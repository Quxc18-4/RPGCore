package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

public interface IMelody {
    /**
     * Kích hoạt Giai điệu và bung hiệu ứng.
     *
     * @param player Người thi triển (Bard)
     * @param rpgPlayer Dữ liệu RPG của Bard
     * @param targetLocation Vị trí tâm điểm để thi triển (Truyền player.getLocation() nếu là Hào quang)
     */
    void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation);
}